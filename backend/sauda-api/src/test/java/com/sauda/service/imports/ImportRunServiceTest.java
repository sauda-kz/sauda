package com.sauda.service.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.config.ImportProperties;
import com.sauda.domain.entity.ImportError;
import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.entity.RawUpload;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.domain.enums.RawUploadStatus;
import com.sauda.domain.enums.StockStatus;
import com.sauda.exception.SaudaException;
import com.sauda.integration.storage.upload.StoredFileUploadService;
import com.sauda.repository.ImportErrorRepository;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.ParsedRowRepository;
import com.sauda.repository.RawUploadRepository;
import com.sauda.service.TenantAccessService;
import com.sauda.service.imports.adapter.ImportAdapter;
import com.sauda.service.imports.adapter.ImportAdapterRegistry;
import com.sauda.service.imports.adapter.ImportSource;
import com.sauda.service.imports.model.AdapterParseResult;
import com.sauda.service.imports.model.AdapterParsedRow;
import com.sauda.service.imports.model.ImportRowFields;
import com.sauda.service.imports.model.RowError;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ImportRunServiceTest {

    @Mock private ImportRunRepository importRunRepository;
    @Mock private ParsedRowRepository parsedRowRepository;
    @Mock private ImportErrorRepository importErrorRepository;
    @Mock private RawUploadRepository rawUploadRepository;
    @Mock private StoredFileUploadService storedFileUploadService;
    @Mock private ImportAdapterRegistry importAdapterRegistry;
    @Mock private ImportAdapter importAdapter;
    @Mock private TenantAccessService tenantAccessService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ImportRunService importRunService;

    private UUID rawUploadId;
    private UUID importRunId;
    private UUID distributorId;
    private RawUpload rawUpload;
    private ImportRun importRun;

    @BeforeEach
    void setUp() {
        importRunService =
                new ImportRunService(
                        importRunRepository,
                        parsedRowRepository,
                        importErrorRepository,
                        rawUploadRepository,
                        null,
                        null,
                        storedFileUploadService,
                        importAdapterRegistry,
                        new ParsedRowValidator(),
                        new ImportProperties(100, null, 2, 4, 50, 2),
                        tenantAccessService,
                        null,
                        eventPublisher);

        rawUploadId = UUID.randomUUID();
        importRunId = UUID.randomUUID();
        distributorId = UUID.randomUUID();

        Organization distributor = new Organization();
        distributor.setId(distributorId);

        rawUpload = new RawUpload();
        rawUpload.setId(rawUploadId);
        rawUpload.setDistributor(distributor);
        rawUpload.setOriginalFilename("prices.csv");
        rawUpload.setMimeType("text/csv");
        rawUpload.setStoragePath("raw/" + distributorId + "/prices.csv");
        rawUpload.setStatus(RawUploadStatus.uploaded);

        importRun = new ImportRun();
        importRun.setId(importRunId);
        importRun.setRawUpload(rawUpload);
        importRun.setDistributor(distributor);
        importRun.setStatus(ImportStatus.pending);
    }

    @Test
    void createRunPersistsPendingImportRun() {
        when(rawUploadRepository.findById(rawUploadId)).thenReturn(Optional.of(rawUpload));
        when(importRunRepository.save(any(ImportRun.class)))
                .thenAnswer(
                        invocation -> {
                            ImportRun run = invocation.getArgument(0);
                            run.setId(importRunId);
                            return run;
                        });

        UUID createdRunId = importRunService.createRun(rawUploadId);

        assertThat(createdRunId).isEqualTo(importRunId);
        ArgumentCaptor<ImportRun> captor = ArgumentCaptor.forClass(ImportRun.class);
        verify(importRunRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ImportStatus.pending);
        assertThat(captor.getValue().getRawUpload()).isSameAs(rawUpload);
        assertThat(captor.getValue().getSourceFilename()).isEqualTo("prices.csv");
    }

    @Test
    void processSavesParsedRowsAndMovesToAwaitingApproval() {
        when(importRunRepository.findWithDistributorById(importRunId))
                .thenReturn(Optional.of(importRun));
        when(importRunRepository.save(any(ImportRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importAdapterRegistry.resolve(any(ImportSource.class))).thenReturn(importAdapter);
        when(importAdapter.key()).thenReturn("csv_v1");
        when(importAdapter.parse(any(ImportSource.class)))
                .thenReturn(
                        new AdapterParseResult(
                                List.of(
                                        parsedAdapterRow(
                                                2,
                                                "SKU-001",
                                                List.of(),
                                                List.of(
                                                        new RowError(
                                                                "price_includes_vat",
                                                                "MISSING",
                                                                "price_includes_vat is not set"))),
                                        parsedAdapterRow(
                                                3,
                                                "SKU-002",
                                                List.of(new RowError("price", "INVALID_NUMBER", "Invalid price")),
                                                List.of()))));
        when(parsedRowRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        importRunService.process(importRunId);

        ArgumentCaptor<ImportRun> runCaptor = ArgumentCaptor.forClass(ImportRun.class);
        verify(importRunRepository, org.mockito.Mockito.atLeastOnce()).save(runCaptor.capture());
        ImportRun savedRun = runCaptor.getAllValues().getLast();
        assertThat(savedRun.getStatus()).isEqualTo(ImportStatus.awaiting_approval);
        assertThat(savedRun.getAdapterKey()).isEqualTo("csv_v1");
        assertThat(savedRun.getTotalRows()).isEqualTo(2);
        assertThat(savedRun.getParsedRowsCount()).isEqualTo(2);
        assertThat(savedRun.getErrorRowsCount()).isEqualTo(1);
        assertThat(savedRun.getStartedAt()).isNotNull();
        assertThat(savedRun.getFinishedAt()).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ParsedRow>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(parsedRowRepository, org.mockito.Mockito.atLeastOnce()).saveAll(rowsCaptor.capture());
        List<ParsedRow> savedRows = rowsCaptor.getAllValues().stream().flatMap(List::stream).toList();
        assertThat(savedRows).hasSize(2);
        assertThat(savedRows.get(0).getStatus()).isEqualTo(ParsedRowStatus.needs_review);
        assertThat(savedRows.get(1).getStatus()).isEqualTo(ParsedRowStatus.error);

        verify(rawUploadRepository).save(rawUpload);
        assertThat(rawUpload.getStatus()).isEqualTo(RawUploadStatus.processed);
        verify(importErrorRepository, never()).save(any());
    }

    @Test
    void processMarksRunFailedWhenAdapterResolutionFails() {
        when(importRunRepository.findWithDistributorById(importRunId))
                .thenReturn(Optional.of(importRun));
        when(importRunRepository.save(any(ImportRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importAdapterRegistry.resolve(any(ImportSource.class)))
                .thenThrow(new SaudaException("Unsupported import format: prices.csv (text/csv)"));
        when(importErrorRepository.save(any(ImportError.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        importRunService.process(importRunId);

        ArgumentCaptor<ImportRun> runCaptor = ArgumentCaptor.forClass(ImportRun.class);
        verify(importRunRepository, org.mockito.Mockito.atLeastOnce()).save(runCaptor.capture());
        assertThat(runCaptor.getAllValues().getLast().getStatus()).isEqualTo(ImportStatus.failed);

        ArgumentCaptor<ImportError> errorCaptor = ArgumentCaptor.forClass(ImportError.class);
        verify(importErrorRepository).save(errorCaptor.capture());
        assertThat(errorCaptor.getValue().getReason()).contains("Unsupported import format");

        verify(parsedRowRepository, never()).saveAll(anyList());
        verify(rawUploadRepository, never()).save(rawUpload);
    }

    private static AdapterParsedRow parsedAdapterRow(
            int rowNumber, String sku, List<RowError> errors, List<RowError> warnings) {
        ImportRowFields fields =
                new ImportRowFields(
                        sku,
                        "Item " + sku,
                        "Brand",
                        "MPN",
                        new BigDecimal("100"),
                        null,
                        5,
                        StockStatus.in_stock,
                        1);
        return new AdapterParsedRow(
                rowNumber, Map.of("sku", sku, "name", "Item " + sku), fields, errors, warnings);
    }
}
