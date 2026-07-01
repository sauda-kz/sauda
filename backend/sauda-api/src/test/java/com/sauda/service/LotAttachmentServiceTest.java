package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotAttachment;
import com.sauda.domain.enums.LotStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.RoleCode;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.integration.storage.StoredObject;
import com.sauda.integration.storage.upload.FileUploadPolicies;
import com.sauda.integration.storage.upload.PreparedUpload;
import com.sauda.integration.storage.upload.StoredFileUploadService;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.LotAttachmentRepository;
import com.sauda.repository.LotRepository;
import com.sauda.service.mapper.LotAttachmentMapper;
import com.sauda.testsupport.SecurityTestFixtures;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class LotAttachmentServiceTest {

    @Mock private LotRepository lotRepository;
    @Mock private LotAttachmentRepository lotAttachmentRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private StoredFileUploadService storedFileUploadService;

    private final LotAttachmentMapper lotAttachmentMapper =
            Mappers.getMapper(LotAttachmentMapper.class);

    private LotAttachmentService lotAttachmentService;

    private UUID lotId;
    private UUID userId;
    private UUID attachmentId;
    private Lot lot;
    private AppUser uploader;

    @BeforeEach
    void setUp() {
        lotAttachmentService =
                new LotAttachmentService(
                        lotRepository,
                        lotAttachmentRepository,
                        appUserRepository,
                        storedFileUploadService,
                        lotAttachmentMapper);

        lotId = UUID.randomUUID();
        userId = UUID.randomUUID();
        attachmentId = UUID.randomUUID();

        lot = new Lot();
        lot.setId(lotId);
        lot.setTitle("SSD lot");
        lot.setStatus(LotStatus.active);

        uploader = new AppUser();
        uploader.setId(userId);

        SecurityTestFixtures.setPrincipal(
                userId,
                "admin@sauda.kz",
                UUID.randomUUID(),
                OrganizationType.platform,
                Set.of(RoleCode.platform_admin.name()),
                "lot:manage");
    }

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void uploadStoresFileAndCreatesRecord() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "spec.pdf",
                        "application/pdf",
                        "pdf-content".getBytes(StandardCharsets.UTF_8));
        PreparedUpload prepared =
                new PreparedUpload(
                        "spec.pdf",
                        "application/pdf",
                        "lots/" + lotId + "/20260701T120000Z_spec.pdf",
                        "pdf-content".getBytes(StandardCharsets.UTF_8),
                        "checksum");

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(storedFileUploadService.prepare(
                        file, FileUploadPolicies.LOT_ATTACHMENT, "lots/" + lotId))
                .thenReturn(prepared);
        when(appUserRepository.getReferenceById(userId)).thenReturn(uploader);
        when(lotAttachmentRepository.save(any(LotAttachment.class)))
                .thenAnswer(
                        invocation -> {
                            LotAttachment attachment = invocation.getArgument(0);
                            attachment.setId(attachmentId);
                            return attachment;
                        });

        var response = lotAttachmentService.upload(lotId, file);

        assertThat(response.id()).isEqualTo(attachmentId);
        assertThat(response.originalFilename()).isEqualTo("spec.pdf");
        verify(storedFileUploadService).store(prepared);
    }

    @Test
    void uploadRejectsUnsupportedExtension() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "virus.exe",
                        "application/octet-stream",
                        "bad".getBytes(StandardCharsets.UTF_8));

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(storedFileUploadService.prepare(
                        file, FileUploadPolicies.LOT_ATTACHMENT, "lots/" + lotId))
                .thenThrow(new SaudaException("Unsupported file type"));

        assertThatThrownBy(() -> lotAttachmentService.upload(lotId, file))
                .isInstanceOf(SaudaException.class);
    }

    @Test
    void listByLotReturnsMetadataWithoutStoragePath() {
        LotAttachment attachment = buildAttachment();
        when(lotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(lotAttachmentRepository.findByLotIdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(attachment));

        var responses = lotAttachmentService.listByLot(lotId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().originalFilename()).isEqualTo("spec.pdf");
    }

    @Test
    void downloadReturnsStoredObject() {
        LotAttachment attachment = buildAttachment();
        StoredObject storedObject =
                new StoredObject(new ByteArrayInputStream(new byte[] {1, 2}), 2, "application/pdf");

        when(lotAttachmentRepository.findByIdAndLotId(attachmentId, lotId))
                .thenReturn(Optional.of(attachment));
        when(storedFileUploadService.fetch(attachment.getStoragePath())).thenReturn(storedObject);

        var download = lotAttachmentService.download(lotId, attachmentId);

        assertThat(download.filename()).isEqualTo("spec.pdf");
        assertThat(download.storedObject()).isEqualTo(storedObject);
    }

    @Test
    void deleteRemovesStorageObjectAndRecord() {
        LotAttachment attachment = buildAttachment();
        when(lotAttachmentRepository.findByIdAndLotId(attachmentId, lotId))
                .thenReturn(Optional.of(attachment));

        lotAttachmentService.delete(lotId, attachmentId);

        verify(storedFileUploadService).delete(attachment.getStoragePath());
        verify(lotAttachmentRepository).delete(attachment);
    }

    @Test
    void uploadThrowsWhenLotMissing() {
        when(lotRepository.findById(lotId)).thenReturn(Optional.empty());

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "spec.pdf",
                        "application/pdf",
                        "pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> lotAttachmentService.upload(lotId, file))
                .isInstanceOf(SaudaNotFoundException.class);
    }

    private LotAttachment buildAttachment() {
        LotAttachment attachment = new LotAttachment();
        attachment.setId(attachmentId);
        attachment.setLot(lot);
        attachment.setUploadedBy(uploader);
        attachment.setOriginalFilename("spec.pdf");
        attachment.setStoragePath("lots/" + lotId + "/20260701T120000Z_spec.pdf");
        attachment.setFileSize(11);
        attachment.setMimeType("application/pdf");
        attachment.setChecksum("abc123");
        return attachment;
    }
}
