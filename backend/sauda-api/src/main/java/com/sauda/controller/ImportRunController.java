package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.dto.imports.ImportRunResponse;
import com.sauda.dto.imports.ParsedRowResponse;
import com.sauda.dto.imports.RejectImportRequest;
import com.sauda.dto.imports.UpdateParsedRowRequest;
import com.sauda.service.imports.ImportRunQueryService;
import com.sauda.service.imports.ImportRunService;
import com.sauda.service.imports.ParsedRowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Import Runs", description = "Import run results and parsed rows")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(ApiConstants.API_V1)
public class ImportRunController {

    private final ImportRunQueryService importRunQueryService;
    private final ParsedRowService parsedRowService;
    private final ImportRunService importRunService;

    public ImportRunController(
            ImportRunQueryService importRunQueryService,
            ParsedRowService parsedRowService,
            ImportRunService importRunService) {
        this.importRunQueryService = importRunQueryService;
        this.parsedRowService = parsedRowService;
        this.importRunService = importRunService;
    }

    @Operation(summary = "List import runs for distributor")
    @GetMapping("/distributors/{distributorId}/import-runs")
    @PreAuthorize("hasAuthority('import:read')")
    public Page<ImportRunResponse> listRuns(
            @PathVariable UUID distributorId, @PageableDefault(size = 20) Pageable pageable) {
        return importRunQueryService.listRuns(distributorId, pageable);
    }

    @Operation(summary = "Get import run details")
    @GetMapping("/distributors/{distributorId}/import-runs/{runId}")
    @PreAuthorize("hasAuthority('import:read')")
    public ImportRunResponse getRun(@PathVariable UUID distributorId, @PathVariable UUID runId) {
        return importRunQueryService.getRun(distributorId, runId);
    }

    @Operation(summary = "List parsed rows for import run")
    @GetMapping("/distributors/{distributorId}/import-runs/{runId}/rows")
    @PreAuthorize("hasAuthority('import:read')")
    public Page<ParsedRowResponse> listRows(
            @PathVariable UUID distributorId,
            @PathVariable UUID runId,
            @RequestParam(required = false) ParsedRowStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return importRunQueryService.listRows(distributorId, runId, status, pageable);
    }

    @Operation(summary = "Edit parsed row before import approval")
    @PatchMapping("/distributors/{distributorId}/import-runs/{runId}/rows/{rowId}")
    @PreAuthorize("hasAuthority('import:approve')")
    public ParsedRowResponse editRow(
            @PathVariable UUID distributorId,
            @PathVariable UUID runId,
            @PathVariable UUID rowId,
            @Valid @RequestBody UpdateParsedRowRequest request) {
        return parsedRowService.editRow(distributorId, runId, rowId, request);
    }

    @Operation(summary = "Approve import run")
    @PostMapping("/distributors/{distributorId}/import-runs/{runId}/approve")
    @PreAuthorize("hasAuthority('import:approve')")
    public ImportRunResponse approveRun(
            @PathVariable UUID distributorId, @PathVariable UUID runId) {
        return importRunService.approveRun(distributorId, runId);
    }

    @Operation(summary = "Reject import run")
    @PostMapping("/distributors/{distributorId}/import-runs/{runId}/reject")
    @PreAuthorize("hasAuthority('import:approve')")
    public ImportRunResponse rejectRun(
            @PathVariable UUID distributorId,
            @PathVariable UUID runId,
            @Valid @RequestBody(required = false) RejectImportRequest request) {
        String reason = request == null ? null : request.reason();
        return importRunService.rejectRun(distributorId, runId, reason);
    }
}
