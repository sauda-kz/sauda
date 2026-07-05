package com.sauda.service.imports;

import com.sauda.config.ImportProperties;
import com.sauda.domain.entity.ImportError;
import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.entity.RawUpload;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.domain.enums.RawUploadStatus;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.integration.storage.StoredObject;
import com.sauda.integration.storage.upload.StoredFileUploadService;
import com.sauda.repository.ImportErrorRepository;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.ParsedRowRepository;
import com.sauda.repository.RawUploadRepository;
import com.sauda.service.imports.adapter.ImportAdapter;
import com.sauda.service.imports.adapter.ImportAdapterRegistry;
import com.sauda.service.imports.adapter.ImportSource;
import com.sauda.service.imports.model.AdapterParseResult;
import com.sauda.service.imports.model.AdapterParsedRow;
import com.sauda.service.imports.model.RowError;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ImportRunService {

    private final ImportRunRepository importRunRepository;
    private final ParsedRowRepository parsedRowRepository;
    private final ImportErrorRepository importErrorRepository;
    private final RawUploadRepository rawUploadRepository;
    private final StoredFileUploadService storedFileUploadService;
    private final ImportAdapterRegistry importAdapterRegistry;
    private final ParsedRowValidator parsedRowValidator;
    private final ImportProperties importProperties;

    public ImportRunService(
            ImportRunRepository importRunRepository,
            ParsedRowRepository parsedRowRepository,
            ImportErrorRepository importErrorRepository,
            RawUploadRepository rawUploadRepository,
            StoredFileUploadService storedFileUploadService,
            ImportAdapterRegistry importAdapterRegistry,
            ParsedRowValidator parsedRowValidator,
            ImportProperties importProperties) {
        this.importRunRepository = importRunRepository;
        this.parsedRowRepository = parsedRowRepository;
        this.importErrorRepository = importErrorRepository;
        this.rawUploadRepository = rawUploadRepository;
        this.storedFileUploadService = storedFileUploadService;
        this.importAdapterRegistry = importAdapterRegistry;
        this.parsedRowValidator = parsedRowValidator;
        this.importProperties = importProperties;
    }

    @Transactional
    public UUID createRun(UUID rawUploadId) {
        RawUpload rawUpload =
                rawUploadRepository
                        .findById(rawUploadId)
                        .orElseThrow(
                                () ->
                                        new SaudaNotFoundException(
                                                "Raw upload not found: " + rawUploadId));

        ImportRun importRun = new ImportRun();
        importRun.setRawUpload(rawUpload);
        importRun.setDistributor(rawUpload.getDistributor());
        importRun.setSourceFilename(rawUpload.getOriginalFilename());
        importRun.setStatus(ImportStatus.pending);

        ImportRun saved = importRunRepository.save(importRun);
        log.info(
                "Import run created: runId={}, rawUploadId={}, distributorId={}",
                saved.getId(),
                rawUploadId,
                rawUpload.getDistributor().getId());
        return saved.getId();
    }

    @Transactional
    public void process(UUID importRunId) {
        ImportRun importRun =
                importRunRepository
                        .findWithDistributorById(importRunId)
                        .orElseThrow(
                                () ->
                                        new SaudaNotFoundException(
                                                "Import run not found: " + importRunId));

        try {
            ImportRunStatusTransitions.assertTransition(importRun.getStatus(), ImportStatus.processing);
            importRun.setStatus(ImportStatus.processing);
            importRun.setStartedAt(Instant.now());
            importRunRepository.save(importRun);

            RawUpload rawUpload = importRun.getRawUpload();
            if (rawUpload == null) {
                throw new SaudaException("Import run has no raw upload attached");
            }

            ImportSource source = buildImportSource(rawUpload);
            ImportAdapter adapter = importAdapterRegistry.resolve(source);
            importRun.setAdapterKey(adapter.key());
            importRunRepository.save(importRun);

            AdapterParseResult parseResult = adapter.parse(source);
            if (isFileLevelError(parseResult)) {
                failRun(importRun, fileLevelReason(parseResult), null);
                return;
            }

            List<ParsedRow> parsedRows = toParsedRows(importRun, parseResult);
            saveParsedRowsInBatches(parsedRows);

            importRun.setTotalRows(parseResult.totalRows());
            importRun.setParsedRowsCount(parseResult.totalRows());
            importRun.setErrorRowsCount(parseResult.errorRows());

            ImportStatus parsedStatus = resolveParsedStatus(parsedRows);
            ImportRunStatusTransitions.assertTransition(ImportStatus.processing, parsedStatus);
            importRun.setStatus(parsedStatus);

            ImportRunStatusTransitions.assertTransition(parsedStatus, ImportStatus.awaiting_approval);
            importRun.setStatus(ImportStatus.awaiting_approval);
            importRun.setFinishedAt(Instant.now());
            importRunRepository.save(importRun);

            rawUpload.setStatus(RawUploadStatus.processed);
            rawUploadRepository.save(rawUpload);

            log.info(
                    "Import run processed: runId={}, adapterKey={}, totalRows={}, errorRows={}, status={}",
                    importRun.getId(),
                    importRun.getAdapterKey(),
                    importRun.getTotalRows(),
                    importRun.getErrorRowsCount(),
                    importRun.getStatus());
        } catch (SaudaException exception) {
            failRun(importRun, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            failRun(importRun, "Import processing failed", exception);
        }
    }

    @SuppressWarnings("unused")
    private void reprocess(UUID importRunId) {
        // Reserved for future re-run support (out of scope for SAUDA-008 step 04).
    }

    private ImportSource buildImportSource(RawUpload rawUpload) {
        return new ImportSource(
                rawUpload.getOriginalFilename(),
                rawUpload.getMimeType(),
                () -> openStoredContent(rawUpload.getStoragePath()));
    }

    private java.io.InputStream openStoredContent(String storagePath) {
        StoredObject storedObject = storedFileUploadService.fetch(storagePath);
        return storedObject.content();
    }

    private List<ParsedRow> toParsedRows(ImportRun importRun, AdapterParseResult parseResult) {
        List<ParsedRow> parsedRows = new ArrayList<>(parseResult.totalRows());
        for (AdapterParsedRow row : parseResult.rows()) {
            ParsedRowStatus status = parsedRowValidator.resolveStatus(row);
            parsedRows.add(ParsedRowEntityMapper.toEntity(importRun, row, status));
        }
        return parsedRows;
    }

    private void saveParsedRowsInBatches(List<ParsedRow> parsedRows) {
        int batchSize = importProperties.saveBatchSize();
        for (int index = 0; index < parsedRows.size(); index += batchSize) {
            int end = Math.min(index + batchSize, parsedRows.size());
            parsedRowRepository.saveAll(parsedRows.subList(index, end));
        }
    }

    private static ImportStatus resolveParsedStatus(List<ParsedRow> parsedRows) {
        boolean hasIssues =
                parsedRows.stream()
                        .anyMatch(
                                row ->
                                        row.getStatus() == ParsedRowStatus.error
                                                || row.getStatus() == ParsedRowStatus.needs_review);
        return hasIssues ? ImportStatus.parsed_with_errors : ImportStatus.parsed;
    }

    private static boolean isFileLevelError(AdapterParseResult parseResult) {
        if (parseResult.rows().size() != 1) {
            return false;
        }
        AdapterParsedRow row = parseResult.rows().getFirst();
        return row.fields() == null
                && row.errors().stream().anyMatch(error -> error.field() == null);
    }

    private static String fileLevelReason(AdapterParseResult parseResult) {
        return parseResult.rows().getFirst().errors().stream()
                .filter(error -> error.field() == null)
                .map(RowError::message)
                .findFirst()
                .orElse("Import file could not be processed");
    }

    private void failRun(ImportRun importRun, String reason, Exception exception) {
        if (ImportRunStatusTransitions.isAllowed(importRun.getStatus(), ImportStatus.failed)) {
            ImportRunStatusTransitions.assertTransition(importRun.getStatus(), ImportStatus.failed);
            importRun.setStatus(ImportStatus.failed);
        } else if (importRun.getStatus() != ImportStatus.failed) {
            importRun.setStatus(ImportStatus.failed);
        }
        importRun.setFinishedAt(Instant.now());
        importRunRepository.save(importRun);

        ImportError importError = new ImportError();
        importError.setImportRun(importRun);
        importError.setReason(reason);
        importErrorRepository.save(importError);

        if (exception == null) {
            log.error("Import run failed: runId={}, reason={}", importRun.getId(), reason);
        } else {
            log.error("Import run failed: runId={}, reason={}", importRun.getId(), reason, exception);
        }
    }
}
