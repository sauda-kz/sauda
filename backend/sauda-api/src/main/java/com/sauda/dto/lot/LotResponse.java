package com.sauda.dto.lot;

import com.sauda.domain.enums.LotDataQualityStatus;
import com.sauda.domain.enums.LotStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
        UUID createdById,
        Instant createdAt,
        Instant updatedAt,
        LotDataQualityStatus dataQualityStatus,
        List<String> missingKeyFields,
        long matchCount) {

    public LotResponse withEnrichment(
            LotDataQualityStatus dataQualityStatus,
            List<String> missingKeyFields,
            long matchCount) {
        return new LotResponse(
                id,
                source,
                externalPurchaseId,
                externalLotId,
                title,
                description,
                customerName,
                category,
                procurementMethod,
                lotType,
                quantity,
                unit,
                budgetAmount,
                currency,
                deliveryLocation,
                deliveryDeadline,
                submissionDeadline,
                warrantyRequirements,
                technicalRequirements,
                requiredDocuments,
                qualificationRequirements,
                contractTermsSummary,
                publishedAt,
                status,
                sourceUrl,
                rawText,
                createdById,
                createdAt,
                updatedAt,
                dataQualityStatus,
                missingKeyFields,
                matchCount);
    }
}
