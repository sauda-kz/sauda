package com.sauda.dto.lotmatch;

import com.sauda.domain.enums.LotMatchStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLotMatchStatusRequest(
        @NotNull LotMatchStatus status, String distributorComment) {}
