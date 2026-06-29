package com.sauda.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Offer;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.LotMatchStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.StockStatus;
import com.sauda.testsupport.LotTestFixtures;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class LotMatchMapperTest {

    private final LotMatchMapper lotMatchMapper = Mappers.getMapper(LotMatchMapper.class);

    @Test
    void mapsLotMatchToResponse() {
        LotMatch match = sampleMatch();

        var response = lotMatchMapper.toResponse(match);

        assertThat(response.lotId()).isEqualTo(match.getLot().getId());
        assertThat(response.offerId()).isEqualTo(match.getOffer().getId());
        assertThat(response.status()).isEqualTo(LotMatchStatus.suggested);
        assertThat(response.matchedRequirements()).containsExactly("brand");
    }

    @Test
    void mapsLotMatchToDistributorCard() {
        LotMatch match = sampleMatch();

        var card = lotMatchMapper.toDistributorCard(match);

        assertThat(card.matchId()).isEqualTo(match.getId());
        assertThat(card.title()).isEqualTo("SSD 1TB");
        assertThat(card.offerName()).isEqualTo("Samsung 990 PRO 1TB");
        assertThat(card.customerName()).isEqualTo("АО Заказчик");
        assertThat(card.stockQuantity()).isEqualTo(120);
        assertThat(card.stockStatus()).isEqualTo(StockStatus.in_stock);
        assertThat(card.priceIncludesVat()).isTrue();
    }

    @Test
    void safeListReturnsEmptyForNullRequirements() {
        LotMatch match = sampleMatch();
        match.setMatchedRequirements(null);
        match.setMissingRequirements(null);
        match.setRiskFlags(null);

        var response = lotMatchMapper.toResponse(match);

        assertThat(response.matchedRequirements()).isEmpty();
        assertThat(response.missingRequirements()).isEmpty();
        assertThat(response.riskFlags()).isEmpty();
    }

    private static LotMatch sampleMatch() {
        Lot lot =
                Mappers.getMapper(LotMapper.class)
                        .toEntity(LotTestFixtures.sampleCreateLotRequest());
        lot.setId(UUID.randomUUID());

        Organization distributor = new Organization();
        distributor.setId(UUID.randomUUID());
        distributor.setType(OrganizationType.distributor);
        distributor.setName("Distributor");

        Offer offer = new Offer();
        offer.setId(UUID.randomUUID());
        offer.setDistributor(distributor);
        offer.setRawName("Samsung 990 PRO 1TB");
        offer.setBrand("Samsung");
        offer.setModelMpn("990 PRO");
        offer.setPrice(new BigDecimal("45000"));
        offer.setStockQuantity(120);
        offer.setStockStatus(StockStatus.in_stock);
        offer.setPriceIncludesVat(true);

        LotMatch match = new LotMatch();
        match.setId(UUID.randomUUID());
        match.setLot(lot);
        match.setOffer(offer);
        match.setDistributor(distributor);
        match.setMatchStatus(LotMatchStatus.suggested);
        match.setMatchReason("Model match");
        match.setMatchedRequirements(List.of("brand"));
        match.setRequiredQuantity(10);
        match.setAvailableQuantity(120);
        match.setEstimatedUnitPrice(new BigDecimal("45000"));
        match.setEstimatedTotalPrice(new BigDecimal("450000"));
        match.setBudgetAmount(lot.getBudgetAmount());
        match.setConfidenceScore(new BigDecimal("0.85"));
        return match;
    }
}
