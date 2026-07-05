package com.sauda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.dto.imports.ImportRunResponse;
import com.sauda.dto.imports.ParsedRowErrorItem;
import com.sauda.dto.imports.ParsedRowResponse;
import com.sauda.exception.GlobalExceptionHandler;
import com.sauda.exception.SaudaForbiddenException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.AppUserRepository;
import com.sauda.service.imports.ImportRunQueryService;
import com.sauda.service.imports.ImportRunService;
import com.sauda.service.imports.ParsedRowService;
import com.sauda.testsupport.WebMvcSecurityTestConfig;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImportRunController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcSecurityTestConfig.class, GlobalExceptionHandler.class, ImportRunControllerTest.MethodSecurityTestConfig.class})
class ImportRunControllerTest {

    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ImportRunQueryService importRunQueryService;
    @MockitoBean private ParsedRowService parsedRowService;
    @MockitoBean private ImportRunService importRunService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "import:read")
    void listAllRunsReturnsGlobalPageForAdmin() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        when(importRunQueryService.listAllRuns(eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRunResponse(runId, distributorId))));

        mockMvc.perform(get("/api/v1/import-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(runId.toString()))
                .andExpect(jsonPath("$.content[0].distributorName").value("Tech Distributor"));
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void listAllRunsForbiddenForNonAdmin() throws Exception {
        when(importRunQueryService.listAllRuns(any(), any(), any()))
                .thenThrow(new SaudaForbiddenException("Admin access required"));

        mockMvc.perform(get("/api/v1/import-runs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void listRunsReturnsPageForOwner() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        when(importRunQueryService.listRuns(eq(distributorId), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRunResponse(runId, distributorId))));

        mockMvc.perform(get("/api/v1/distributors/{distributorId}/import-runs", distributorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(runId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("awaiting_approval"));
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void getRunReturnsDetails() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        when(importRunQueryService.getRun(distributorId, runId))
                .thenReturn(sampleRunResponse(runId, distributorId));

        mockMvc.perform(
                        get(
                                "/api/v1/distributors/{distributorId}/import-runs/{runId}",
                                distributorId,
                                runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(runId.toString()))
                .andExpect(jsonPath("$.originalFilename").value("prices.csv"));
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void listRowsReturnsFilteredPage() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();

        when(importRunQueryService.listRows(
                        eq(distributorId), eq(runId), eq(ParsedRowStatus.error), any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(
                                        new ParsedRowResponse(
                                                rowId,
                                                runId,
                                                2,
                                                Map.of("sku", "SKU-002"),
                                                Map.of("sku", "SKU-002"),
                                                ParsedRowStatus.error,
                                                List.of(
                                                        new ParsedRowErrorItem(
                                                                "price",
                                                                "INVALID_NUMBER",
                                                                "Invalid price")),
                                                List.of(),
                                                null))));

        mockMvc.perform(
                        get(
                                        "/api/v1/distributors/{distributorId}/import-runs/{runId}/rows",
                                        distributorId,
                                        runId)
                                .param("status", "error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("error"))
                .andExpect(jsonPath("$.content[0].errors[0].code").value("INVALID_NUMBER"));
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void adminCanReadAnyDistributorRuns() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        when(importRunQueryService.getRun(distributorId, runId))
                .thenReturn(sampleRunResponse(runId, distributorId));

        mockMvc.perform(
                        get(
                                "/api/v1/distributors/{distributorId}/import-runs/{runId}",
                                distributorId,
                                runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distributorId").value(distributorId.toString()));
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void getRunReturnsNotFoundForForeignRun() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        when(importRunQueryService.getRun(distributorId, runId))
                .thenThrow(new SaudaNotFoundException("Import run not found: " + runId));

        mockMvc.perform(
                        get(
                                "/api/v1/distributors/{distributorId}/import-runs/{runId}",
                                distributorId,
                                runId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "offer:read")
    void listRunsForbiddenWithoutImportReadPermission() throws Exception {
        UUID distributorId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/distributors/{distributorId}/import-runs", distributorId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "import:approve")
    void approveRunReturnsAppliedStatus() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        when(importRunService.approveRun(distributorId, runId))
                .thenReturn(
                        new ImportRunResponse(
                                runId,
                                UUID.randomUUID(),
                                "prices.csv",
                                distributorId,
                                "Tech Distributor",
                                "csv_v1",
                                ImportStatus.applied,
                                10,
                                10,
                                1,
                                Instant.parse("2026-07-01T10:00:00Z"),
                                Instant.parse("2026-07-01T10:01:00Z"),
                                Instant.parse("2026-07-01T11:00:00Z"),
                                null,
                                Instant.parse("2026-07-01T10:00:00Z"),
                                Instant.parse("2026-07-01T11:00:00Z")));

        mockMvc.perform(
                        post(
                                "/api/v1/distributors/{distributorId}/import-runs/{runId}/approve",
                                distributorId,
                                runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("applied"))
                .andExpect(jsonPath("$.approvedAt").exists());
    }

    @Test
    @WithMockUser(authorities = "import:approve")
    void rejectRunReturnsRejectedStatus() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        when(importRunService.rejectRun(distributorId, runId, "Wrong mapping"))
                .thenReturn(
                        new ImportRunResponse(
                                runId,
                                UUID.randomUUID(),
                                "prices.csv",
                                distributorId,
                                "Tech Distributor",
                                "csv_v1",
                                ImportStatus.rejected,
                                10,
                                10,
                                2,
                                Instant.parse("2026-07-01T10:00:00Z"),
                                Instant.parse("2026-07-01T10:01:00Z"),
                                null,
                                Instant.parse("2026-07-01T11:00:00Z"),
                                Instant.parse("2026-07-01T10:00:00Z"),
                                Instant.parse("2026-07-01T11:00:00Z")));

        mockMvc.perform(
                        post(
                                        "/api/v1/distributors/{distributorId}/import-runs/{runId}/reject",
                                        distributorId,
                                        runId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Wrong mapping\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.rejectedAt").exists());
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void approveRunForbiddenWithoutApprovePermission() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/v1/distributors/{distributorId}/import-runs/{runId}/approve",
                                distributorId,
                                runId))
                .andExpect(status().isForbidden());
    }

    private static ImportRunResponse sampleRunResponse(UUID runId, UUID distributorId) {
        return new ImportRunResponse(
                runId,
                UUID.randomUUID(),
                "prices.csv",
                distributorId,
                "Tech Distributor",
                "csv_v1",
                ImportStatus.awaiting_approval,
                10,
                10,
                2,
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T10:01:00Z"),
                null,
                null,
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T10:01:00Z"));
    }
}
