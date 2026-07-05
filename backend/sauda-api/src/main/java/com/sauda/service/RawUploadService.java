package com.sauda.service;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.RawUpload;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.RawUploadStatus;
import com.sauda.domain.enums.RoleCode;
import com.sauda.dto.rawupload.RawUploadDownload;
import com.sauda.dto.rawupload.RawUploadResponse;
import com.sauda.exception.SaudaException;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.integration.storage.upload.FileUploadPolicies;
import com.sauda.integration.storage.upload.PreparedUpload;
import com.sauda.integration.storage.upload.StoredFileUploadService;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.repository.RawUploadRepository;
import com.sauda.security.principal.SaudaPrincipal;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.RawUploadMapper;
import com.sauda.service.imports.event.RawUploadStoredEvent;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class RawUploadService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final RawUploadRepository rawUploadRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final StoredFileUploadService storedFileUploadService;
    private final RawUploadMapper rawUploadMapper;
    private final TenantAccessService tenantAccessService;
    private final ApplicationEventPublisher eventPublisher;

    public RawUploadService(
            RawUploadRepository rawUploadRepository,
            OrganizationRepository organizationRepository,
            AppUserRepository appUserRepository,
            StoredFileUploadService storedFileUploadService,
            RawUploadMapper rawUploadMapper,
            TenantAccessService tenantAccessService,
            ApplicationEventPublisher eventPublisher) {
        this.rawUploadRepository = rawUploadRepository;
        this.organizationRepository = organizationRepository;
        this.appUserRepository = appUserRepository;
        this.storedFileUploadService = storedFileUploadService;
        this.rawUploadMapper = rawUploadMapper;
        this.tenantAccessService = tenantAccessService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(noRollbackFor = SaudaException.class)
    public RawUploadResponse upload(UUID distributorId, MultipartFile file) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);

        SaudaPrincipal principal = SecurityUtils.requirePrincipal();
        Organization distributor = organizationRepository.getReferenceById(resolvedDistributorId);
        AppUser uploader = appUserRepository.getReferenceById(principal.id());

        RawUpload upload = new RawUpload();
        upload.setDistributor(distributor);
        upload.setUploadedBy(uploader);
        upload.setUploadedByRole(resolveUploaderRole(principal));
        upload.setStatus(RawUploadStatus.processing);

        try {
            PreparedUpload prepared =
                    storedFileUploadService.prepare(
                            file,
                            FileUploadPolicies.RAW_DISTRIBUTOR_PRICE,
                            "raw/" + resolvedDistributorId);
            upload.setOriginalFilename(prepared.originalFilename());
            upload.setStoragePath(prepared.storagePath());
            upload.setMimeType(prepared.mimeType());
            upload.setFileSize(prepared.contentLength());
            upload.setChecksum(prepared.checksumSha256());

            storedFileUploadService.store(prepared);

            upload.setStatus(RawUploadStatus.uploaded);
            RawUpload saved = rawUploadRepository.save(upload);
            eventPublisher.publishEvent(new RawUploadStoredEvent(saved.getId()));
            log.info(
                    "Raw upload completed: uploadId={}, distributorId={}, path={}, size={}",
                    saved.getId(),
                    resolvedDistributorId,
                    prepared.storagePath(),
                    prepared.contentLength());
            return rawUploadMapper.toResponse(saved);
        } catch (SaudaException exception) {
            persistFailedUpload(upload, exception.getMessage(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            persistFailedUpload(upload, "Failed to store uploaded file", exception);
            throw new SaudaException("Failed to store uploaded file");
        }
    }

    @Transactional(readOnly = true)
    public Page<RawUploadResponse> listForDistributor(UUID distributorId, Pageable pageable) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        return rawUploadRepository
                .findByDistributorIdOrderByCreatedAtDesc(resolvedDistributorId, pageable)
                .map(rawUploadMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RawUploadResponse getUpload(UUID distributorId, UUID uploadId) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        return rawUploadMapper.toResponse(findUploadOrThrow(resolvedDistributorId, uploadId));
    }

    @Transactional(readOnly = true)
    public RawUploadDownload download(UUID distributorId, UUID uploadId) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        RawUpload upload = findUploadOrThrow(resolvedDistributorId, uploadId);
        assertDownloadable(upload);
        return new RawUploadDownload(
                upload.getOriginalFilename(),
                upload.getMimeType(),
                storedFileUploadService.fetch(upload.getStoragePath()));
    }

    private RawUpload findUploadOrThrow(UUID distributorId, UUID uploadId) {
        return rawUploadRepository
                .findByIdAndDistributorId(uploadId, distributorId)
                .orElseThrow(() -> new SaudaNotFoundException("Raw upload not found: " + uploadId));
    }

    private void assertDistributorOrg(UUID distributorId) {
        if (!organizationRepository.existsByIdAndType(
                distributorId, OrganizationType.distributor)) {
            throw new SaudaNotFoundException("Distributor not found: " + distributorId);
        }
    }

    private void assertDownloadable(RawUpload upload) {
        if (upload.getStatus() != RawUploadStatus.uploaded
                && upload.getStatus() != RawUploadStatus.processed) {
            throw new SaudaException("File is not available for download");
        }
    }

    private String resolveUploaderRole(SaudaPrincipal principal) {
        if (principal.roleCodes().contains(RoleCode.platform_admin.name())) {
            return RoleCode.platform_admin.name();
        }
        if (principal.roleCodes().contains(RoleCode.distributor_manager.name())) {
            return RoleCode.distributor_manager.name();
        }
        return principal.roleCodes().stream()
                .findFirst()
                .orElse(RoleCode.distributor_manager.name());
    }

    private void persistFailedUpload(RawUpload upload, String message, Exception exception) {
        upload.setStatus(RawUploadStatus.failed);
        upload.setErrorMessage(truncateError(message));
        if (upload.getChecksum() == null) {
            upload.setChecksum(storedFileUploadService.emptyChecksum());
        }
        if (upload.getOriginalFilename() == null) {
            upload.setOriginalFilename("unknown");
        }
        if (upload.getStoragePath() == null) {
            upload.setStoragePath("failed/" + UUID.randomUUID());
        }
        if (upload.getMimeType() == null) {
            upload.setMimeType("application/octet-stream");
        }
        rawUploadRepository.save(upload);
        log.error(
                "Raw upload failed: distributorId={}, filename={}, path={}",
                upload.getDistributor().getId(),
                upload.getOriginalFilename(),
                upload.getStoragePath(),
                exception);
    }

    private static String truncateError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
