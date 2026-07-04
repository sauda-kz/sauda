package com.sauda.service;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.LotStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.dto.lotmatch.CreateLotMatchRequest;
import com.sauda.dto.lotmatch.DistributorLotMatchCardResponse;
import com.sauda.dto.lotmatch.LotMatchResponse;
import com.sauda.dto.lotmatch.SendLotToDistributorRequest;
import com.sauda.dto.lotmatch.UpdateLotMatchStatusRequest;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.LotMatchMapper;
import com.sauda.service.notification.event.LotSentToDistributorEvent;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class LotMatchService {

    private static final Set<LotMatchStatus> DISTRIBUTOR_ALLOWED_STATUSES =
            EnumSet.of(
                    LotMatchStatus.interested,
                    LotMatchStatus.dismissed,
                    LotMatchStatus.needs_review,
                    LotMatchStatus.not_matched,
                    LotMatchStatus.mismatch_reported);

    private static final Set<LotMatchStatus> SEND_ALLOWED_STATUSES =
            EnumSet.of(LotMatchStatus.matched, LotMatchStatus.needs_review);

    private final LotMatchRepository lotMatchRepository;
    private final LotService lotService;
    private final OfferRepository offerRepository;
    private final OrganizationRepository organizationRepository;
    private final LotMatchMapper lotMatchMapper;
    private final LotMatchCalculator lotMatchCalculator;
    private final TenantAccessService tenantAccessService;
    private final ApplicationEventPublisher eventPublisher;

    public LotMatchService(
            LotMatchRepository lotMatchRepository,
            LotService lotService,
            OfferRepository offerRepository,
            OrganizationRepository organizationRepository,
            LotMatchMapper lotMatchMapper,
            LotMatchCalculator lotMatchCalculator,
            TenantAccessService tenantAccessService,
            ApplicationEventPublisher eventPublisher) {
        this.lotMatchRepository = lotMatchRepository;
        this.lotService = lotService;
        this.offerRepository = offerRepository;
        this.organizationRepository = organizationRepository;
        this.lotMatchMapper = lotMatchMapper;
        this.lotMatchCalculator = lotMatchCalculator;
        this.tenantAccessService = tenantAccessService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates an internal draft match ({@code suggested}) without notifying the distributor. Use
     * {@link #sendToDistributor(UUID, SendLotToDistributorRequest)} to make a match visible.
     */
    @Transactional
    public LotMatchResponse createMatch(CreateLotMatchRequest request) {
        Lot lot = lotService.findLotOrThrow(request.lotId());
        Offer offer =
                offerRepository
                        .findWithDistributorById(request.offerId())
                        .orElseThrow(
                                () ->
                                        new SaudaNotFoundException(
                                                "Offer not found: " + request.offerId()));

        if (lotMatchRepository.existsByLotIdAndOfferId(lot.getId(), offer.getId())) {
            throw new SaudaException("Match already exists for this lot and offer");
        }

        LotMatch match = new LotMatch();
        match.setLot(lot);
        match.setOffer(offer);
        match.setDistributor(offer.getDistributor());
        match.setMatchStatus(
                request.status() != null ? request.status() : LotMatchStatus.suggested);
        match.setMatchReason(request.matchReason());
        match.setConfidenceScore(request.confidenceScore());
        if (request.matchedRequirements() != null) {
            match.setMatchedRequirements(request.matchedRequirements());
        }
        if (request.missingRequirements() != null) {
            match.setMissingRequirements(request.missingRequirements());
        }
        if (request.riskFlags() != null) {
            match.setRiskFlags(request.riskFlags());
        }
        match.setNeedsManualReview(
                request.needsManualReview() != null ? request.needsManualReview() : true);
        match.setAdminComment(request.adminComment() != null ? request.adminComment() : "");

        boolean derivedRequiresReview = lotMatchCalculator.applyDerivedFields(match, lot, offer);
        match.setNeedsManualReview(match.isNeedsManualReview() || derivedRequiresReview);
        return lotMatchMapper.toResponse(lotMatchRepository.save(match));
    }

    /**
     * Sends a lot to the distributor: creates or reactivates a match, sets {@code matched} (or
     * {@code needs_review}), records {@code sent_to_distributor_at}, and triggers notifications.
     */
    @Transactional
    public LotMatchResponse sendToDistributor(UUID lotId, SendLotToDistributorRequest request) {
        assertPlatformAccess();
        Lot lot = lotService.findLotOrThrow(lotId);
        assertLotSendable(lot);

        Offer offer =
                offerRepository
                        .findWithDistributorById(request.offerId())
                        .orElseThrow(
                                () ->
                                        new SaudaNotFoundException(
                                                "Offer not found: " + request.offerId()));

        LotMatchStatus targetStatus = resolveSendStatus(request.status());
        LotMatch match =
                lotMatchRepository
                        .findByLotIdAndOfferId(lotId, request.offerId())
                        .orElseGet(LotMatch::new);

        if (match.getId() == null) {
            match.setLot(lot);
            match.setOffer(offer);
            match.setDistributor(offer.getDistributor());
        }

        applySendRequestFields(match, request, targetStatus);
        boolean derivedRequiresReview = lotMatchCalculator.applyDerivedFields(match, lot, offer);
        match.setNeedsManualReview(match.isNeedsManualReview() || derivedRequiresReview);
        match.setSentToDistributorAt(Instant.now());

        LotMatch saved = lotMatchRepository.save(match);
        eventPublisher.publishEvent(new LotSentToDistributorEvent(saved.getId()));

        log.info(
                "Lot sent to distributor: lotId={}, offerId={}, distributorId={}, matchId={}",
                lotId,
                request.offerId(),
                offer.getDistributor().getId(),
                saved.getId());

        return lotMatchMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<LotMatchResponse> listByLot(UUID lotId, Pageable pageable) {
        lotService.findLotOrThrow(lotId);
        return lotMatchRepository.findByLotId(lotId, pageable).map(lotMatchMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LotMatchResponse getMatch(UUID matchId) {
        return lotMatchMapper.toResponse(findMatchOrThrow(matchId));
    }

    @Transactional(readOnly = true)
    public Page<DistributorLotMatchCardResponse> listForDistributor(
            UUID distributorId, LotMatchStatus status, boolean includeUnsent, Pageable pageable) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        if (includeUnsent) {
            assertPlatformAccess();
        }

        Page<LotMatch> page =
                lotMatchRepository.findForDistributor(
                        resolvedDistributorId,
                        !includeUnsent,
                        status != null ? status.name() : null,
                        pageable);
        return page.map(lotMatchMapper::toDistributorCard);
    }

    @Transactional(readOnly = true)
    public DistributorLotMatchCardResponse getForDistributor(UUID distributorId, UUID matchId) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        LotMatch match = findMatchForDistributorOrThrow(matchId, resolvedDistributorId);
        assertVisibleToDistributor(match);
        return lotMatchMapper.toDistributorCard(match);
    }

    @Transactional
    public DistributorLotMatchCardResponse updateStatusForDistributor(
            UUID distributorId, UUID matchId, UpdateLotMatchStatusRequest request) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        if (!DISTRIBUTOR_ALLOWED_STATUSES.contains(request.status())) {
            throw new SaudaException("Status not allowed for distributor: " + request.status());
        }

        LotMatch match = findMatchForDistributorOrThrow(matchId, resolvedDistributorId);
        assertVisibleToDistributor(match);
        match.setMatchStatus(request.status());
        if (request.distributorComment() != null) {
            match.setDistributorComment(request.distributorComment());
        }
        return lotMatchMapper.toDistributorCard(lotMatchRepository.save(match));
    }

    private static void applySendRequestFields(
            LotMatch match, SendLotToDistributorRequest request, LotMatchStatus targetStatus) {
        match.setMatchStatus(targetStatus);
        if (request.matchReason() != null) {
            match.setMatchReason(request.matchReason());
        } else if (match.getMatchReason() == null) {
            match.setMatchReason("");
        }
        if (request.riskFlags() != null) {
            match.setRiskFlags(request.riskFlags());
        }
        if (request.adminComment() != null) {
            match.setAdminComment(request.adminComment());
        } else if (match.getAdminComment() == null) {
            match.setAdminComment("");
        }
    }

    private static LotMatchStatus resolveSendStatus(LotMatchStatus requestedStatus) {
        if (requestedStatus == null) {
            return LotMatchStatus.matched;
        }
        if (!SEND_ALLOWED_STATUSES.contains(requestedStatus)) {
            throw new SaudaException("Status not allowed for send: " + requestedStatus);
        }
        return requestedStatus;
    }

    private static void assertLotSendable(Lot lot) {
        if (lot.getStatus() == LotStatus.archived || lot.getStatus() == LotStatus.cancelled) {
            throw new SaudaException("Cannot send archived or cancelled lot");
        }
    }

    private static void assertPlatformAccess() {
        if (SecurityUtils.requirePrincipal().organizationType() != OrganizationType.platform) {
            throw new SaudaForbiddenException("Operation is available for platform users only");
        }
    }

    private static void assertVisibleToDistributor(LotMatch match) {
        if (match.getSentToDistributorAt() == null
                && SecurityUtils.requirePrincipal().organizationType()
                        != OrganizationType.platform) {
            throw new SaudaNotFoundException("Lot match not found: " + match.getId());
        }
    }

    private void assertDistributorOrg(UUID distributorId) {
        if (!organizationRepository.existsByIdAndType(
                distributorId, OrganizationType.distributor)) {
            throw new SaudaNotFoundException("Distributor not found: " + distributorId);
        }
    }

    private LotMatch findMatchOrThrow(UUID matchId) {
        return lotMatchRepository
                .findById(matchId)
                .orElseThrow(() -> new SaudaNotFoundException("Lot match not found: " + matchId));
    }

    private LotMatch findMatchForDistributorOrThrow(UUID matchId, UUID distributorId) {
        return lotMatchRepository
                .findByIdAndDistributorId(matchId, distributorId)
                .orElseThrow(() -> new SaudaNotFoundException("Lot match not found: " + matchId));
    }
}
