package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Lot;
import com.sauda.domain.enums.LotDataQualityStatus;
import com.sauda.domain.enums.LotStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.exception.SaudaIncompleteLotException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.LotMatchCountView;
import com.sauda.repository.LotMatchRepository;
import com.sauda.repository.LotRepository;
import com.sauda.security.principal.SaudaPrincipal;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.LotMapper;
import com.sauda.testsupport.LotTestFixtures;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {

    @Mock private LotRepository lotRepository;
    @Mock private LotMatchRepository lotMatchRepository;
    @Mock private AppUserRepository appUserRepository;

    private final LotMapper lotMapper = Mappers.getMapper(LotMapper.class);
    private final LotValidationService lotValidationService = new LotValidationService();
    private LotService lotService;

    @BeforeEach
    void setUp() {
        lotService =
                new LotService(
                        lotRepository,
                        lotMatchRepository,
                        appUserRepository,
                        lotMapper,
                        lotValidationService);
    }

    @Test
    void createLotMapsRequestToEntityAndBack() {
        UUID userId = UUID.randomUUID();
        AppUser creator = new AppUser();
        creator.setId(userId);

        when(lotRepository.save(any(Lot.class)))
                .thenAnswer(
                        invocation -> {
                            Lot lot = invocation.getArgument(0);
                            lot.setId(UUID.randomUUID());
                            return lot;
                        });
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(creator));

        try (var securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requirePrincipal).thenReturn(samplePrincipal(userId));

            var response = lotService.createLot(LotTestFixtures.sampleCreateLotRequest());

            assertThat(response.externalLotId()).isEqualTo("LOT-001");
            assertThat(response.title()).isEqualTo("SSD 1TB");
            assertThat(response.status()).isEqualTo(LotStatus.active);
            assertThat(response.createdById()).isEqualTo(userId);
            assertThat(response.dataQualityStatus()).isEqualTo(LotDataQualityStatus.complete);
        }
    }

    @Test
    void createIncompleteLotWithoutConfirmationThrows() {
        assertThatThrownBy(() -> lotService.createLot(LotTestFixtures.incompleteCreateLotRequest()))
                .isInstanceOf(SaudaIncompleteLotException.class)
                .satisfies(
                        ex -> {
                            var warning = ((SaudaIncompleteLotException) ex).getWarning();
                            assertThat(warning.missingFields()).contains("budgetAmount");
                        });
    }

    @Test
    void createIncompleteLotWithConfirmationSetsNeedsReview() {
        UUID userId = UUID.randomUUID();
        AppUser creator = new AppUser();
        creator.setId(userId);

        var request =
                new com.sauda.dto.lot.CreateLotRequest(
                        LotTestFixtures.incompleteCreateLotRequest().source(),
                        LotTestFixtures.incompleteCreateLotRequest().externalPurchaseId(),
                        LotTestFixtures.incompleteCreateLotRequest().externalLotId(),
                        LotTestFixtures.incompleteCreateLotRequest().title(),
                        LotTestFixtures.incompleteCreateLotRequest().description(),
                        LotTestFixtures.incompleteCreateLotRequest().customerName(),
                        LotTestFixtures.incompleteCreateLotRequest().category(),
                        LotTestFixtures.incompleteCreateLotRequest().procurementMethod(),
                        LotTestFixtures.incompleteCreateLotRequest().lotType(),
                        LotTestFixtures.incompleteCreateLotRequest().quantity(),
                        LotTestFixtures.incompleteCreateLotRequest().unit(),
                        LotTestFixtures.incompleteCreateLotRequest().budgetAmount(),
                        LotTestFixtures.incompleteCreateLotRequest().currency(),
                        LotTestFixtures.incompleteCreateLotRequest().deliveryLocation(),
                        LotTestFixtures.incompleteCreateLotRequest().deliveryDeadline(),
                        LotTestFixtures.incompleteCreateLotRequest().submissionDeadline(),
                        LotTestFixtures.incompleteCreateLotRequest().warrantyRequirements(),
                        LotTestFixtures.incompleteCreateLotRequest().technicalRequirements(),
                        LotTestFixtures.incompleteCreateLotRequest().requiredDocuments(),
                        LotTestFixtures.incompleteCreateLotRequest().qualificationRequirements(),
                        LotTestFixtures.incompleteCreateLotRequest().contractTermsSummary(),
                        LotTestFixtures.incompleteCreateLotRequest().publishedAt(),
                        LotStatus.active,
                        LotTestFixtures.incompleteCreateLotRequest().sourceUrl(),
                        LotTestFixtures.incompleteCreateLotRequest().rawText(),
                        true);

        when(lotRepository.save(any(Lot.class)))
                .thenAnswer(
                        invocation -> {
                            Lot lot = invocation.getArgument(0);
                            lot.setId(UUID.randomUUID());
                            return lot;
                        });
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(creator));

        try (var securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requirePrincipal).thenReturn(samplePrincipal(userId));

            var response = lotService.createLot(request);

            assertThat(response.status()).isEqualTo(LotStatus.needs_review);
            assertThat(response.dataQualityStatus()).isEqualTo(LotDataQualityStatus.needs_review);
        }
    }

    @Test
    void updateLotAppliesChanges() {
        UUID lotId = UUID.randomUUID();
        Lot existing = lotMapper.toEntity(LotTestFixtures.sampleCreateLotRequest());
        existing.setId(lotId);

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(existing));
        when(lotRepository.save(existing)).thenReturn(existing);
        when(lotMatchRepository.countByLotId(lotId)).thenReturn(2L);

        var response = lotService.updateLot(lotId, LotTestFixtures.sampleUpdateLotRequest());

        assertThat(response.title()).isEqualTo("SSD 1TB updated");
        assertThat(response.status()).isEqualTo(LotStatus.archived);
        assertThat(response.matchCount()).isEqualTo(2L);
    }

    @Test
    void archiveLotSetsArchivedStatus() {
        UUID lotId = UUID.randomUUID();
        Lot existing = lotMapper.toEntity(LotTestFixtures.sampleCreateLotRequest());
        existing.setId(lotId);

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(existing));
        when(lotRepository.save(existing)).thenReturn(existing);
        when(lotMatchRepository.countByLotId(lotId)).thenReturn(0L);

        var response = lotService.archiveLot(lotId);

        assertThat(response.status()).isEqualTo(LotStatus.archived);
    }

    @Test
    void listLotsWithFiltersAndMatchCount() {
        Lot lot = lotMapper.toEntity(LotTestFixtures.sampleCreateLotRequest());
        lot.setId(UUID.randomUUID());
        Pageable pageable = Pageable.ofSize(20);

        when(lotRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lot)));
        when(lotMatchRepository.countMatchesByLotIds(List.of(lot.getId())))
                .thenReturn(List.of(matchCountView(lot.getId(), 3L)));

        var page = lotService.listLots(LotStatus.active, "SSD", "SSD", "manual", pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().matchCount()).isEqualTo(3L);
    }

    @Test
    void getLotThrowsWhenMissing() {
        UUID lotId = UUID.randomUUID();
        when(lotRepository.findById(lotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lotService.getLot(lotId))
                .isInstanceOf(SaudaNotFoundException.class);
    }

    private static SaudaPrincipal samplePrincipal(UUID userId) {
        return new SaudaPrincipal(
                userId,
                "admin@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.platform,
                Set.of("platform_admin"),
                SaudaPrincipal.toAuthorities(Set.of("lot:create")));
    }

    private static LotMatchCountView matchCountView(UUID lotId, long count) {
        return new LotMatchCountView() {
            @Override
            public UUID getLotId() {
                return lotId;
            }

            @Override
            public long getMatchCount() {
                return count;
            }
        };
    }
}
