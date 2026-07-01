package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.config.StorageProperties;
import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.RawUpload;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.RawUploadStatus;
import com.sauda.domain.enums.RoleCode;
import com.sauda.dto.rawupload.RawUploadDownload;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.integration.storage.ObjectStorageProvider;
import com.sauda.integration.storage.StoredObject;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.repository.RawUploadRepository;
import com.sauda.service.mapper.RawUploadMapper;
import com.sauda.testsupport.SecurityTestFixtures;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class RawUploadServiceTest {

    @Mock private RawUploadRepository rawUploadRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private ObjectStorageProvider objectStorageProvider;
    @Mock private TenantAccessService tenantAccessService;

    private final RawUploadMapper rawUploadMapper = Mappers.getMapper(RawUploadMapper.class);
    private final StorageProperties storageProperties =
            new StorageProperties(
                    "http://localhost:9000", "key", "secret", "bucket", "us-east-1", 1024);

    private RawUploadService rawUploadService;

    private UUID distributorId;
    private UUID userId;
    private Organization distributor;
    private AppUser uploader;

    @BeforeEach
    void setUp() {
        rawUploadService =
                new RawUploadService(
                        rawUploadRepository,
                        organizationRepository,
                        appUserRepository,
                        objectStorageProvider,
                        rawUploadMapper,
                        tenantAccessService,
                        storageProperties);

        distributorId = UUID.randomUUID();
        userId = UUID.randomUUID();

        distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);

        uploader = new AppUser();
        uploader.setId(userId);
        uploader.setOrganization(distributor);

        SecurityTestFixtures.setPrincipal(
                userId,
                "manager@dist.kz",
                distributorId,
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "import:run");
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
                        "prices.csv",
                        "text/csv",
                        "sku,price".getBytes(StandardCharsets.UTF_8));

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(organizationRepository.getReferenceById(distributorId)).thenReturn(distributor);
        when(appUserRepository.getReferenceById(userId)).thenReturn(uploader);
        when(rawUploadRepository.save(any(RawUpload.class)))
                .thenAnswer(
                        invocation -> {
                            RawUpload upload = invocation.getArgument(0);
                            upload.setId(UUID.randomUUID());
                            return upload;
                        });

        var response = rawUploadService.upload(distributorId, file);

        assertThat(response.status()).isEqualTo(RawUploadStatus.uploaded);
        assertThat(response.originalFilename()).isEqualTo("prices.csv");
        assertThat(response.distributorId()).isEqualTo(distributorId);
        assertThat(response.uploadedByUserId()).isEqualTo(userId);
        assertThat(response.uploadedByRole()).isEqualTo(RoleCode.distributor_manager.name());
        assertThat(response.storagePath()).startsWith("raw/" + distributorId + "/");
        assertThat(response.storagePath()).endsWith("_prices.csv");

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorageProvider)
                .putObject(pathCaptor.capture(), any(), eq(9L), eq("text/csv"));
        assertThat(pathCaptor.getValue()).isEqualTo(response.storagePath());
    }

    @Test
    void uploadPreservesUnicodeFilename() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "Прайс остатки.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[] {1, 2, 3});

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(organizationRepository.getReferenceById(distributorId)).thenReturn(distributor);
        when(appUserRepository.getReferenceById(userId)).thenReturn(uploader);
        when(rawUploadRepository.save(any(RawUpload.class)))
                .thenAnswer(
                        invocation -> {
                            RawUpload upload = invocation.getArgument(0);
                            upload.setId(UUID.randomUUID());
                            return upload;
                        });

        var response = rawUploadService.upload(distributorId, file);

        assertThat(response.originalFilename()).isEqualTo("Прайс остатки.xlsx");
        assertThat(response.storagePath()).endsWith("_Прайс остатки.xlsx");
    }

    @Test
    void uploadRejectsUnsupportedExtension() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "prices.pdf", "application/pdf", new byte[] {1, 2, 3});

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);

        assertThatThrownBy(() -> rawUploadService.upload(distributorId, file))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile file =
                new MockMultipartFile("file", "prices.csv", "text/csv", new byte[0]);

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);

        assertThatThrownBy(() -> rawUploadService.upload(distributorId, file))
                .isInstanceOf(SaudaException.class)
                .hasMessage("File is required");
    }

    @Test
    void uploadPersistsFailedRecordWhenStorageFails() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "stock.xlsx", "application/vnd.ms-excel", new byte[] {1});

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(organizationRepository.getReferenceById(distributorId)).thenReturn(distributor);
        when(appUserRepository.getReferenceById(userId)).thenReturn(uploader);
        doThrow(new IllegalStateException("storage down"))
                .when(objectStorageProvider)
                .putObject(any(), any(), any(Long.class), any());
        when(rawUploadRepository.save(any(RawUpload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> rawUploadService.upload(distributorId, file))
                .isInstanceOf(SaudaException.class)
                .hasMessage("Failed to store uploaded file");

        ArgumentCaptor<RawUpload> uploadCaptor = ArgumentCaptor.forClass(RawUpload.class);
        verify(rawUploadRepository).save(uploadCaptor.capture());
        assertThat(uploadCaptor.getValue().getStatus()).isEqualTo(RawUploadStatus.failed);
        assertThat(uploadCaptor.getValue().getErrorMessage())
                .isEqualTo("Failed to store uploaded file");
    }

    @Test
    void listForDistributorUsesResolvedTenant() {
        UUID uploadId = UUID.randomUUID();
        RawUpload upload = buildUpload(uploadId, RawUploadStatus.uploaded);

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(rawUploadRepository.findByDistributorIdOrderByCreatedAtDesc(
                        eq(distributorId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(upload)));

        var page = rawUploadService.listForDistributor(distributorId, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().id()).isEqualTo(uploadId);
    }

    @Test
    void downloadReturnsStoredObjectForUploadedFile() {
        UUID uploadId = UUID.randomUUID();
        RawUpload upload = buildUpload(uploadId, RawUploadStatus.uploaded);
        StoredObject storedObject =
                new StoredObject(new ByteArrayInputStream(new byte[] {1}), 1L, "text/csv");

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(rawUploadRepository.findByIdAndDistributorId(uploadId, distributorId))
                .thenReturn(Optional.of(upload));
        when(objectStorageProvider.getObject(upload.getStoragePath())).thenReturn(storedObject);

        RawUploadDownload download = rawUploadService.download(distributorId, uploadId);

        assertThat(download.filename()).isEqualTo("prices.csv");
        assertThat(download.storedObject()).isSameAs(storedObject);
    }

    @Test
    void downloadRejectsFailedUpload() {
        UUID uploadId = UUID.randomUUID();
        RawUpload upload = buildUpload(uploadId, RawUploadStatus.failed);

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(rawUploadRepository.findByIdAndDistributorId(uploadId, distributorId))
                .thenReturn(Optional.of(upload));

        assertThatThrownBy(() -> rawUploadService.download(distributorId, uploadId))
                .isInstanceOf(SaudaException.class)
                .hasMessage("File is not available for download");
    }

    @Test
    void getUploadThrowsWhenNotFound() {
        UUID uploadId = UUID.randomUUID();

        when(tenantAccessService.resolveDistributorId(distributorId)).thenReturn(distributorId);
        when(organizationRepository.existsByIdAndType(distributorId, OrganizationType.distributor))
                .thenReturn(true);
        when(rawUploadRepository.findByIdAndDistributorId(uploadId, distributorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rawUploadService.getUpload(distributorId, uploadId))
                .isInstanceOf(SaudaNotFoundException.class);
    }

    private RawUpload buildUpload(UUID uploadId, RawUploadStatus status) {
        RawUpload upload = new RawUpload();
        upload.setId(uploadId);
        upload.setDistributor(distributor);
        upload.setUploadedBy(uploader);
        upload.setUploadedByRole(RoleCode.distributor_manager.name());
        upload.setOriginalFilename("prices.csv");
        upload.setStoragePath("raw/" + distributorId + "/20250101T120000Z_prices.csv");
        upload.setFileSize(3L);
        upload.setMimeType("text/csv");
        upload.setChecksum("abc");
        upload.setStatus(status);
        return upload;
    }
}
