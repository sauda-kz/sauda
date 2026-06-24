package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.dto.lotmatch.CreateLotMatchRequest;
import com.sauda.dto.lotmatch.DistributorLotMatchCardResponse;
import com.sauda.dto.lotmatch.LotMatchResponse;
import com.sauda.dto.lotmatch.UpdateLotMatchStatusRequest;
import com.sauda.service.LotMatchService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1)
public class LotMatchController {

    private final LotMatchService lotMatchService;

    public LotMatchController(LotMatchService lotMatchService) {
        this.lotMatchService = lotMatchService;
    }

    @PostMapping("/lot-matches")
    public ResponseEntity<LotMatchResponse> createMatch(
            @Valid @RequestBody CreateLotMatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lotMatchService.createMatch(request));
    }

    @GetMapping("/lot-matches")
    public Page<LotMatchResponse> listMatches(
            @RequestParam UUID lotId, @PageableDefault(size = 20) Pageable pageable) {
        return lotMatchService.listByLot(lotId, pageable);
    }

    @GetMapping("/lot-matches/{matchId}")
    public LotMatchResponse getMatch(@PathVariable UUID matchId) {
        return lotMatchService.getMatch(matchId);
    }

    @GetMapping("/distributors/{distributorId}/lot-matches")
    public Page<DistributorLotMatchCardResponse> listForDistributor(
            @PathVariable UUID distributorId,
            @RequestParam(required = false) LotMatchStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return lotMatchService.listForDistributor(distributorId, status, pageable);
    }

    @GetMapping("/distributors/{distributorId}/lot-matches/{matchId}")
    public DistributorLotMatchCardResponse getForDistributor(
            @PathVariable UUID distributorId, @PathVariable UUID matchId) {
        return lotMatchService.getForDistributor(distributorId, matchId);
    }

    @PatchMapping("/distributors/{distributorId}/lot-matches/{matchId}/status")
    public DistributorLotMatchCardResponse updateStatusForDistributor(
            @PathVariable UUID distributorId,
            @PathVariable UUID matchId,
            @Valid @RequestBody UpdateLotMatchStatusRequest request) {
        return lotMatchService.updateStatusForDistributor(distributorId, matchId, request);
    }
}
