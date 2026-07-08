package com.sauda.service.imports.model;

import java.util.List;
import java.util.Map;

public record AdapterParsedRow(
        int sourceRowNumber,
        Map<String, Object> rawRowData,
        ImportRowFields fields,
        List<RowError> errors,
        List<RowError> warnings) {

    public AdapterParsedRow {
        rawRowData = rawRowData == null ? Map.of() : Map.copyOf(rawRowData);
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
