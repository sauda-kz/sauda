package com.sauda.dto.auth;

import com.sauda.domain.enums.OrganizationType;
import java.util.Set;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        String email,
        UUID organizationId,
        OrganizationType organizationType,
        Set<String> roles) {}
