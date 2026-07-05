package com.sauda.service.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.repository.ParsedRowRepository;
import com.sauda.service.TenantAccessService;
import com.sauda.service.mapper.ImportRunMapper;
import com.sauda.service.mapper.ParsedRowMapper;
import com.sauda.testsupport.ImportTestFixtures;
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
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ImportRunQueryServiceTest {

    @Mock private ImportRunRepository importRunRepository;
    @Mock private ParsedRowRepository parsedRowRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private TenantAccessService tenantAccessService;

    private final ImportRunMapper importRunMapper = Mappers.getMapper(ImportRunMapper.class);
    private final ParsedRowMapper parsedRowMapper = Mappers.getMapper(ParsedRowMapper.class);

    private ImportRunQueryService importRunQueryService;

    private UUID ownerDistributorId;
    private UUID foreignDistributorId;
    private UUID runId;
    private ImportRun importRun;

    @BeforeEach
    void setUp() {
        importRunQueryService =
                new ImportRunQueryService(
                        importRunRepository,
                        parsedRowRepository,
                        organizationRepository,
                        tenantAccessService,
                        importRunMapper,
                        parsedRowMapper);

        ownerDistributorId = UUID.randomUUID();
        foreignDistributorId = UUID.randomUUID();
        runId = UUID.randomUUID();

        Organization distributor = ImportTestFixtures.sampleDistributor(ownerDistributorId);
        importRun =
                ImportTestFixtures.sampleImportRun(
                        runId,
                        distributor,
                        ImportTestFixtures.sampleRawUpload(UUID.randomUUID(), distributor),
                        ImportStatus.awaiting_approval);
    }

    @Test
    void getRunUsesResolvedTenantForDistributor() {
        when(tenantAccessService.resolveDistributorId(ownerDistributorId))
                .thenReturn(ownerDistributorId);
        when(organizationRepository.existsByIdAndType(ownerDistributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, ownerDistributorId))
                .thenReturn(Optional.of(importRun));

        var response = importRunQueryService.getRun(ownerDistributorId, runId);

        assertThat(response.id()).isEqualTo(runId);
        assertThat(response.distributorId()).isEqualTo(ownerDistributorId);
    }

    @Test
    void getRunNotFoundWhenRunBelongsToAnotherDistributor() {
        when(tenantAccessService.resolveDistributorId(ownerDistributorId))
                .thenReturn(ownerDistributorId);
        when(organizationRepository.existsByIdAndType(ownerDistributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, ownerDistributorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> importRunQueryService.getRun(ownerDistributorId, runId))
                .isInstanceOf(SaudaNotFoundException.class)
                .hasMessageContaining("Import run not found");
    }

    @Test
    void adminCanReadRunForRequestedDistributor() {
        when(tenantAccessService.resolveDistributorId(foreignDistributorId))
                .thenReturn(foreignDistributorId);
        when(organizationRepository.existsByIdAndType(foreignDistributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, foreignDistributorId))
                .thenReturn(Optional.of(importRun));

        var response = importRunQueryService.getRun(foreignDistributorId, runId);

        assertThat(response.distributorId()).isEqualTo(ownerDistributorId);
        verify(tenantAccessService).resolveDistributorId(foreignDistributorId);
    }

    @Test
    void listAllRunsRequiresPlatformAdmin() {
        when(importRunRepository.findAll(org.mockito.ArgumentMatchers.<Specification<ImportRun>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(importRun)));

        importRunQueryService.listAllRuns(null, ImportStatus.awaiting_approval, Pageable.unpaged());

        verify(tenantAccessService).assertPlatformAdmin();
    }

    @Test
    void listAllRunsRejectedForNonAdmin() {
        org.mockito.Mockito.doThrow(new SaudaForbiddenException("Admin access required"))
                .when(tenantAccessService)
                .assertPlatformAdmin();

        assertThatThrownBy(
                        () ->
                                importRunQueryService.listAllRuns(
                                        null, null, Pageable.unpaged()))
                .isInstanceOf(SaudaForbiddenException.class);

        verify(importRunRepository, never())
                .findAll(org.mockito.ArgumentMatchers.<Specification<ImportRun>>any(), any(Pageable.class));
    }

    @Test
    void listRunsScopedToResolvedDistributor() {
        when(tenantAccessService.resolveDistributorId(ownerDistributorId))
                .thenReturn(ownerDistributorId);
        when(organizationRepository.existsByIdAndType(ownerDistributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByDistributorIdOrderByCreatedAtDesc(
                        eq(ownerDistributorId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(importRun)));

        var page = importRunQueryService.listRuns(ownerDistributorId, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        verify(importRunRepository)
                .findByDistributorIdOrderByCreatedAtDesc(eq(ownerDistributorId), any(Pageable.class));
    }
}
