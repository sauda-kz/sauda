package com.sauda.dto.organization;

import com.sauda.domain.enums.OrganizationType;
import java.util.UUID;

public record OrganizationResponse(
        UUID id, OrganizationType type, String name, String bin, boolean vatPayer) {}
