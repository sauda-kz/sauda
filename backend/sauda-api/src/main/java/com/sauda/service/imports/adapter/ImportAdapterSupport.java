package com.sauda.service.imports.adapter;

import com.sauda.service.imports.model.AdapterParseResult;
import com.sauda.service.imports.model.AdapterParsedRow;
import com.sauda.service.imports.model.RowError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ImportAdapterSupport {

    static final String MISSING_HEADER = "MISSING_HEADER";
    static final String ROW_LIMIT_EXCEEDED = "ROW_LIMIT_EXCEEDED";

    private ImportAdapterSupport() {}

    static AdapterParseResult missingHeaderResult() {
        return fileLevelError(MISSING_HEADER, "Header row is required");
    }

    static AdapterParseResult rowLimitExceededResult(int maxRows) {
        return fileLevelError(ROW_LIMIT_EXCEEDED, "Row limit exceeded: " + maxRows);
    }

    static AdapterParseResult fileLevelError(String code, String message) {
        return new AdapterParseResult(
                List.of(
                        new AdapterParsedRow(
                                0,
                                Map.of(),
                                null,
                                List.of(new RowError(null, code, message)),
                                List.of())));
    }

    static boolean isBlankRow(Map<String, String> cells) {
        return cells.values().stream().allMatch(value -> ImportRowMapper.trimToNull(value) == null);
    }

    static Map<String, String> normalizeCells(Map<String, String> rawCells) {
        Map<String, String> normalized = new LinkedHashMap<>();
        rawCells.forEach(
                (key, value) -> {
                    String normalizedKey = ImportRowMapper.normalizeColumnName(key);
                    if (!normalizedKey.isEmpty()) {
                        normalized.put(normalizedKey, value);
                    }
                });
        return normalized;
    }

    static Map<String, Object> toRawRowData(Map<String, String> rawCells) {
        return Map.copyOf(rawCells);
    }

    static AdapterParsedRow toParsedRow(
            int sourceRowNumber, Map<String, String> rawCells, ImportRowMapper rowMapper) {
        Map<String, String> normalizedCells = normalizeCells(rawCells);
        ImportRowMapper.ImportRowMappingResult mapping = rowMapper.map(normalizedCells);
        return new AdapterParsedRow(
                sourceRowNumber,
                toRawRowData(rawCells),
                mapping.fields(),
                mapping.errors(),
                mapping.warnings());
    }
}
