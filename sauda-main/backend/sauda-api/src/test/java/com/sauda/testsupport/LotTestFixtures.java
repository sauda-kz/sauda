package com.sauda.testsupport;

import com.sauda.domain.enums.LotStatus;
import com.sauda.dto.lot.CreateLotRequest;
import com.sauda.dto.lot.UpdateLotRequest;
import java.math.BigDecimal;
import java.time.Instant;

public final class LotTestFixtures {

    private LotTestFixtures() {}

    public static CreateLotRequest sampleCreateLotRequest() {
        return new CreateLotRequest(
                "manual",
                "PUR-001",
                "LOT-001",
                "SSD 1TB",
                "Description",
                "АО Заказчик",
                "SSD",
                "запрос ценовых предложений",
                "товар",
                10,
                "шт",
                new BigDecimal("500000"),
                "KZT",
                "Алматы",
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                "24 месяца",
                "NVMe, 1TB",
                "Сертификат соответствия",
                "Опыт от 3 лет",
                "Оплата по факту",
                Instant.parse("2026-06-01T00:00:00Z"),
                "https://goszakup.kz/lot/1",
                "raw text",
                LotStatus.active);
    }

    public static UpdateLotRequest sampleUpdateLotRequest() {
        CreateLotRequest create = sampleCreateLotRequest();
        return new UpdateLotRequest(
                create.source(),
                create.externalPurchaseId(),
                create.externalLotId(),
                "SSD 1TB updated",
                create.description(),
                create.customerName(),
                create.category(),
                create.procurementMethod(),
                create.lotType(),
                create.quantity(),
                create.unit(),
                create.budgetAmount(),
                create.currency(),
                create.deliveryLocation(),
                create.deliveryDeadline(),
                create.submissionDeadline(),
                create.warrantyRequirements(),
                create.technicalRequirements(),
                create.requiredDocuments(),
                create.qualificationRequirements(),
                create.contractTermsSummary(),
                create.publishedAt(),
                create.sourceUrl(),
                create.rawText(),
                LotStatus.archived);
    }
}
