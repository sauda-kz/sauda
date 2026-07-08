package com.sauda.service.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sauda.domain.enums.ImportStatus;
import com.sauda.exception.SaudaException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ImportRunStatusTransitionsTest {

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void allowsTransition(ImportStatus from, ImportStatus to) {
        ImportRunStatusTransitions.assertTransition(from, to);
        assertThat(ImportRunStatusTransitions.isAllowed(from, to)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("disallowedTransitions")
    void rejectsTransition(ImportStatus from, ImportStatus to) {
        assertThat(ImportRunStatusTransitions.isAllowed(from, to)).isFalse();
        assertThatThrownBy(() -> ImportRunStatusTransitions.assertTransition(from, to))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("Invalid import run status transition");
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(ImportStatus.pending, ImportStatus.processing),
                Arguments.of(ImportStatus.processing, ImportStatus.parsed),
                Arguments.of(ImportStatus.processing, ImportStatus.parsed_with_errors),
                Arguments.of(ImportStatus.processing, ImportStatus.failed),
                Arguments.of(ImportStatus.parsed, ImportStatus.awaiting_approval),
                Arguments.of(ImportStatus.parsed, ImportStatus.approved),
                Arguments.of(ImportStatus.parsed, ImportStatus.rejected),
                Arguments.of(ImportStatus.parsed_with_errors, ImportStatus.awaiting_approval),
                Arguments.of(ImportStatus.parsed_with_errors, ImportStatus.approved),
                Arguments.of(ImportStatus.parsed_with_errors, ImportStatus.rejected),
                Arguments.of(ImportStatus.awaiting_approval, ImportStatus.approved),
                Arguments.of(ImportStatus.awaiting_approval, ImportStatus.rejected),
                Arguments.of(ImportStatus.approved, ImportStatus.applied),
                Arguments.of(ImportStatus.approved, ImportStatus.failed));
    }

    private static Stream<Arguments> disallowedTransitions() {
        return Stream.of(
                Arguments.of(ImportStatus.pending, ImportStatus.approved),
                Arguments.of(ImportStatus.pending, ImportStatus.applied),
                Arguments.of(ImportStatus.awaiting_approval, ImportStatus.applied),
                Arguments.of(ImportStatus.applied, ImportStatus.approved),
                Arguments.of(ImportStatus.rejected, ImportStatus.approved),
                Arguments.of(ImportStatus.failed, ImportStatus.processing));
    }
}
