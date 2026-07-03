package com.sauda.dto.lotmatch;

import com.sauda.domain.enums.LotMatchStatus;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SendLotToDistributorRequest(
        @NotNull UUID offerId,
        String matchReason,
        List<String> riskFlags,
        String adminComment,
        LotMatchStatus status) {}
