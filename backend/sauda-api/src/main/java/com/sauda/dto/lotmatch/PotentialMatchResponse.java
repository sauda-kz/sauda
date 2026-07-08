package com.sauda.dto.lotmatch;

import com.sauda.domain.enums.LotMatchStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PotentialMatchResponse(
        UUID offerId,
        UUID distributorId,
        String distributorName,
        String offerName,
        BigDecimal price,
        Boolean priceIncludesVat,
        Integer stockQuantity,
        String stockStatus,
        String leadTime,
        String matchReason,
        List<String> missingData,
        LotMatchStatus recommendedStatus,
        BigDecimal confidenceScore,
        String quantityCheck,
        String stockCheck,
        String priceCheck) {}
