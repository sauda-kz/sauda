package com.sauda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.domain.enums.RawUploadStatus;
import com.sauda.domain.enums.RoleCode;
import com.sauda.dto.rawupload.RawUploadDownload;
import com.sauda.dto.rawupload.RawUploadResponse;
import com.sauda.integration.storage.StoredObject;
import com.sauda.repository.AppUserRepository;
import com.sauda.service.RawUploadService;
import com.sauda.testsupport.WebMvcSecurityTestConfig;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RawUploadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
class RawUploadControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RawUploadService rawUploadService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "import:run")
    void uploadReturnsCreated() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        MockMultipartFile file =
                new MockMultipartFile("file", "prices.csv", "text/csv", "sku,price".getBytes());

        when(rawUploadService.upload(eq(distributorId), any()))
                .thenReturn(
                        new RawUploadResponse(
                                uploadId,
                                distributorId,
                                UUID.randomUUID(),
                                RoleCode.distributor_manager.name(),
                                "prices.csv",
                                "raw/" + distributorId + "/20250101T120000Z_prices.csv",
                                9L,
                                "text/csv",
                                "checksum",
                                RawUploadStatus.uploaded,
                                null,
                                Instant.parse("2025-01-01T12:00:00Z"),
                                Instant.parse("2025-01-01T12:00:00Z")));

        mockMvc.perform(
                        multipart("/api/v1/distributors/{distributorId}/raw-uploads", distributorId)
                                .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(uploadId.toString()))
                .andExpect(jsonPath("$.status").value("uploaded"))
                .andExpect(jsonPath("$.originalFilename").value("prices.csv"));
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void listForDistributorReturnsPage() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();

        when(rawUploadService.listForDistributor(eq(distributorId), any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(
                                        new RawUploadResponse(
                                                uploadId,
                                                distributorId,
                                                UUID.randomUUID(),
                                                RoleCode.distributor_manager.name(),
                                                "prices.csv",
                                                "raw/path/prices.csv",
                                                9L,
                                                "text/csv",
                                                "checksum",
                                                RawUploadStatus.uploaded,
                                                null,
                                                Instant.parse("2025-01-01T12:00:00Z"),
                                                Instant.parse("2025-01-01T12:00:00Z")))));

        mockMvc.perform(get("/api/v1/distributors/{distributorId}/raw-uploads", distributorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(uploadId.toString()));
    }

    @Test
    @WithMockUser(authorities = "import:read")
    void downloadReturnsAttachment() throws Exception {
        UUID distributorId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        StoredObject storedObject =
                new StoredObject(new ByteArrayInputStream("data".getBytes()), 4L, "text/csv");

        when(rawUploadService.download(distributorId, uploadId))
                .thenReturn(new RawUploadDownload("prices.csv", "text/csv", storedObject));

        mockMvc.perform(
                        get(
                                "/api/v1/distributors/{distributorId}/raw-uploads/{uploadId}/download",
                                distributorId,
                                uploadId))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"prices.csv\""))
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")));
    }
}
