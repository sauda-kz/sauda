package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sauda.domain.enums.OrganizationType;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.testsupport.SecurityTestFixtures;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantAccessServiceTest {

    private final TenantAccessService tenantAccessService = new TenantAccessService();

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void assertCurrentOrganizationPassesForMatchingTenant() {
        UUID organizationId = UUID.randomUUID();
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "buyer-a@shop.kz",
                organizationId,
                OrganizationType.buyer,
                Set.of("buyer"),
                "org:read");

        tenantAccessService.assertCurrentOrganization(organizationId);

        assertThat(tenantAccessService.currentOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void assertCurrentOrganizationFailsForDifferentTenant() {
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "buyer-a@shop.kz",
                UUID.randomUUID(),
                OrganizationType.buyer,
                Set.of("buyer"),
                "org:read");

        assertThatThrownBy(() -> tenantAccessService.assertCurrentOrganization(UUID.randomUUID()))
                .isInstanceOf(SaudaForbiddenException.class);
    }

    @Test
    void resolveDistributorIdOverridesPathForDistributorUser() {
        UUID distributorId = UUID.randomUUID();
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "dist@supplier.kz",
                distributorId,
                OrganizationType.distributor,
                Set.of("distributor_manager"),
                "lot_match:read");

        UUID resolved = tenantAccessService.resolveDistributorId(UUID.randomUUID());

        assertThat(resolved).isEqualTo(distributorId);
    }

    @Test
    void resolveDistributorIdKeepsRequestedIdForPlatformAdmin() {
        UUID requestedId = UUID.randomUUID();
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "admin@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.platform,
                Set.of("platform_admin"),
                "lot_match:read");

        UUID resolved = tenantAccessService.resolveDistributorId(requestedId);

        assertThat(resolved).isEqualTo(requestedId);
    }
}
