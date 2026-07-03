package com.sauda.service;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.enums.CheckResult;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.StockStatus;
import com.sauda.dto.lotmatch.PotentialMatchResponse;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.OfferSpecifications;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.matching.MatchSignal;
import com.sauda.service.matching.MatchSignalEvaluator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class LotMatchSuggestionService {

    private static final int MAX_SUGGESTIONS = 50;
    private static final BigDecimal MIN_CONFIDENCE_SCORE = new BigDecimal("0.20");

    private final LotService lotService;
    private final OfferRepository offerRepository;
    private final LotMatchRepository lotMatchRepository;
    private final LotMatchCalculator lotMatchCalculator;
    private final List<MatchSignalEvaluator> matchSignalEvaluators;

    public LotMatchSuggestionService(
            LotService lotService,
            OfferRepository offerRepository,
            LotMatchRepository lotMatchRepository,
            LotMatchCalculator lotMatchCalculator,
            List<MatchSignalEvaluator> matchSignalEvaluators) {
        this.lotService = lotService;
        this.offerRepository = offerRepository;
        this.lotMatchRepository = lotMatchRepository;
        this.lotMatchCalculator = lotMatchCalculator;
        this.matchSignalEvaluators = matchSignalEvaluators;
    }

    @Transactional(readOnly = true)
    public List<PotentialMatchResponse> suggestForLot(UUID lotId) {
        assertPlatformAccess();
        Lot lot = lotService.findLotOrThrow(lotId);
        List<UUID> excludedOfferIds = lotMatchRepository.findMatchedOfferIdsByLotId(lotId);

        List<Offer> candidates =
                offerRepository.findAll(
                        OfferSpecifications.suggestionCandidates(
                                lot.getCategory(), excludedOfferIds));

        List<PotentialMatchResponse> suggestions =
                candidates.stream()
                        .map(offer -> toPotentialMatch(lot, offer))
                        .filter(
                                suggestion ->
                                        suggestion.confidenceScore()
                                                        .compareTo(MIN_CONFIDENCE_SCORE)
                                                >= 0)
                        .sorted(
                                Comparator.comparing(PotentialMatchResponse::confidenceScore)
                                        .reversed())
                        .limit(MAX_SUGGESTIONS)
                        .toList();

        log.info("Potential matches suggested: lotId={}, count={}", lotId, suggestions.size());
        return suggestions;
    }

    private PotentialMatchResponse toPotentialMatch(Lot lot, Offer offer) {
        List<MatchSignal> signals =
                matchSignalEvaluators.stream()
                        .map(evaluator -> evaluator.evaluate(lot, offer))
                        .flatMap(Optional::stream)
                        .toList();

        BigDecimal confidenceScore =
                signals.stream()
                        .map(MatchSignal::weight)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .min(BigDecimal.ONE);
        String matchReason =
                signals.stream().map(MatchSignal::reason).reduce((a, b) -> a + "; " + b).orElse("");

        LotMatch derivedMatch = new LotMatch();
        boolean needsManualReview = lotMatchCalculator.applyDerivedFields(derivedMatch, lot, offer);
        List<String> missingData = buildMissingData(lot, offer);
        LotMatchStatus recommendedStatus =
                resolveRecommendedStatus(derivedMatch, missingData, needsManualReview);

        return new PotentialMatchResponse(
                offer.getId(),
                offer.getDistributor().getId(),
                offer.getDistributor().getName(),
                offer.getRawName(),
                offer.getPrice(),
                offer.getPriceIncludesVat(),
                offer.getStockQuantity(),
                offer.getStockStatus() != null ? offer.getStockStatus().name() : null,
                offer.getLeadTime(),
                matchReason,
                missingData,
                recommendedStatus,
                confidenceScore,
                derivedMatch.getQuantityCheck().name(),
                derivedMatch.getStockCheck().name(),
                derivedMatch.getPriceCheck().name());
    }

    private static List<String> buildMissingData(Lot lot, Offer offer) {
        List<String> missingData = new ArrayList<>();
        if (offer.getPrice() == null) {
            missingData.add("цена не указана");
        }
        if (offer.getStockQuantity() == null || offer.getStockQuantity() <= 0) {
            missingData.add("остаток = 0");
        }
        if (offer.getStockStatus() == StockStatus.out_of_stock
                || offer.getStockStatus() == StockStatus.unknown) {
            missingData.add("статус наличия требует проверки");
        }
        if (!StringUtils.hasText(offer.getLeadTime())) {
            missingData.add("срок поставки не указан");
        }
        if (StringUtils.hasText(lot.getWarrantyRequirements())) {
            missingData.add("гарантия в предложении не указана");
        }
        return List.copyOf(missingData);
    }

    private static LotMatchStatus resolveRecommendedStatus(
            LotMatch derivedMatch, List<String> missingData, boolean needsManualReview) {
        if (needsManualReview || !missingData.isEmpty()) {
            return LotMatchStatus.needs_review;
        }
        if (derivedMatch.getQuantityCheck() == CheckResult.fail
                || derivedMatch.getStockCheck() == CheckResult.fail
                || derivedMatch.getPriceCheck() == CheckResult.fail) {
            return LotMatchStatus.needs_review;
        }
        if (derivedMatch.getQuantityCheck() == CheckResult.unknown
                || derivedMatch.getStockCheck() == CheckResult.unknown
                || derivedMatch.getPriceCheck() == CheckResult.unknown) {
            return LotMatchStatus.needs_review;
        }
        return LotMatchStatus.matched;
    }

    private static void assertPlatformAccess() {
        if (SecurityUtils.requirePrincipal().organizationType() != OrganizationType.platform) {
            throw new SaudaForbiddenException(
                    "Potential matches are available for platform users only");
        }
    }
}
