package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.dto.lotmatch.CreateLotMatchRequest;
import com.sauda.dto.lotmatch.UpdateLotMatchStatusRequest;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.service.mapper.LotMatchMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LotMatchServiceTest {

    @Mock private LotMatchRepository lotMatchRepository;
    @Mock private LotService lotService;
    @Mock private OfferRepository offerRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private TenantAccessService tenantAccessService;

    private final LotMatchMapper lotMatchMapper = Mappers.getMapper(LotMatchMapper.class);
    private final LotMatchCalculator lotMatchCalculator = new LotMatchCalculator();
    private LotMatchService lotMatchService;

    private UUID lotId;
    private UUID offerId;
    private UUID distributorId;
    private Lot lot;
    private Offer offer;
    private Organization distributor;

    @BeforeEach
    void setUp() {
        lotMatchService =
                new LotMatchService(
                        lotMatchRepository,
                        lotService,
                        offerRepository,
                        organizationRepository,
                        lotMatchMapper,
                        lotMatchCalculator,
                        tenantAccessService);

        lotId = UUID.randomUUID();
        offerId = UUID.randomUUID();
        distributorId = UUID.randomUUID();

        lot = new Lot();
        lot.setId(lotId);
        lot.setTitle("SSD 1TB");
        lot.setQuantity(10);
        lot.setBudgetAmount(new BigDecimal("100000"));

        distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);

        offer = new Offer();
        offer.setId(offerId);
        offer.setDistributor(distributor);
        offer.setRawName("Samsung 990 PRO");
        offer.setPrice(new BigDecimal("8000"));
        offer.setStockQty(15);
    }

    @Test
    void createMatchPopulatesDerivedFields() {
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(offerRepository.findWithDistributorById(offerId)).thenReturn(Optional.of(offer));
        when(lotMatchRepository.existsByLotIdAndOfferId(lotId, offerId)).thenReturn(false);
        when(lotMatchRepository.save(any(LotMatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                lotMatchService.createMatch(
                        new CreateLotMatchRequest(
                                lotId,
                                offerId,
                                "Brand and model match",
                                null,
                                null,
                                List.of("brand"),
                                null,
                                null,
                                null,
                                "Admin note"));

        ArgumentCaptor<LotMatch> captor = ArgumentCaptor.forClass(LotMatch.class);
        verify(lotMatchRepository).save(captor.capture());
        LotMatch saved = captor.getValue();

        assertThat(saved.getRequiredQuantity()).isEqualTo(10);
        assertThat(saved.getAvailableQuantity()).isEqualTo(15);
        assertThat(saved.getEstimatedUnitPrice()).isEqualByComparingTo("8000");
        assertThat(saved.getEstimatedTotalPrice()).isEqualByComparingTo("80000");
        assertThat(saved.getEstimatedMargin()).isEqualByComparingTo("20000");
        assertThat(saved.getDistributor().getId()).isEqualTo(distributorId);
        assertThat(response.status()).isEqualTo(LotMatchStatus.suggested);
        assertThat(response.lotId()).isEqualTo(lotId);
        assertThat(response.matchedRequirements()).containsExactly("brand");
    }

    @Test
    void createMatchRejectsDuplicate() {
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(offerRepository.findWithDistributorById(offerId)).thenReturn(Optional.of(offer));
        when(lotMatchRepository.existsByLotIdAndOfferId(lotId, offerId)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                lotMatchService.createMatch(
                                        new CreateLotMatchRequest(
                                                lotId,
                                                offerId,
                                                "duplicate",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createMatchThrowsWhenOfferMissing() {
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(offerRepository.findWithDistributorById(offerId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                lotMatchService.createMatch(
                                        new CreateLotMatchRequest(
                                                lotId, offerId, "reason", null, null, null, null,
                                                null, null, null)))
                .isInstanceOf(SaudaNotFoundException.class);
    }

    @Test
    void listByLotReturnsMappedPage() {
        UUID matchId = UUID.randomUUID();
        LotMatch match = buildSavedMatch(matchId);
        Pageable pageable = Pageable.ofSize(20);

        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(lotMatchRepository.findByLotId(lotId, pageable))
                .thenReturn(new PageImpl<>(List.of(match)));

        var page = lotMatchService.listByLot(lotId, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().id()).isEqualTo(matchId);
    }

    @Test
    void getMatchReturnsMappedResponse() {
        UUID matchId = UUID.randomUUID();
        when(lotMatchRepository.findById(matchId))
                .thenReturn(Optional.of(buildSavedMatch(matchId)));

        var response = lotMatchService.getMatch(matchId);

        assertThat(response.id()).isEqualTo(matchId);
        assertThat(response.offerId()).isEqualTo(offerId);
    }

    @Test
    void listForDistributorFiltersByStatus() {
        UUID matchId = UUID.randomUUID();
        LotMatch match = buildSavedMatch(matchId);
        Pageable pageable = Pageable.ofSize(20);

        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(lotMatchRepository.findByDistributorIdAndMatchStatusOrderByCreatedAtDesc(
                        eq(distributorId), eq(LotMatchStatus.suggested), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(match)));

        var page =
                lotMatchService.listForDistributor(
                        distributorId, LotMatchStatus.suggested, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().offerName()).isEqualTo("Samsung 990 PRO");
    }

    @Test
    void updateStatusForDistributorUpdatesMatch() {
        UUID matchId = UUID.randomUUID();
        LotMatch match = buildSavedMatch(matchId);

        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(lotMatchRepository.findByIdAndDistributorId(matchId, distributorId))
                .thenReturn(Optional.of(match));
        when(lotMatchRepository.save(match)).thenReturn(match);

        var card =
                lotMatchService.updateStatusForDistributor(
                        distributorId,
                        matchId,
                        new UpdateLotMatchStatusRequest(
                                LotMatchStatus.interested, "Готовы участвовать"));

        assertThat(card.status()).isEqualTo(LotMatchStatus.interested);
        assertThat(card.distributorComment()).isEqualTo("Готовы участвовать");
    }

    @Test
    void updateStatusForDistributorRejectsPlatformOnlyStatus() {
        UUID matchId = UUID.randomUUID();
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);

        assertThatThrownBy(
                        () ->
                                lotMatchService.updateStatusForDistributor(
                                        distributorId,
                                        matchId,
                                        new UpdateLotMatchStatusRequest(
                                                LotMatchStatus.matched, null)))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void getForDistributorThrowsWhenTenantMismatch() {
        UUID matchId = UUID.randomUUID();
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(lotMatchRepository.findByIdAndDistributorId(matchId, distributorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lotMatchService.getForDistributor(distributorId, matchId))
                .isInstanceOf(SaudaNotFoundException.class);
    }

    @Test
    void getForDistributorThrowsWhenOrgIsNotDistributor() {
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(false);
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);

        assertThatThrownBy(
                        () -> lotMatchService.getForDistributor(distributorId, UUID.randomUUID()))
                .isInstanceOf(SaudaNotFoundException.class)
                .hasMessageContaining("Distributor not found");
    }

    private LotMatch buildSavedMatch(UUID matchId) {
        LotMatch match = new LotMatch();
        match.setId(matchId);
        match.setLot(lot);
        match.setOffer(offer);
        match.setDistributor(distributor);
        match.setMatchStatus(LotMatchStatus.suggested);
        match.setMatchReason("Brand and model match");
        match.setRequiredQuantity(10);
        match.setAvailableQuantity(15);
        return match;
    }
}
