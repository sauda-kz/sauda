package com.sauda.service;

import com.sauda.domain.enums.OrganizationType;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.security.principal.SaudaPrincipal;
import com.sauda.security.principal.SecurityUtils;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessService {

    public UUID currentOrganizationId() {
        return SecurityUtils.requirePrincipal().organizationId();
    }

    public void assertCurrentOrganization(UUID organizationId) {
        if (!currentOrganizationId().equals(organizationId)) {
            throw new SaudaForbiddenException("Access denied to organization data");
        }
    }

    public UUID resolveDistributorId(UUID requestedDistributorId) {
        SaudaPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.organizationType() == OrganizationType.distributor) {
            return principal.organizationId();
        }
        return requestedDistributorId;
    }
}
