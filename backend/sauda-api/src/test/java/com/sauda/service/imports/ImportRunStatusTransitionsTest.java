package com.sauda.service.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sauda.domain.enums.ImportStatus;
import com.sauda.exception.SaudaException;
import org.junit.jupiter.api.Test;

class ImportRunStatusTransitionsTest {

    @Test
    void allowsExpectedProcessingFlow() {
        assertThat(ImportRunStatusTransitions.isAllowed(ImportStatus.pending, ImportStatus.processing))
                .isTrue();
        assertThat(ImportRunStatusTransitions.isAllowed(ImportStatus.processing, ImportStatus.parsed))
                .isTrue();
        assertThat(
                        ImportRunStatusTransitions.isAllowed(
                                ImportStatus.parsed, ImportStatus.awaiting_approval))
                .isTrue();
    }

    @Test
    void rejectsInvalidTransition() {
        assertThatThrownBy(
                        () ->
                                ImportRunStatusTransitions.assertTransition(
                                        ImportStatus.pending, ImportStatus.approved))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("Invalid import run status transition");
    }

    @Test
    void allowsApprovedToAppliedOrFailed() {
        assertThat(ImportRunStatusTransitions.isAllowed(ImportStatus.approved, ImportStatus.applied))
                .isTrue();
        assertThat(ImportRunStatusTransitions.isAllowed(ImportStatus.approved, ImportStatus.failed))
                .isTrue();
    }
}
