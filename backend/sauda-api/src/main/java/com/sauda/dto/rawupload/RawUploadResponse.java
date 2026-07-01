package com.sauda.dto.rawupload;

import com.sauda.domain.enums.RawUploadStatus;
import java.time.Instant;
import java.util.UUID;

public record RawUploadResponse(
        UUID id,
        UUID distributorId,
        UUID uploadedByUserId,
        String uploadedByRole,
        String originalFilename,
        String storagePath,
        long fileSize,
        String mimeType,
        String checksum,
        RawUploadStatus status,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {}
