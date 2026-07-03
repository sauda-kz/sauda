package com.sauda.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.testsupport.OfferTestFixtures;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OfferMapperTest {

    private final OfferMapper offerMapper = Mappers.getMapper(OfferMapper.class);

    @Test
    void mapsOfferToResponse() {
        UUID distributorId = UUID.randomUUID();
        var distributor = OfferTestFixtures.sampleDistributor(distributorId);
        var offer = OfferTestFixtures.sampleOffer(UUID.randomUUID(), distributor);

        var response = offerMapper.toResponse(offer);

        assertThat(response.distributorId()).isEqualTo(distributorId);
        assertThat(response.distributorName()).isEqualTo("Tech Distributor");
        assertThat(response.category()).isEqualTo("SSD");
        assertThat(response.stockQuantity()).isEqualTo(120);
    }
}
