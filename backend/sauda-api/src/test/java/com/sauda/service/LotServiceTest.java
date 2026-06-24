package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.enums.LotStatus;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.LotRepository;
import com.sauda.service.mapper.LotMapper;
import com.sauda.testsupport.LotTestFixtures;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {

    @Mock private LotRepository lotRepository;

    private final LotMapper lotMapper = Mappers.getMapper(LotMapper.class);
    private LotService lotService;

    @BeforeEach
    void setUp() {
        lotService = new LotService(lotRepository, lotMapper);
    }

    @Test
    void createLotMapsRequestToEntityAndBack() {
        when(lotRepository.save(any(Lot.class)))
                .thenAnswer(
                        invocation -> {
                            Lot lot = invocation.getArgument(0);
                            lot.setId(UUID.randomUUID());
                            return lot;
                        });

        var response = lotService.createLot(LotTestFixtures.sampleCreateLotRequest());

        assertThat(response.externalLotId()).isEqualTo("LOT-001");
        assertThat(response.title()).isEqualTo("SSD 1TB");
        assertThat(response.status()).isEqualTo(LotStatus.active);
    }

    @Test
    void updateLotAppliesChanges() {
        UUID lotId = UUID.randomUUID();
        Lot existing = lotMapper.toEntity(LotTestFixtures.sampleCreateLotRequest());
        existing.setId(lotId);

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(existing));
        when(lotRepository.save(existing)).thenReturn(existing);

        var response = lotService.updateLot(lotId, LotTestFixtures.sampleUpdateLotRequest());

        assertThat(response.title()).isEqualTo("SSD 1TB updated");
        assertThat(response.status()).isEqualTo(LotStatus.archived);
    }

    @Test
    void archiveLotSetsArchivedStatus() {
        UUID lotId = UUID.randomUUID();
        Lot existing = lotMapper.toEntity(LotTestFixtures.sampleCreateLotRequest());
        existing.setId(lotId);

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(existing));
        when(lotRepository.save(existing)).thenReturn(existing);

        var response = lotService.archiveLot(lotId);

        assertThat(response.status()).isEqualTo(LotStatus.archived);
    }

    @Test
    void listLotsWithStatusFilter() {
        Lot lot = lotMapper.toEntity(LotTestFixtures.sampleCreateLotRequest());
        lot.setId(UUID.randomUUID());
        Pageable pageable = Pageable.ofSize(20);

        when(lotRepository.findByStatus(LotStatus.active, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(lot)));

        var page = lotService.listLots(LotStatus.active, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().externalLotId()).isEqualTo("LOT-001");
    }

    @Test
    void getLotThrowsWhenMissing() {
        UUID lotId = UUID.randomUUID();
        when(lotRepository.findById(lotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lotService.getLot(lotId))
                .isInstanceOf(SaudaNotFoundException.class);
    }
}
