package com.sauda.service.imports;

import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.dto.imports.ImportRunResponse;
import com.sauda.dto.imports.ParsedRowResponse;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.ImportRunRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.repository.ParsedRowRepository;
import com.sauda.service.TenantAccessService;
import com.sauda.service.mapper.ImportRunMapper;
import com.sauda.service.mapper.ParsedRowMapper;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportRunQueryService {

    private final ImportRunRepository importRunRepository;
    private final ParsedRowRepository parsedRowRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;
    private final ImportRunMapper importRunMapper;
    private final ParsedRowMapper parsedRowMapper;

    public ImportRunQueryService(
            ImportRunRepository importRunRepository,
            ParsedRowRepository parsedRowRepository,
            OrganizationRepository organizationRepository,
            TenantAccessService tenantAccessService,
            ImportRunMapper importRunMapper,
            ParsedRowMapper parsedRowMapper) {
        this.importRunRepository = importRunRepository;
        this.parsedRowRepository = parsedRowRepository;
        this.organizationRepository = organizationRepository;
        this.tenantAccessService = tenantAccessService;
        this.importRunMapper = importRunMapper;
        this.parsedRowMapper = parsedRowMapper;
    }

    @Transactional(readOnly = true)
    public Page<ImportRunResponse> listRuns(UUID distributorId, Pageable pageable) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        return importRunRepository
                .findByDistributorIdOrderByCreatedAtDesc(resolvedDistributorId, pageable)
                .map(importRunMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ImportRunResponse getRun(UUID distributorId, UUID runId) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        return importRunMapper.toResponse(findRunOrThrow(resolvedDistributorId, runId));
    }

    @Transactional(readOnly = true)
    public Page<ParsedRowResponse> listRows(
            UUID distributorId, UUID runId, ParsedRowStatus statusFilter, Pageable pageable) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        ImportRun importRun = findRunOrThrow(resolvedDistributorId, runId);

        Page<ParsedRow> rows =
                statusFilter == null
                        ? parsedRowRepository.findByImportRunId(importRun.getId(), pageable)
                        : parsedRowRepository.findByImportRunIdAndStatus(
                                importRun.getId(), statusFilter, pageable);
        return rows.map(parsedRowMapper::toResponse);
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
}
