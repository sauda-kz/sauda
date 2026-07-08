package com.sauda.dto.imports;

import com.sauda.domain.enums.ImportStatus;
import java.time.Instant;
import java.util.UUID;

public record ImportRunResponse(
        UUID id,
        UUID rawUploadId,
        String originalFilename,
        UUID distributorId,
        String distributorName,
        String adapterKey,
        ImportStatus status,
        int totalRows,
        int parsedRowsCount,
        int errorRowsCount,
        Instant startedAt,
        Instant finishedAt,
        Instant approvedAt,
        Instant rejectedAt,
        Instant createdAt,
        Instant updatedAt) {}
