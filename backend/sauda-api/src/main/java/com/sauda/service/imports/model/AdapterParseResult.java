package com.sauda.service.imports.model;

import java.util.List;

public record AdapterParseResult(List<AdapterParsedRow> rows) {

    public AdapterParseResult {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public int totalRows() {
        return rows.size();
    }

    public int errorRows() {
        return (int) rows.stream().filter(row -> !row.errors().isEmpty()).count();
    }
}
