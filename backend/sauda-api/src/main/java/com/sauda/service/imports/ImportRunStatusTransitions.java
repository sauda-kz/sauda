package com.sauda.service.imports;

import com.sauda.domain.enums.ImportStatus;
import com.sauda.exception.SaudaException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ImportRunStatusTransitions {

    private static final Map<ImportStatus, Set<ImportStatus>> ALLOWED =
            Map.of(
                    ImportStatus.pending,
                    EnumSet.of(ImportStatus.processing),
                    ImportStatus.processing,
                    EnumSet.of(
                            ImportStatus.parsed,
                            ImportStatus.parsed_with_errors,
                            ImportStatus.failed),
                    ImportStatus.parsed,
                    EnumSet.of(
                            ImportStatus.awaiting_approval,
                            ImportStatus.approved,
                            ImportStatus.rejected),
                    ImportStatus.parsed_with_errors,
                    EnumSet.of(
                            ImportStatus.awaiting_approval,
                            ImportStatus.approved,
                            ImportStatus.rejected),
                    ImportStatus.awaiting_approval,
                    EnumSet.of(ImportStatus.approved, ImportStatus.rejected),
                    ImportStatus.approved,
                    EnumSet.of(ImportStatus.applied, ImportStatus.failed));

    private ImportRunStatusTransitions() {}

    public static void assertTransition(ImportStatus from, ImportStatus to) {
        if (!isAllowed(from, to)) {
            throw new SaudaException("Invalid import run status transition: " + from + " -> " + to);
        }
    }

    public static boolean isAllowed(ImportStatus from, ImportStatus to) {
        Set<ImportStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }
}
