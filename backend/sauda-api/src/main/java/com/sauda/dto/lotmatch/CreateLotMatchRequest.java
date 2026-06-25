package com.sauda.dto.lotmatch;

import com.sauda.domain.enums.LotMatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateLotMatchRequest(
        @NotNull UUID lotId,
        @NotNull UUID offerId,
        @NotBlank String matchReason,
        LotMatchStatus status,
        BigDecimal confidenceScore,
        List<String> matchedRequirements,
        List<String> missingRequirements,
        List<String> riskFlags,
        Boolean needsManualReview,
        String adminComment) {}
