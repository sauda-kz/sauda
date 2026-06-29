package com.sauda.service;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.enums.LotStatus;
import com.sauda.dto.lot.CreateLotRequest;
import com.sauda.dto.lot.LotResponse;
import com.sauda.dto.lot.UpdateLotRequest;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.LotRepository;
import com.sauda.service.mapper.LotMapper;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotService {

    private final LotRepository lotRepository;
    private final LotMapper lotMapper;

    public LotService(LotRepository lotRepository, LotMapper lotMapper) {
        this.lotRepository = lotRepository;
        this.lotMapper = lotMapper;
    }

    @Transactional(readOnly = true)
    public Page<LotResponse> listLots(LotStatus status, Pageable pageable) {
        Page<Lot> page =
                status != null
                        ? lotRepository.findByStatus(status, pageable)
                        : lotRepository.findAll(pageable);
        return page.map(lotMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LotResponse getLot(UUID id) {
        return lotMapper.toResponse(findLotOrThrow(id));
    }

    @Transactional
    public LotResponse createLot(CreateLotRequest request) {
        Lot lot = lotMapper.toEntity(request);
        return lotMapper.toResponse(lotRepository.save(lot));
    }

    @Transactional
    public LotResponse updateLot(UUID id, UpdateLotRequest request) {
        Lot lot = findLotOrThrow(id);
        lotMapper.updateEntity(lot, request);
        return lotMapper.toResponse(lotRepository.save(lot));
    }

    @Transactional
    public LotResponse archiveLot(UUID id) {
        Lot lot = findLotOrThrow(id);
        lot.setStatus(LotStatus.archived);
        return lotMapper.toResponse(lotRepository.save(lot));
    }

    Lot findLotOrThrow(UUID id) {
        return lotRepository
                .findById(id)
                .orElseThrow(() -> new SaudaNotFoundException("Lot not found: " + id));
    }
}
