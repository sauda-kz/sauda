package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.RoleCode;
import com.sauda.domain.enums.StockStatus;
import com.sauda.dto.lotmatch.PotentialMatchResponse;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.OfferRepository;
import com.sauda.testsupport.OfferTestFixtures;
import com.sauda.testsupport.SecurityTestFixtures;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class LotMatchSuggestionServiceTest {

    @Mock private LotService lotService;
    @Mock private OfferRepository offerRepository;
    @Mock private LotMatchRepository lotMatchRepository;
    @Spy private LotMatchCalculator lotMatchCalculator = new LotMatchCalculator();

    @InjectMocks private LotMatchSuggestionService lotMatchSuggestionService;

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void suggestForLotReturnsOffersMatchingCategory() {
        UUID lotId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        Organization distributor = OfferTestFixtures.sampleDistributor(UUID.randomUUID());
        Offer offer = OfferTestFixtures.sampleOffer(offerId, distributor);
        Lot lot = sampleLot(lotId);

        setPlatformPrincipal();
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(lotMatchRepository.findMatchedOfferIdsByLotId(lotId)).thenReturn(List.of());
        when(offerRepository.findAll(any(Specification.class))).thenReturn(List.of(offer));

        List<PotentialMatchResponse> suggestions = lotMatchSuggestionService.suggestForLot(lotId);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.getFirst().offerId()).isEqualTo(offerId);
        assertThat(suggestions.getFirst().matchReason()).contains("Совпала категория: SSD");
        assertThat(suggestions.getFirst().confidenceScore()).isGreaterThanOrEqualTo(new BigDecimal("0.40"));
    }

    @Test
    void suggestForLotFillsMissingDataForZeroStockAndWarranty() {
        UUID lotId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        Organization distributor = OfferTestFixtures.sampleDistributor(UUID.randomUUID());
        Offer offer = OfferTestFixtures.sampleOffer(offerId, distributor);
        offer.setStockQuantity(0);
        offer.setStockStatus(StockStatus.out_of_stock);
        offer.setLeadTime(null);
        Lot lot = sampleLot(lotId);

        setPlatformPrincipal();
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(lotMatchRepository.findMatchedOfferIdsByLotId(lotId)).thenReturn(List.of());
        when(offerRepository.findAll(any(Specification.class))).thenReturn(List.of(offer));

        PotentialMatchResponse suggestion =
                lotMatchSuggestionService.suggestForLot(lotId).getFirst();

        assertThat(suggestion.missingData())
                .contains(
                        "остаток = 0",
                        "статус наличия требует проверки",
                        "срок поставки не указан",
                        "гарантия в предложении не указана");
        assertThat(suggestion.recommendedStatus()).isEqualTo(LotMatchStatus.needs_review);
        assertThat(suggestion.stockCheck()).isEqualTo("fail");
    }

    @Test
    void suggestForLotReturnsEmptyListWhenNoOffers() {
        UUID lotId = UUID.randomUUID();
        Lot lot = sampleLot(lotId);

        setPlatformPrincipal();
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(lotMatchRepository.findMatchedOfferIdsByLotId(lotId)).thenReturn(List.of());
        when(offerRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThat(lotMatchSuggestionService.suggestForLot(lotId)).isEmpty();
    }

    @Test
    void suggestForLotExcludesAlreadyMatchedOffers() {
        UUID lotId = UUID.randomUUID();
        UUID matchedOfferId = UUID.randomUUID();
        Lot lot = sampleLot(lotId);

        setPlatformPrincipal();
        when(lotService.findLotOrThrow(lotId)).thenReturn(lot);
        when(lotMatchRepository.findMatchedOfferIdsByLotId(lotId))
                .thenReturn(List.of(matchedOfferId));
        when(offerRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThat(lotMatchSuggestionService.suggestForLot(lotId)).isEmpty();
    }

    @Test
    void suggestForLotRejectsNonPlatformUsers() {
        UUID lotId = UUID.randomUUID();
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "dist@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "lot_match:read");

        assertThatThrownBy(() -> lotMatchSuggestionService.suggestForLot(lotId))
                .isInstanceOf(SaudaForbiddenException.class);
    }

    private static Lot sampleLot(UUID lotId) {
        Lot lot = new Lot();
        lot.setId(lotId);
        lot.setCategory("SSD");
        lot.setTitle("SSD 1TB");
        lot.setTechnicalRequirements("NVMe Samsung 990 PRO");
        lot.setQuantity(10);
        lot.setBudgetAmount(new BigDecimal("500000"));
        lot.setWarrantyRequirements("24 месяца");
        return lot;
    }

    private static void setPlatformPrincipal() {
        SecurityTestFixtures.setPrincipal(
                UUID.randomUUID(),
                "admin@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.platform,
                Set.of(RoleCode.platform_admin.name()),
                "lot_match:read");
    }
}
