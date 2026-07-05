package com.sauda.service.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.config.ImportProperties;
import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.RawUpload;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.domain.enums.RoleCode;
import com.sauda.dto.imports.ImportRunResponse;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.ImportErrorRepository;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.repository.ParsedRowRepository;
import com.sauda.repository.RawUploadRepository;
import com.sauda.service.TenantAccessService;
import com.sauda.service.mapper.ImportRunMapper;
import com.sauda.testsupport.SecurityTestFixtures;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportRunServiceApprovalTest {

    @Mock private ImportRunRepository importRunRepository;
    @Mock private ParsedRowRepository parsedRowRepository;
    @Mock private ImportErrorRepository importErrorRepository;
    @Mock private RawUploadRepository rawUploadRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private TenantAccessService tenantAccessService;
    @Mock private OfferUpsertService offerUpsertService;

    private final ImportRunMapper importRunMapper = Mappers.getMapper(ImportRunMapper.class);

    private ImportRunService importRunService;

    private UUID distributorId;
    private UUID otherDistributorId;
    private UUID runId;
    private UUID userId;
    private ImportRun importRun;
    private AppUser approver;

    @BeforeEach
    void setUp() {
        importRunService =
                new ImportRunService(
                        importRunRepository,
                        parsedRowRepository,
                        importErrorRepository,
                        rawUploadRepository,
                        organizationRepository,
                        appUserRepository,
                        null,
                        null,
                        new ParsedRowValidator(),
                        new ImportProperties(100, null, 2, 4, 50, 2),
                        tenantAccessService,
                        importRunMapper,
                        offerUpsertService);

        distributorId = UUID.randomUUID();
        otherDistributorId = UUID.randomUUID();
        runId = UUID.randomUUID();
        userId = UUID.randomUUID();

        Organization distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);

        RawUpload rawUpload = new RawUpload();
        rawUpload.setId(UUID.randomUUID());
        rawUpload.setOriginalFilename("prices.csv");

        importRun = new ImportRun();
        importRun.setId(runId);
        importRun.setDistributor(distributor);
        importRun.setRawUpload(rawUpload);
        importRun.setStatus(ImportStatus.awaiting_approval);
        importRun.setTotalRows(3);
        importRun.setParsedRowsCount(3);
        importRun.setErrorRowsCount(1);

        approver = new AppUser();
        approver.setId(userId);

        SecurityTestFixtures.setPrincipal(
                userId,
                "manager@dist.kz",
                distributorId,
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "import:approve");
    }

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void approveRunTransitionsToAppliedAndAppliesOffers() {
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.of(importRun));
        when(parsedRowRepository.countByImportRunId(runId)).thenReturn(3L);
        when(parsedRowRepository.countByImportRunIdAndStatus(runId, ParsedRowStatus.error))
                .thenReturn(1L);
        when(appUserRepository.getReferenceById(userId)).thenReturn(approver);
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            ImportRun run = invocation.getArgument(0);
                            run.setStatus(ImportStatus.applied);
                            return null;
                        })
                .when(offerUpsertService)
                .applyImportRun(any(ImportRun.class));

        ImportRunResponse response = importRunService.approveRun(distributorId, runId);

        assertThat(response.status()).isEqualTo(ImportStatus.applied);
        assertThat(response.approvedAt()).isNotNull();

        org.mockito.ArgumentCaptor<ImportRun> runCaptor =
                org.mockito.ArgumentCaptor.forClass(ImportRun.class);
        verify(importRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getApprovedBy()).isSameAs(approver);

        verify(offerUpsertService).applyImportRun(any(ImportRun.class));
    }

    @Test
    void approveRunRejectedWhenAllRowsAreErrors() {
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.of(importRun));
        when(parsedRowRepository.countByImportRunId(runId)).thenReturn(2L);
        when(parsedRowRepository.countByImportRunIdAndStatus(runId, ParsedRowStatus.error))
                .thenReturn(2L);

        assertThatThrownBy(() -> importRunService.approveRun(distributorId, runId))
                .isInstanceOf(SaudaException.class)
                .hasMessage("Import has no rows that can be applied");

        verify(offerUpsertService, never()).applyImportRun(any());
    }

    @Test
    void approveRunRejectedFromAppliedStatus() {
        importRun.setStatus(ImportStatus.applied);

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.of(importRun));

        assertThatThrownBy(() -> importRunService.approveRun(distributorId, runId))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("Invalid import run status transition");
    }

    @Test
    void rejectRunTransitionsToRejected() {
        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.of(importRun));
        when(appUserRepository.getReferenceById(userId)).thenReturn(approver);
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportRunResponse response =
                importRunService.rejectRun(distributorId, runId, "Wrong format");

        assertThat(response.status()).isEqualTo(ImportStatus.rejected);
        assertThat(response.rejectedAt()).isNotNull();

        verify(offerUpsertService, never()).applyImportRun(any());
    }

    @Test
    void rejectRunNotFoundForForeignTenant() {
        when(tenantAccessService.resolveDistributorId(otherDistributorId))
                .thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> importRunService.rejectRun(otherDistributorId, runId, "reason"))
                .isInstanceOf(SaudaNotFoundException.class)
                .hasMessageContaining("Import run not found");
    }
}
