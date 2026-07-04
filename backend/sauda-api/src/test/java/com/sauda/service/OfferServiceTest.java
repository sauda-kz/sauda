package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.RoleCode;
import com.sauda.domain.enums.StockStatus;
import com.sauda.dto.offer.OfferSearchCriteria;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.OfferRepository;
import com.sauda.service.mapper.OfferMapper;
import com.sauda.testsupport.OfferTestFixtures;
import com.sauda.testsupport.SecurityTestFixtures;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock private OfferRepository offerRepository;

    @Spy private OfferMapper offerMapper = Mappers.getMapper(OfferMapper.class);

    @Mock private TenantAccessService tenantAccessService;

    @InjectMocks private OfferService offerService;

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void searchReturnsOffersForPlatformAdmin() {
        UUID distributorId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        Organization distributor = OfferTestFixtures.sampleDistributor(distributorId);
        Offer offer = OfferTestFixtures.sampleOffer(offerId, distributor);

        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "admin@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.platform,
                Set.of(RoleCode.platform_admin.name()),
                "offer:read");

        when(offerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(offer)));

        var page =
                offerService.search(
                        new OfferSearchCriteria(
                                distributorId, "SSD", "Samsung", "990", StockStatus.in_stock, true),
                        Pageable.ofSize(20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().id()).isEqualTo(offerId);
        assertThat(page.getContent().getFirst().distributorName()).isEqualTo("Tech Distributor");
        assertThat(page.getContent().getFirst().category()).isEqualTo("SSD");
    }

    @Test
    void searchScopesDistributorToOwnOrganization() {
        UUID distributorId = UUID.randomUUID();
        Organization distributor = OfferTestFixtures.sampleDistributor(distributorId);
        Offer offer = OfferTestFixtures.sampleOffer(UUID.randomUUID(), distributor);

        SecurityTestFixtures.setPrincipal(
                distributorId,
                "manager@dist.kz",
                distributorId,
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "offer:read");

        when(offerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(offer)));

        var page =
                offerService.search(
                        new OfferSearchCriteria(UUID.randomUUID(), null, null, null, null, true),
                        Pageable.ofSize(20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().distributorId()).isEqualTo(distributorId);
    }

    @Test
    void getByIdReturnsOfferForAdmin() {
        UUID offerId = UUID.randomUUID();
        UUID distributorId = UUID.randomUUID();
        Organization distributor = OfferTestFixtures.sampleDistributor(distributorId);
        Offer offer = OfferTestFixtures.sampleOffer(offerId, distributor);

        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "admin@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.platform,
                Set.of(RoleCode.platform_admin.name()),
                "offer:read");

        when(offerRepository.findWithDetailsById(offerId)).thenReturn(Optional.of(offer));

        var response = offerService.getById(offerId);

        assertThat(response.rawName()).isEqualTo("Samsung 990 PRO 1TB");
        assertThat(response.priceIncludesVat()).isTrue();
    }

    @Test
    void getByIdRejectsForeignDistributorOffer() {
        UUID offerId = UUID.randomUUID();
        UUID ownerDistributorId = UUID.randomUUID();
        UUID otherDistributorId = UUID.randomUUID();
        Organization distributor = OfferTestFixtures.sampleDistributor(ownerDistributorId);
        Offer offer = OfferTestFixtures.sampleOffer(offerId, distributor);

        SecurityTestFixtures.setPrincipal(
                otherDistributorId,
                "manager@dist.kz",
                otherDistributorId,
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "offer:read");

        when(offerRepository.findWithDetailsById(offerId)).thenReturn(Optional.of(offer));
        org.mockito.Mockito.doThrow(new SaudaForbiddenException("Access denied"))
                .when(tenantAccessService)
                .assertCurrentOrganization(ownerDistributorId);

        assertThatThrownBy(() -> offerService.getById(offerId))
                .isInstanceOf(SaudaForbiddenException.class);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID offerId = UUID.randomUUID();

        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "admin@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.platform,
                Set.of(RoleCode.platform_admin.name()),
                "offer:read");

        when(offerRepository.findWithDetailsById(offerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.getById(offerId))
                .isInstanceOf(SaudaNotFoundException.class);
    }
}
