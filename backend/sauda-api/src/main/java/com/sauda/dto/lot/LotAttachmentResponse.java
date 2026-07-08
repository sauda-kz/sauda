package com.sauda.dto.lot;

import java.time.Instant;
import java.util.UUID;

public record LotAttachmentResponse(
        UUID id,
        UUID lotId,
        String originalFilename,
        long fileSize,
        String mimeType,
        UUID uploadedById,
        Instant createdAt) {}
