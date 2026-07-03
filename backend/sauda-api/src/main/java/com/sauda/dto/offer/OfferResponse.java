package com.sauda.dto.offer;

import com.sauda.domain.enums.StockStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID distributorId,
        String distributorName,
        String rawName,
        String brand,
        String modelMpn,
        String category,
        BigDecimal price,
        String currency,
        Boolean priceIncludesVat,
        Integer stockQuantity,
        StockStatus stockStatus,
        String leadTime,
        Instant lastUpdatedAt,
        Instant createdAt) {}
