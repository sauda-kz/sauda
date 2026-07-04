package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.domain.enums.StockStatus;
import com.sauda.dto.offer.OfferResponse;
import com.sauda.dto.offer.OfferSearchCriteria;
import com.sauda.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Offers", description = "Read-only offer catalog for admin review")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(ApiConstants.API_V1 + "/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @Operation(summary = "Search offers")
    @GetMapping
    @PreAuthorize("hasAuthority('offer:read')")
    public Page<OfferResponse> searchOffers(
            @RequestParam(required = false) UUID distributorId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return offerService.search(
                new OfferSearchCriteria(distributorId, category, brand, q, stockStatus, activeOnly),
                pageable);
    }

    @Operation(summary = "Get offer by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('offer:read')")
    public OfferResponse getOffer(@PathVariable UUID id) {
        return offerService.getById(id);
    }
}
