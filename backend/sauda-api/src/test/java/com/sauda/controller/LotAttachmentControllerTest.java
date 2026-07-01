package com.sauda.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.dto.lot.LotAttachmentDownload;
import com.sauda.dto.lot.LotAttachmentResponse;
import com.sauda.integration.storage.StoredObject;
import com.sauda.repository.AppUserRepository;
import com.sauda.service.LotAttachmentService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LotAttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
class LotAttachmentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LotAttachmentService lotAttachmentService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "lot:manage")
    void uploadReturnsCreated() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        MockMultipartFile file =
                new MockMultipartFile("file", "spec.pdf", "application/pdf", "pdf".getBytes());

        when(lotAttachmentService.upload(eq(lotId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        new LotAttachmentResponse(
                                attachmentId,
                                lotId,
                                "spec.pdf",
                                3,
                                "application/pdf",
                                UUID.randomUUID(),
                                Instant.now()));

        mockMvc.perform(
                        multipart("/api/v1/lots/{lotId}/attachments", lotId)
                                .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("spec.pdf"));
    }

    @Test
    @WithMockUser(authorities = "lot:read")
    void listReturnsAttachments() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(lotAttachmentService.listByLot(lotId))
                .thenReturn(
                        List.of(
                                new LotAttachmentResponse(
                                        UUID.randomUUID(),
                                        lotId,
                                        "spec.pdf",
                                        100,
                                        "application/pdf",
                                        UUID.randomUUID(),
                                        Instant.now())));

        mockMvc.perform(get("/api/v1/lots/{lotId}/attachments", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalFilename").value("spec.pdf"));
    }

    @Test
    @WithMockUser(authorities = "lot:read")
    void downloadReturnsFile() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        StoredObject storedObject =
                new StoredObject(new ByteArrayInputStream("pdf".getBytes()), 3, "application/pdf");

        when(lotAttachmentService.download(lotId, attachmentId))
                .thenReturn(new LotAttachmentDownload("spec.pdf", "application/pdf", storedObject));

        mockMvc.perform(
                        get(
                                "/api/v1/lots/{lotId}/attachments/{attachmentId}/download",
                                lotId,
                                attachmentId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spec.pdf\""));
    }

    @Test
    @WithMockUser(authorities = "lot:manage")
    void deleteReturnsNoContent() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        doNothing().when(lotAttachmentService).delete(lotId, attachmentId);

        mockMvc.perform(
                        delete(
                                "/api/v1/lots/{lotId}/attachments/{attachmentId}",
                                lotId,
                                attachmentId))
                .andExpect(status().isNoContent());
    }
}
