package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.testsupport.LotTestFixtures;
import org.junit.jupiter.api.Test;

class LotValidationServiceTest {

    private final LotValidationService lotValidationService = new LotValidationService();

    @Test
    void completeLotHasNoMissingKeyFields() {
        assertThat(
                        lotValidationService.findMissingKeyFields(
                                LotTestFixtures.sampleCreateLotRequest()))
                .isEmpty();
    }

    @Test
    void incompleteLotReportsMissingKeyFields() {
        assertThat(
                        lotValidationService.findMissingKeyFields(
                                LotTestFixtures.incompleteCreateLotRequest()))
                .containsExactlyInAnyOrder(
                        "budgetAmount",
                        "deliveryDeadline",
                        "technicalRequirements",
                        "submissionDeadline",
                        "quantity");
    }

    @Test
    void buildWarningUsesRussianFieldLabels() {
        var warning =
                lotValidationService.buildWarning(
                        java.util.List.of("budgetAmount", "technicalRequirements"));

        assertThat(warning.message()).contains("сумма лота");
        assertThat(warning.message()).contains("технические требования");
        assertThat(warning.message()).contains("Требует проверки");
    }
}
