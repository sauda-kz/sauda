package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.LotStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.RoleCode;
import com.sauda.dto.lotmatch.CreateLotMatchRequest;
import com.sauda.dto.lotmatch.SendLotToDistributorRequest;
import com.sauda.dto.lotmatch.UpdateLotMatchStatusRequest;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.service.mapper.LotMatchMapper;
import com.sauda.testsupport.SecurityTestFixtures;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
    @Mock private InternalNotificationService internalNotificationService;

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
                        tenantAccessService,
                        internalNotificationService);

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
        offer.setStockQuantity(15);
    }

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void sendToDistributorCreatesMatchWithSentTimestamp() {
        setPlatformPrincipal();
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(offerRepository.findWithDistributorById(offerId)).thenReturn(Optional.of(offer));
        when(lotMatchRepository.findByLotIdAndOfferId(lotId, offerId)).thenReturn(Optional.empty());
        when(lotMatchRepository.save(any(LotMatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                lotMatchService.sendToDistributor(
                        lotId,
                        new SendLotToDistributorRequest(
                                offerId, "Category match", List.of("on_order"), "Please review", null));

        ArgumentCaptor<LotMatch> captor = ArgumentCaptor.forClass(LotMatch.class);
        verify(lotMatchRepository).save(captor.capture());
        LotMatch saved = captor.getValue();

        assertThat(saved.getMatchStatus()).isEqualTo(LotMatchStatus.matched);
        assertThat(saved.getSentToDistributorAt()).isNotNull();
        assertThat(saved.getMatchReason()).isEqualTo("Category match");
        assertThat(saved.getRiskFlags()).containsExactly("on_order");
        assertThat(response.status()).isEqualTo(LotMatchStatus.matched);
        verify(internalNotificationService).notifyLotSent(saved);
    }

    @Test
    void sendToDistributorReactivatesExistingDismissedMatch() {
        setPlatformPrincipal();
        UUID matchId = UUID.randomUUID();
        LotMatch existing = buildSavedMatch(matchId);
        existing.setMatchStatus(LotMatchStatus.dismissed);
        existing.setSentToDistributorAt(null);

        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(offerRepository.findWithDistributorById(offerId)).thenReturn(Optional.of(offer));
        when(lotMatchRepository.findByLotIdAndOfferId(lotId, offerId))
                .thenReturn(Optional.of(existing));
        when(lotMatchRepository.save(existing)).thenReturn(existing);

        lotMatchService.sendToDistributor(
                lotId, new SendLotToDistributorRequest(offerId, "Reactivated", null, null, null));

        assertThat(existing.getMatchStatus()).isEqualTo(LotMatchStatus.matched);
        assertThat(existing.getSentToDistributorAt()).isNotNull();
        verify(internalNotificationService).notifyLotSent(existing);
    }

    @Test
    void sendToDistributorIsIdempotentForAlreadySentMatch() {
        setPlatformPrincipal();
        UUID matchId = UUID.randomUUID();
        LotMatch existing = buildSavedMatch(matchId);
        existing.setMatchStatus(LotMatchStatus.matched);
        Instant previousSentAt = Instant.parse("2026-06-01T00:00:00Z");
        existing.setSentToDistributorAt(previousSentAt);

        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(offerRepository.findWithDistributorById(offerId)).thenReturn(Optional.of(offer));
        when(lotMatchRepository.findByLotIdAndOfferId(lotId, offerId))
                .thenReturn(Optional.of(existing));
        when(lotMatchRepository.save(existing)).thenReturn(existing);

        lotMatchService.sendToDistributor(
                lotId,
                new SendLotToDistributorRequest(
                        offerId, "Resent", null, "Updated comment", LotMatchStatus.needs_review));

        assertThat(existing.getMatchStatus()).isEqualTo(LotMatchStatus.needs_review);
        assertThat(existing.getSentToDistributorAt()).isAfter(previousSentAt);
        assertThat(existing.getAdminComment()).isEqualTo("Updated comment");
        verify(lotMatchRepository).save(existing);
    }

    @Test
    void sendToDistributorRejectsArchivedLot() {
        setPlatformPrincipal();
        lot.setStatus(LotStatus.archived);
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);

        assertThatThrownBy(
                        () ->
                                lotMatchService.sendToDistributor(
                                        lotId,
                                        new SendLotToDistributorRequest(
                                                offerId, "reason", null, null, null)))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("archived or cancelled");

        verify(lotMatchRepository, never()).save(any());
    }

    @Test
    void sendToDistributorRejectsNonPlatformUser() {
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "dist@sauda.kz",
                distributorId,
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "lot_match:manage");

        assertThatThrownBy(
                        () ->
                                lotMatchService.sendToDistributor(
                                        lotId,
                                        new SendLotToDistributorRequest(
                                                offerId, "reason", null, null, null)))
                .isInstanceOf(SaudaForbiddenException.class);
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
    void listForDistributorFiltersSentMatchesByDefault() {
        UUID matchId = UUID.randomUUID();
        LotMatch match = buildSavedMatch(matchId);
        match.setSentToDistributorAt(Instant.now());
        match.setMatchStatus(LotMatchStatus.matched);
        Pageable pageable = Pageable.ofSize(20);

        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(lotMatchRepository.findSentForDistributor(
                        eq(distributorId), eq(LotMatchStatus.matched), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(match)));

        var page =
                lotMatchService.listForDistributor(
                        distributorId, LotMatchStatus.matched, false, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().offerName()).isEqualTo("Samsung 990 PRO");
    }

    @Test
    void listForDistributorIncludeUnsentRequiresPlatformUser() {
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "dist@sauda.kz",
                distributorId,
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "lot_match:read");

        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);

        assertThatThrownBy(
                        () ->
                                lotMatchService.listForDistributor(
                                        distributorId, null, true, Pageable.ofSize(20)))
                .isInstanceOf(SaudaForbiddenException.class);
    }

    @Test
    void listForDistributorFiltersByStatus() {
        UUID matchId = UUID.randomUUID();
        LotMatch match = buildSavedMatch(matchId);
        Pageable pageable = Pageable.ofSize(20);

        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(lotMatchRepository.findSentForDistributor(
                        eq(distributorId), eq(LotMatchStatus.suggested), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(match)));

        var page =
                lotMatchService.listForDistributor(
                        distributorId, LotMatchStatus.suggested, false, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().offerName()).isEqualTo("Samsung 990 PRO");
    }

    @Test
    void updateStatusForDistributorUpdatesMatch() {
        UUID matchId = UUID.randomUUID();
        LotMatch match = buildSavedMatch(matchId);
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "dist@sauda.kz",
                distributorId,
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "lot_match:review");

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
        match.setMatchStatus(LotMatchStatus.matched);
        match.setMatchReason("Brand and model match");
        match.setRequiredQuantity(10);
        match.setAvailableQuantity(15);
        match.setSentToDistributorAt(Instant.now());
        return match;
    }

    private static void setPlatformPrincipal() {
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "admin@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.platform,
                Set.of(RoleCode.platform_admin.name()),
                "lot_match:manage");
    }
}
