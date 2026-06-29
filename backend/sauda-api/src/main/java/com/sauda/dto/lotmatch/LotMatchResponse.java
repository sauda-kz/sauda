package com.sauda.dto.lotmatch;

import com.sauda.domain.enums.CheckResult;
import com.sauda.domain.enums.LotMatchStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LotMatchResponse(
        UUID id,
        UUID lotId,
        UUID offerId,
        UUID distributorId,
        LotMatchStatus status,
        BigDecimal confidenceScore,
        String matchReason,
        List<String> matchedRequirements,
        List<String> missingRequirements,
        List<String> riskFlags,
        int requiredQuantity,
        int availableQuantity,
        CheckResult quantityCheck,
        CheckResult stockCheck,
        CheckResult priceCheck,
        BigDecimal estimatedUnitPrice,
        BigDecimal estimatedTotalPrice,
        BigDecimal budgetAmount,
        BigDecimal estimatedMargin,
        boolean needsManualReview,
        String adminComment,
        String distributorComment,
        Instant createdAt,
        Instant updatedAt) {}
