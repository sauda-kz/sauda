package com.sauda.service.imports;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.dto.imports.ParsedRowResponse;
import com.sauda.dto.imports.UpdateParsedRowRequest;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.repository.ParsedRowRepository;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.TenantAccessService;
import com.sauda.service.imports.adapter.ImportRowMapper;
import com.sauda.service.imports.model.AdapterParsedRow;
import com.sauda.service.mapper.ParsedRowMapper;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ParsedRowService {

    private static final Set<ImportStatus> EDITABLE_RUN_STATUSES =
            EnumSet.of(
                    ImportStatus.parsed,
                    ImportStatus.parsed_with_errors,
                    ImportStatus.awaiting_approval);

    private final ParsedRowRepository parsedRowRepository;
    private final ImportRunRepository importRunRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final TenantAccessService tenantAccessService;
    private final ImportRowMapper importRowMapper;
    private final ParsedRowValidator parsedRowValidator;
    private final ParsedRowMapper parsedRowMapper;

    public ParsedRowService(
            ParsedRowRepository parsedRowRepository,
            ImportRunRepository importRunRepository,
            OrganizationRepository organizationRepository,
            AppUserRepository appUserRepository,
            TenantAccessService tenantAccessService,
            ImportRowMapper importRowMapper,
            ParsedRowValidator parsedRowValidator,
            ParsedRowMapper parsedRowMapper) {
        this.parsedRowRepository = parsedRowRepository;
        this.importRunRepository = importRunRepository;
        this.organizationRepository = organizationRepository;
        this.appUserRepository = appUserRepository;
        this.tenantAccessService = tenantAccessService;
        this.importRowMapper = importRowMapper;
        this.parsedRowValidator = parsedRowValidator;
        this.parsedRowMapper = parsedRowMapper;
    }

    @Transactional
    public ParsedRowResponse editRow(
            UUID distributorId, UUID runId, UUID rowId, UpdateParsedRowRequest request) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);

        ImportRun importRun = findRunOrThrow(resolvedDistributorId, runId);
        assertRunEditable(importRun);

        ParsedRow parsedRow =
                parsedRowRepository
                        .findByIdAndImportRunId(rowId, runId)
                        .orElseThrow(
                                () -> new SaudaNotFoundException("Parsed row not found: " + rowId));

        ImportRowMapper.ImportRowMappingResult mapping = importRowMapper.map(toRawCells(request));
        AdapterParsedRow adapterRow =
                new AdapterParsedRow(
                        parsedRow.getSourceRowNumber() == null ? 0 : parsedRow.getSourceRowNumber(),
                        parsedRow.getRawRowData(),
                        mapping.fields(),
                        mapping.errors(),
                        mapping.warnings());
        ParsedRowStatus status = parsedRowValidator.resolveEditedStatus(adapterRow);

        ParsedRowEntityMapper.applyValidationResult(
                parsedRow, mapping.fields(), mapping.errors(), mapping.warnings(), status);

        UUID editorId = SecurityUtils.requirePrincipal().id();
        AppUser editor = appUserRepository.getReferenceById(editorId);
        parsedRow.setEditedBy(editor);
        parsedRow.setEditedAt(Instant.now());

        ParsedRow saved = parsedRowRepository.save(parsedRow);
        recomputeRunCounters(importRun);
        importRunRepository.save(importRun);

        log.info(
                "Parsed row edited: rowId={}, runId={}, userId={}, status={}",
                rowId,
                runId,
                editorId,
                status);
        return parsedRowMapper.toResponse(saved);
    }

    private void recomputeRunCounters(ImportRun importRun) {
        UUID runId = importRun.getId();
        int errorRows =
                (int) parsedRowRepository.countByImportRunIdAndStatus(runId, ParsedRowStatus.error);
        importRun.setErrorRowsCount(errorRows);

        if (importRun.getStatus() == ImportStatus.parsed
                || importRun.getStatus() == ImportStatus.parsed_with_errors) {
            long needsReviewRows =
                    parsedRowRepository.countByImportRunIdAndStatus(
                            runId, ParsedRowStatus.needs_review);
            ImportStatus runStatus =
                    errorRows > 0 || needsReviewRows > 0
                            ? ImportStatus.parsed_with_errors
                            : ImportStatus.parsed;
            importRun.setStatus(runStatus);
        }
    }

    private static void assertRunEditable(ImportRun importRun) {
        if (!EDITABLE_RUN_STATUSES.contains(importRun.getStatus())) {
            throw new SaudaException("Import can no longer be edited");
        }
    }

    private ImportRun findRunOrThrow(UUID distributorId, UUID runId) {
        return importRunRepository
                .findByIdAndDistributorId(runId, distributorId)
                .orElseThrow(() -> new SaudaNotFoundException("Import run not found: " + runId));
    }

    private void assertDistributorOrg(UUID distributorId) {
        if (!organizationRepository.existsByIdAndType(
                distributorId, OrganizationType.distributor)) {
            throw new SaudaNotFoundException("Distributor not found: " + distributorId);
        }
    }

    private static Map<String, String> toRawCells(UpdateParsedRowRequest request) {
        Map<String, String> cells = new LinkedHashMap<>();
        cells.put("sku", request.sku());
        cells.put("name", request.name());
        putIfNotNull(cells, "brand", request.brand());
        putIfNotNull(cells, "mpn", request.mpn());
        if (request.price() != null) {
            cells.put("price", request.price().toPlainString());
        }
        if (request.priceIncludesVat() != null) {
            cells.put("price_includes_vat", request.priceIncludesVat().toString());
        }
        if (request.stockQuantity() != null) {
            cells.put("stock_quantity", request.stockQuantity().toString());
        }
        if (request.stockStatus() != null) {
            cells.put("stock_status", request.stockStatus().name());
        }
        if (request.leadTimeDays() != null) {
            cells.put("lead_time_days", request.leadTimeDays().toString());
        }
        return cells;
    }

    private static void putIfNotNull(Map<String, String> target, String key, String value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
