package com.sauda.dto.lot;

import com.sauda.domain.enums.LotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record UpdateLotRequest(
        String source,
        String externalPurchaseId,
        String externalLotId,
        @NotBlank String title,
        String description,
        @NotBlank String customerName,
        @NotBlank String category,
        String procurementMethod,
        String lotType,
        @Positive Integer quantity,
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
        Boolean confirmIncomplete) {}
