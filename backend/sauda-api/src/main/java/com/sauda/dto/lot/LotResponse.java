package com.sauda.dto.lot;

import com.sauda.domain.enums.LotStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LotResponse(
        UUID id,
        String source,
        String externalPurchaseId,
        String externalLotId,
        String title,
        String description,
        String customerName,
        String category,
        String procurementMethod,
        String lotType,
        Integer quantity,
        String unit,
        BigDecimal budgetAmount,
        String currency,
        String deliveryLocation,
        Instant deliveryDeadline,
        Instant submissionDeadline,
        String warrantyRequirements,
        String technicalRequirements,
        String requiredDocuments,
        String qualificationRequirements,
        String contractTermsSummary,
        Instant publishedAt,
        LotStatus status,
        String sourceUrl,
        String rawText,
        Instant createdAt,
        Instant updatedAt) {}
