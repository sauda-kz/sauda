package com.sauda.dto.lot;

import com.sauda.domain.enums.LotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record UpdateLotRequest(
        @NotBlank String source,
        String externalPurchaseId,
        @NotBlank String externalLotId,
        @NotBlank String title,
        String description,
        @NotBlank String customerName,
        @NotBlank String category,
        String procurementMethod,
        @NotBlank String lotType,
        @NotNull @Positive Integer quantity,
        @NotBlank String unit,
        @NotNull @Positive BigDecimal budgetAmount,
        @NotBlank String currency,
        @NotBlank String deliveryLocation,
        @NotNull Instant deliveryDeadline,
        Instant submissionDeadline,
        String warrantyRequirements,
        @NotBlank String technicalRequirements,
        @NotBlank String requiredDocuments,
        String qualificationRequirements,
        String contractTermsSummary,
        Instant publishedAt,
        @NotBlank String sourceUrl,
        String rawText,
        @NotNull LotStatus status) {}
