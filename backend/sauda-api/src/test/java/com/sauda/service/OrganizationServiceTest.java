package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.dto.organization.OrganizationResponse;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.OrganizationRepository;
import com.sauda.service.mapper.OrganizationMapper;
import com.sauda.testsupport.SecurityTestFixtures;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;

    private final OrganizationMapper organizationMapper =
            Mappers.getMapper(OrganizationMapper.class);

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void getCurrentOrganizationReturnsTenantProfile() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setType(OrganizationType.buyer);
        organization.setName("Buyer Shop A");
        organization.setBin("123456789012");
        organization.setVatPayer(true);

        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "buyer-a@shop.kz",
                organizationId,
                OrganizationType.buyer,
                Set.of("buyer"),
                "org:read");

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        OrganizationService organizationService =
                new OrganizationService(organizationRepository, organizationMapper);

        OrganizationResponse response = organizationService.getCurrentOrganization();

        assertThat(response.id()).isEqualTo(organizationId);
        assertThat(response.name()).isEqualTo("Buyer Shop A");
        assertThat(response.type()).isEqualTo(OrganizationType.buyer);
    }

    @Test
    void getCurrentOrganizationThrowsWhenMissing() {
        UUID organizationId = UUID.randomUUID();
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "buyer-a@shop.kz",
                organizationId,
                OrganizationType.buyer,
                Set.of("buyer"),
                "org:read");

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

        OrganizationService organizationService =
                new OrganizationService(organizationRepository, organizationMapper);

        assertThatThrownBy(organizationService::getCurrentOrganization)
                .isInstanceOf(SaudaNotFoundException.class);
    }
}
