package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.dto.lotmatch.CreateLotMatchRequest;
import com.sauda.dto.lotmatch.LotMatchResponse;
import com.sauda.dto.lotmatch.UpdateLotMatchStatusRequest;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.service.mapper.LotMatchMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotMatchServiceTest {

    @Mock private LotMatchRepository lotMatchRepository;
    @Mock private LotService lotService;
    @Mock private OfferRepository offerRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private LotMatchMapper lotMatchMapper;

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
                        lotMatchCalculator);

        lotId = UUID.randomUUID();
        offerId = UUID.randomUUID();
        distributorId = UUID.randomUUID();

        lot = new Lot();
        lot.setId(lotId);
        lot.setQuantity(10);
        lot.setBudgetAmount(new BigDecimal("100000"));

        distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);

        offer = new Offer();
        offer.setId(offerId);
        offer.setDistributor(distributor);
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
        when(lotMatchMapper.toResponse(any(LotMatch.class)))
                .thenAnswer(
                        invocation -> {
                            LotMatch match = invocation.getArgument(0);
                            return new LotMatchResponse(
                                    match.getId(),
                                    lotId,
                                    offerId,
                                    distributorId,
                                    match.getMatchStatus(),
                                    match.getConfidenceScore(),
                                    match.getMatchReason(),
                                    Collections.emptyList(),
                                    Collections.emptyList(),
                                    Collections.emptyList(),
                                    match.getRequiredQuantity(),
                                    match.getAvailableQuantity(),
                                    match.getQuantityCheck(),
                                    match.getStockCheck(),
                                    match.getPriceCheck(),
                                    match.getEstimatedUnitPrice(),
                                    match.getEstimatedTotalPrice(),
                                    match.getBudgetAmount(),
                                    match.getEstimatedMargin(),
                                    match.isNeedsManualReview(),
                                    match.getAdminComment(),
                                    match.getDistributorComment(),
                                    null,
                                    null);
                        });

        var response =
                lotMatchService.createMatch(
                        new CreateLotMatchRequest(
                                lotId,
                                offerId,
                                "Brand and model match",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null));

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
    }

    @Test
    void updateStatusForDistributorRejectsPlatformOnlyStatus() {
        UUID matchId = UUID.randomUUID();
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);

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
        when(lotMatchRepository.findByIdAndDistributorId(matchId, distributorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lotMatchService.getForDistributor(distributorId, matchId))
                .isInstanceOf(SaudaNotFoundException.class);
    }
}
