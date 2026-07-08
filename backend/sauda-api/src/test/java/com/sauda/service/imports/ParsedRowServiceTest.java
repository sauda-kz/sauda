package com.sauda.service.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.domain.enums.RoleCode;
import com.sauda.domain.enums.StockStatus;
import com.sauda.dto.imports.UpdateParsedRowRequest;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.repository.ParsedRowRepository;
import com.sauda.service.TenantAccessService;
import com.sauda.service.imports.adapter.ImportRowMapper;
import com.sauda.service.mapper.ParsedRowMapper;
import com.sauda.testsupport.SecurityTestFixtures;
import java.math.BigDecimal;
import java.util.Map;
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

@ExtendWith(MockitoExtension.class)
class ParsedRowServiceTest {

    @Mock private ParsedRowRepository parsedRowRepository;
    @Mock private ImportRunRepository importRunRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private TenantAccessService tenantAccessService;

    private final ImportRowMapper importRowMapper = new ImportRowMapper();
    private final ParsedRowValidator parsedRowValidator = new ParsedRowValidator();
    private final ParsedRowMapper parsedRowMapper = Mappers.getMapper(ParsedRowMapper.class);

    private ParsedRowService parsedRowService;

    private UUID distributorId;
    private UUID otherDistributorId;
    private UUID runId;
    private UUID rowId;
    private UUID userId;
    private ImportRun importRun;
    private ParsedRow parsedRow;
    private AppUser editor;

    @BeforeEach
    void setUp() {
        parsedRowService =
                new ParsedRowService(
                        parsedRowRepository,
                        importRunRepository,
                        organizationRepository,
                        appUserRepository,
                        tenantAccessService,
                        importRowMapper,
                        parsedRowValidator,
                        parsedRowMapper);

        distributorId = UUID.randomUUID();
        otherDistributorId = UUID.randomUUID();
        runId = UUID.randomUUID();
        rowId = UUID.randomUUID();
        userId = UUID.randomUUID();

        Organization distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);

        importRun = new ImportRun();
        importRun.setId(runId);
        importRun.setDistributor(distributor);
        importRun.setStatus(ImportStatus.awaiting_approval);
        importRun.setErrorRowsCount(1);

        parsedRow = new ParsedRow();
        parsedRow.setId(rowId);
        parsedRow.setImportRun(importRun);
        parsedRow.setSourceRowNumber(2);
        parsedRow.setRawRowData(Map.of("sku", "SKU-OLD"));
        parsedRow.setParsedData(Map.of("sku", "SKU-OLD", "name", "Old item"));
        parsedRow.setStatus(ParsedRowStatus.error);
        parsedRow.setErrors(
                java.util.List.of(
                        Map.of(
                                "field",
                                "price",
                                "code",
                                "INVALID_NUMBER",
                                "message",
                                "Invalid price")));

        editor = new AppUser();
        editor.setId(userId);

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
    void editRowFixesValidationErrorsAndMarksEdited() {
        UpdateParsedRowRequest request = validRequest();

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.of(importRun));
        when(parsedRowRepository.findByIdAndImportRunId(rowId, runId))
                .thenReturn(Optional.of(parsedRow));
        when(appUserRepository.getReferenceById(userId)).thenReturn(editor);
        when(parsedRowRepository.save(any(ParsedRow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parsedRowRepository.countByImportRunIdAndStatus(runId, ParsedRowStatus.error))
                .thenReturn(0L);
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = parsedRowService.editRow(distributorId, runId, rowId, request);

        assertThat(response.status()).isEqualTo(ParsedRowStatus.edited);
        assertThat(response.parsedData()).containsEntry("sku", "SKU-001");
        assertThat(response.errors()).isEmpty();
        assertThat(response.editedAt()).isNotNull();

        ArgumentCaptor<ParsedRow> rowCaptor = ArgumentCaptor.forClass(ParsedRow.class);
        verify(parsedRowRepository).save(rowCaptor.capture());
        assertThat(rowCaptor.getValue().getStatus()).isEqualTo(ParsedRowStatus.edited);
        assertThat(rowCaptor.getValue().getEditedBy()).isSameAs(editor);

        ArgumentCaptor<ImportRun> runCaptor = ArgumentCaptor.forClass(ImportRun.class);
        verify(importRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getErrorRowsCount()).isZero();
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(ImportStatus.awaiting_approval);
    }

    @Test
    void editRowKeepsErrorWhenRevalidationFails() {
        UpdateParsedRowRequest request =
                new UpdateParsedRowRequest(
                        "SKU-001",
                        "Fixed item",
                        null,
                        null,
                        new BigDecimal("-1"),
                        true,
                        5,
                        StockStatus.in_stock,
                        1);

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.of(importRun));
        when(parsedRowRepository.findByIdAndImportRunId(rowId, runId))
                .thenReturn(Optional.of(parsedRow));
        when(appUserRepository.getReferenceById(userId)).thenReturn(editor);
        when(parsedRowRepository.save(any(ParsedRow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parsedRowRepository.countByImportRunIdAndStatus(runId, ParsedRowStatus.error))
                .thenReturn(1L);
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = parsedRowService.editRow(distributorId, runId, rowId, request);

        assertThat(response.status()).isEqualTo(ParsedRowStatus.error);
        assertThat(response.errors()).isNotEmpty();
    }

    @Test
    void editRowRejectedWhenImportRunApproved() {
        importRun.setStatus(ImportStatus.approved);

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.of(importRun));

        assertThatThrownBy(
                        () -> parsedRowService.editRow(distributorId, runId, rowId, validRequest()))
                .isInstanceOf(SaudaException.class)
                .hasMessage("Import can no longer be edited");
    }

    @Test
    void editRowNotFoundForForeignTenant() {
        when(tenantAccessService.resolveDistributorId(otherDistributorId))
                .thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(importRunRepository.findByIdAndDistributorId(runId, distributorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                parsedRowService.editRow(
                                        otherDistributorId, runId, rowId, validRequest()))
                .isInstanceOf(SaudaNotFoundException.class)
                .hasMessageContaining("Import run not found");
    }

    private static UpdateParsedRowRequest validRequest() {
        return new UpdateParsedRowRequest(
                "SKU-001",
                "Fixed item",
                "Brand",
                "MPN-1",
                new BigDecimal("100"),
                true,
                5,
                StockStatus.in_stock,
                1);
    }
}
