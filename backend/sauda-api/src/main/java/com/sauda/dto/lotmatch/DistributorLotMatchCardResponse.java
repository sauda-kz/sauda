package com.sauda.dto.lotmatch;

import com.sauda.domain.enums.LotMatchStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DistributorLotMatchCardResponse(
        UUID matchId,
        LotMatchStatus status,
        String title,
        String customerName,
        BigDecimal budgetAmount,
        String currency,
        Instant deliveryDeadline,
        Instant submissionDeadline,
        String deliveryLocation,
        String category,
        Integer quantity,
        String unit,
        String requiredDocuments,
        String sourceUrl,
        String offerName,
        String brand,
        String modelMpn,
        int availableQuantity,
        BigDecimal estimatedUnitPrice,
        BigDecimal estimatedTotalPrice,
        BigDecimal estimatedMargin,
        BigDecimal confidenceScore,
        String matchReason,
        List<String> matchedRequirements,
        List<String> missingRequirements,
        List<String> riskFlags,
        boolean needsManualReview,
        String distributorComment) {}
