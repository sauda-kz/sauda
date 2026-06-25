package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.domain.enums.LotStatus;
import com.sauda.dto.lot.CreateLotRequest;
import com.sauda.dto.lot.LotResponse;
import com.sauda.dto.lot.UpdateLotRequest;
import com.sauda.service.LotService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/lots")
public class LotController {

    private final LotService lotService;

    public LotController(LotService lotService) {
        this.lotService = lotService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('lot:read')")
    public Page<LotResponse> listLots(
            @RequestParam(required = false) LotStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return lotService.listLots(status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('lot:read')")
    public LotResponse getLot(@PathVariable UUID id) {
        return lotService.getLot(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('lot:create')")
    public ResponseEntity<LotResponse> createLot(@Valid @RequestBody CreateLotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lotService.createLot(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('lot:manage')")
    public LotResponse updateLot(
            @PathVariable UUID id, @Valid @RequestBody UpdateLotRequest request) {
        return lotService.updateLot(id, request);
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('lot:manage')")
    public LotResponse archiveLot(@PathVariable UUID id) {
        return lotService.archiveLot(id);
    }
}
