package com.sauda.dto.imports;

import com.sauda.domain.enums.ParsedRowStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ParsedRowResponse(
        UUID id,
        UUID importRunId,
        Integer sourceRowNumber,
        Map<String, Object> rawRowData,
        Map<String, Object> parsedData,
        ParsedRowStatus status,
        List<ParsedRowErrorItem> errors,
        List<ParsedRowErrorItem> warnings,
        Instant editedAt) {}
