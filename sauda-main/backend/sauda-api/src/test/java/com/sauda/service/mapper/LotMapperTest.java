package com.sauda.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.domain.entity.Lot;
import com.sauda.testsupport.LotTestFixtures;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class LotMapperTest {

    private final LotMapper lotMapper = Mappers.getMapper(LotMapper.class);

    @Test
    void mapsLotEntityToResponse() {
        Lot lot = lotMapper.toEntity(LotTestFixtures.sampleCreateLotRequest());
        lot.setId(UUID.randomUUID());

        var response = lotMapper.toResponse(lot);

        assertThat(response.id()).isEqualTo(lot.getId());
        assertThat(response.title()).isEqualTo("SSD 1TB");
        assertThat(response.technicalRequirements()).isEqualTo("NVMe, 1TB");
    }

    @Test
    void updatesExistingLotFromRequest() {
        Lot lot = lotMapper.toEntity(LotTestFixtures.sampleCreateLotRequest());
        lotMapper.updateEntity(lot, LotTestFixtures.sampleUpdateLotRequest());

        assertThat(lot.getTitle()).isEqualTo("SSD 1TB updated");
    }
}
