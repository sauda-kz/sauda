package com.sauda.service;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Lot;
import com.sauda.domain.enums.LotDataQualityStatus;
import com.sauda.domain.enums.LotStatus;
import com.sauda.dto.lot.CreateLotRequest;
import com.sauda.dto.lot.LotResponse;
import com.sauda.dto.lot.UpdateLotRequest;
import com.sauda.exception.SaudaIncompleteLotException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.LotMatchCountView;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.LotRepository;
import com.sauda.repository.LotSpecifications;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.LotMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class LotService {

    private final LotRepository lotRepository;
    private final LotMatchRepository lotMatchRepository;
    private final AppUserRepository appUserRepository;
    private final LotMapper lotMapper;
    private final LotValidationService lotValidationService;

    public LotService(
            LotRepository lotRepository,
            LotMatchRepository lotMatchRepository,
            AppUserRepository appUserRepository,
            LotMapper lotMapper,
            LotValidationService lotValidationService) {
        this.lotRepository = lotRepository;
        this.lotMatchRepository = lotMatchRepository;
        this.appUserRepository = appUserRepository;
        this.lotMapper = lotMapper;
        this.lotValidationService = lotValidationService;
    }

    @Transactional(readOnly = true)
    public Page<LotResponse> listLots(
            LotStatus status, String query, String category, String source, Pageable pageable) {
        Page<Lot> page =
                lotRepository.findAll(
                        LotSpecifications.withFilters(status, category, source, query), pageable);
        Map<UUID, Long> matchCounts = loadMatchCounts(page.getContent());
        return page.map(lot -> enrichResponse(lot, matchCounts.getOrDefault(lot.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public LotResponse getLot(UUID id) {
        Lot lot = findLotOrThrow(id);
        return enrichResponse(lot, lotMatchRepository.countByLotId(id));
    }

    @Transactional
    public LotResponse createLot(CreateLotRequest request) {
        List<String> missingKeyFields = lotValidationService.findMissingKeyFields(request);
        Lot lot = lotMapper.toEntity(request);
        applyStatusRules(lot, missingKeyFields, request.confirmIncomplete(), request.status());
        resolveCreatedBy(lot);
        Lot saved = lotRepository.save(lot);
        log.info(
                "Created lot: lotId={}, status={}, missingKeyFields={}",
                saved.getId(),
                saved.getStatus(),
                missingKeyFields.size());
        return enrichResponse(saved, 0L);
    }

    @Transactional
    public LotResponse updateLot(UUID id, UpdateLotRequest request) {
        Lot lot = findLotOrThrow(id);
        lotMapper.updateEntity(lot, request);
        List<String> missingKeyFields = lotValidationService.findMissingKeyFields(lot);
        applyStatusRules(lot, missingKeyFields, request.confirmIncomplete(), request.status());
        Lot saved = lotRepository.save(lot);
        log.info(
                "Updated lot: lotId={}, status={}, missingKeyFields={}",
                saved.getId(),
                saved.getStatus(),
                missingKeyFields.size());
        return enrichResponse(saved, lotMatchRepository.countByLotId(id));
    }

    @Transactional
    public LotResponse archiveLot(UUID id) {
        Lot lot = findLotOrThrow(id);
        lot.setStatus(LotStatus.archived);
        return enrichResponse(lotRepository.save(lot), lotMatchRepository.countByLotId(id));
    }

    Lot findLotOrThrow(UUID id) {
        return lotRepository
                .findById(id)
                .orElseThrow(() -> new SaudaNotFoundException("Lot not found: " + id));
    }

    private void applyStatusRules(
            Lot lot,
            List<String> missingKeyFields,
            Boolean confirmIncomplete,
            LotStatus requestedStatus) {
        if (!missingKeyFields.isEmpty()) {
            if (!Boolean.TRUE.equals(confirmIncomplete)) {
                throw new SaudaIncompleteLotException(
                        lotValidationService.buildWarning(missingKeyFields));
            }
            lot.setStatus(LotStatus.needs_review);
            return;
        }
        lot.setStatus(requestedStatus != null ? requestedStatus : LotStatus.draft);
    }

    private void resolveCreatedBy(Lot lot) {
        UUID userId = SecurityUtils.requirePrincipal().id();
        AppUser creator =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() -> new SaudaNotFoundException("User not found: " + userId));
        lot.setCreatedBy(creator);
    }

    private LotResponse enrichResponse(Lot lot, long matchCount) {
        List<String> missingKeyFields = lotValidationService.findMissingKeyFields(lot);
        LotDataQualityStatus dataQualityStatus =
                missingKeyFields.isEmpty()
                        ? LotDataQualityStatus.complete
                        : LotDataQualityStatus.needs_review;
        return lotMapper
                .toResponse(lot)
                .withEnrichment(dataQualityStatus, missingKeyFields, matchCount);
    }

    private Map<UUID, Long> loadMatchCounts(List<Lot> lots) {
        if (lots.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> lotIds = lots.stream().map(Lot::getId).toList();
        return lotMatchRepository.countMatchesByLotIds(lotIds).stream()
                .collect(
                        Collectors.toMap(
                                LotMatchCountView::getLotId, LotMatchCountView::getMatchCount));
    }
}
