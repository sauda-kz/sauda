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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class LotMatchSuggestionService {

    private static final int MAX_SUGGESTIONS = 50;
    private static final BigDecimal MIN_CONFIDENCE_SCORE = new BigDecimal("0.20");
    private static final BigDecimal CATEGORY_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal NAME_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal BRAND_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal MODEL_WEIGHT = new BigDecimal("0.15");
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final LotService lotService;
    private final OfferRepository offerRepository;
    private final LotMatchRepository lotMatchRepository;
    private final LotMatchCalculator lotMatchCalculator;

    public LotMatchSuggestionService(
            LotService lotService,
            OfferRepository offerRepository,
            LotMatchRepository lotMatchRepository,
            LotMatchCalculator lotMatchCalculator) {
        this.lotService = lotService;
        this.offerRepository = offerRepository;
        this.lotMatchRepository = lotMatchRepository;
        this.lotMatchCalculator = lotMatchCalculator;
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
        List<String> matchSignals = new ArrayList<>();
        BigDecimal confidenceScore = BigDecimal.ZERO;

        if (matchesCategory(lot, offer)) {
            matchSignals.add("Совпала категория: " + lot.getCategory());
            confidenceScore = confidenceScore.add(CATEGORY_WEIGHT);
        }

        double nameOverlap = nameTokenOverlapScore(lot, offer);
        if (nameOverlap > 0) {
            matchSignals.add("Совпадение по названию");
            confidenceScore =
                    confidenceScore.add(
                            NAME_WEIGHT.multiply(
                                    BigDecimal.valueOf(nameOverlap).setScale(4, RoundingMode.HALF_UP)));
        }

        if (matchesBrand(lot, offer)) {
            matchSignals.add("Совпал бренд: " + offer.getBrand());
            confidenceScore = confidenceScore.add(BRAND_WEIGHT);
        }

        if (matchesModel(lot, offer)) {
            matchSignals.add("Совпала модель: " + offer.getModelMpn());
            confidenceScore = confidenceScore.add(MODEL_WEIGHT);
        }

        confidenceScore = confidenceScore.min(BigDecimal.ONE);

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
                String.join("; ", matchSignals),
                missingData,
                recommendedStatus,
                confidenceScore,
                derivedMatch.getQuantityCheck().name(),
                derivedMatch.getStockCheck().name(),
                derivedMatch.getPriceCheck().name());
    }

    private static boolean matchesCategory(Lot lot, Offer offer) {
        if (!StringUtils.hasText(lot.getCategory())) {
            return false;
        }
        if (offer.getCanonicalProduct() == null
                || !StringUtils.hasText(offer.getCanonicalProduct().getCategory())) {
            return false;
        }
        return lot.getCategory().equalsIgnoreCase(offer.getCanonicalProduct().getCategory());
    }

    private static double nameTokenOverlapScore(Lot lot, Offer offer) {
        Set<String> lotTokens = tokenize(lot.getTitle(), lot.getTechnicalRequirements());
        Set<String> offerTokens = tokenize(offer.getRawName());
        if (lotTokens.isEmpty() || offerTokens.isEmpty()) {
            return 0;
        }
        long overlap =
                lotTokens.stream().filter(token -> offerTokens.contains(token)).count();
        return (double) overlap / Math.max(lotTokens.size(), offerTokens.size());
    }

    private static boolean matchesBrand(Lot lot, Offer offer) {
        if (!StringUtils.hasText(offer.getBrand())) {
            return false;
        }
        String haystack = combinedLotText(lot).toLowerCase(Locale.ROOT);
        return haystack.contains(offer.getBrand().toLowerCase(Locale.ROOT));
    }

    private static boolean matchesModel(Lot lot, Offer offer) {
        if (!StringUtils.hasText(offer.getModelMpn())) {
            return false;
        }
        String haystack = combinedLotText(lot).toLowerCase(Locale.ROOT);
        return haystack.contains(offer.getModelMpn().toLowerCase(Locale.ROOT));
    }

    private static Set<String> tokenize(String... parts) {
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            for (String token : TOKEN_SPLITTER.split(part.toLowerCase(Locale.ROOT))) {
                if (token.length() >= 2) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    private static String combinedLotText(Lot lot) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(lot.getTitle())) {
            builder.append(lot.getTitle()).append(' ');
        }
        if (StringUtils.hasText(lot.getTechnicalRequirements())) {
            builder.append(lot.getTechnicalRequirements());
        }
        return builder.toString();
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
            throw new SaudaForbiddenException("Potential matches are available for platform users only");
        }
    }
}
