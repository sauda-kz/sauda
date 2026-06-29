package com.sauda.service;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.dto.lotmatch.CreateLotMatchRequest;
import com.sauda.dto.lotmatch.DistributorLotMatchCardResponse;
import com.sauda.dto.lotmatch.LotMatchResponse;
import com.sauda.dto.lotmatch.UpdateLotMatchStatusRequest;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.service.mapper.LotMatchMapper;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotMatchService {

    private static final Set<LotMatchStatus> DISTRIBUTOR_ALLOWED_STATUSES =
            EnumSet.of(
                    LotMatchStatus.interested,
                    LotMatchStatus.dismissed,
                    LotMatchStatus.needs_review,
                    LotMatchStatus.not_matched,
                    LotMatchStatus.mismatch_reported);

    private final LotMatchRepository lotMatchRepository;
    private final LotService lotService;
    private final OfferRepository offerRepository;
    private final OrganizationRepository organizationRepository;
    private final LotMatchMapper lotMatchMapper;
    private final LotMatchCalculator lotMatchCalculator;
    private final TenantAccessService tenantAccessService;

    public LotMatchService(
            LotMatchRepository lotMatchRepository,
            LotService lotService,
            OfferRepository offerRepository,
            OrganizationRepository organizationRepository,
            LotMatchMapper lotMatchMapper,
            LotMatchCalculator lotMatchCalculator,
            TenantAccessService tenantAccessService) {
        this.lotMatchRepository = lotMatchRepository;
        this.lotService = lotService;
        this.offerRepository = offerRepository;
        this.organizationRepository = organizationRepository;
        this.lotMatchMapper = lotMatchMapper;
        this.lotMatchCalculator = lotMatchCalculator;
        this.tenantAccessService = tenantAccessService;
    }

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
            UUID distributorId, LotMatchStatus status, Pageable pageable) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        Page<LotMatch> page =
                status != null
                        ? lotMatchRepository.findByDistributorIdAndMatchStatusOrderByCreatedAtDesc(
                                resolvedDistributorId, status, pageable)
                        : lotMatchRepository.findByDistributorIdOrderByCreatedAtDesc(
                                resolvedDistributorId, pageable);
        return page.map(lotMatchMapper::toDistributorCard);
    }

    @Transactional(readOnly = true)
    public DistributorLotMatchCardResponse getForDistributor(UUID distributorId, UUID matchId) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        LotMatch match = findMatchForDistributorOrThrow(matchId, resolvedDistributorId);
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
        match.setMatchStatus(request.status());
        if (request.distributorComment() != null) {
            match.setDistributorComment(request.distributorComment());
        }
        return lotMatchMapper.toDistributorCard(lotMatchRepository.save(match));
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
