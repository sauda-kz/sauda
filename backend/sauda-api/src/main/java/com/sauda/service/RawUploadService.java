package com.sauda.service;

import com.sauda.config.StorageProperties;
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
import com.sauda.integration.storage.ObjectStorageProvider;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.OrganizationRepository;
import com.sauda.repository.RawUploadRepository;
import com.sauda.security.principal.SaudaPrincipal;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.RawUploadMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class RawUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xlsx", "xls", "csv");
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final Pattern UNSAFE_FILENAME_CHARS =
            Pattern.compile("[/\\\\\0\u0000-\u001F\u007F\"';:]");
    private static final DateTimeFormatter STORAGE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final RawUploadRepository rawUploadRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectStorageProvider objectStorageProvider;
    private final RawUploadMapper rawUploadMapper;
    private final TenantAccessService tenantAccessService;
    private final long maxFileSizeBytes;

    public RawUploadService(
            RawUploadRepository rawUploadRepository,
            OrganizationRepository organizationRepository,
            AppUserRepository appUserRepository,
            ObjectStorageProvider objectStorageProvider,
            RawUploadMapper rawUploadMapper,
            TenantAccessService tenantAccessService,
            StorageProperties storageProperties) {
        this.rawUploadRepository = rawUploadRepository;
        this.organizationRepository = organizationRepository;
        this.appUserRepository = appUserRepository;
        this.objectStorageProvider = objectStorageProvider;
        this.rawUploadMapper = rawUploadMapper;
        this.tenantAccessService = tenantAccessService;
        this.maxFileSizeBytes = storageProperties.maxFileSizeBytes();
    }

    @Transactional(noRollbackFor = SaudaException.class)
    public RawUploadResponse upload(UUID distributorId, MultipartFile file) {
        UUID resolvedDistributorId = tenantAccessService.resolveDistributorId(distributorId);
        assertDistributorOrg(resolvedDistributorId);
        validateFile(file);

        SaudaPrincipal principal = SecurityUtils.requirePrincipal();
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storagePath = buildStoragePath(resolvedDistributorId, originalFilename);
        String mimeType = resolveMimeType(originalFilename);

        Organization distributor = organizationRepository.getReferenceById(resolvedDistributorId);
        AppUser uploader = appUserRepository.getReferenceById(principal.id());

        RawUpload upload = new RawUpload();
        upload.setDistributor(distributor);
        upload.setUploadedBy(uploader);
        upload.setUploadedByRole(resolveUploaderRole(principal));
        upload.setOriginalFilename(originalFilename);
        upload.setStoragePath(storagePath);
        upload.setMimeType(mimeType);
        upload.setFileSize(file.getSize());
        upload.setStatus(RawUploadStatus.processing);

        try {
            byte[] content = file.getBytes();
            upload.setFileSize(content.length);
            upload.setChecksum(computeSha256Hex(content));

            objectStorageProvider.putObject(
                    storagePath, new ByteArrayInputStream(content), content.length, mimeType);

            upload.setStatus(RawUploadStatus.uploaded);
            RawUpload saved = rawUploadRepository.save(upload);
            log.info(
                    "Raw upload completed: uploadId={}, distributorId={}, path={}, size={}",
                    saved.getId(),
                    resolvedDistributorId,
                    storagePath,
                    content.length);
            return rawUploadMapper.toResponse(saved);
        } catch (IOException exception) {
            persistFailedUpload(upload, "Failed to read uploaded file", exception);
            throw new SaudaException("Failed to read uploaded file");
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
                objectStorageProvider.getObject(upload.getStoragePath()));
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

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SaudaException("File is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new SaudaException(
                    "File exceeds maximum allowed size of " + maxFileSizeBytes + " bytes");
        }
        String extension = extractExtensionFromOriginalName(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new SaudaException(
                    "Unsupported file type. Allowed extensions: .xlsx, .xls, .csv");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new SaudaException("Filename is required");
        }
        String basename = Path.of(originalFilename).getFileName().toString().strip();
        if (basename.isBlank() || basename.contains("..")) {
            throw new SaudaException("Invalid filename");
        }
        String sanitized = UNSAFE_FILENAME_CHARS.matcher(basename).replaceAll("_").strip();
        if (sanitized.isBlank() || sanitized.startsWith(".")) {
            throw new SaudaException("Invalid filename");
        }
        return truncateFilename(sanitized, MAX_FILENAME_LENGTH);
    }

    private static String truncateFilename(String filename, int maxLength) {
        if (filename.length() <= maxLength) {
            return filename;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return filename.substring(0, maxLength);
        }
        String extension = filename.substring(dotIndex);
        int baseMaxLength = maxLength - extension.length();
        if (baseMaxLength <= 0) {
            return filename.substring(0, maxLength);
        }
        return filename.substring(0, baseMaxLength) + extension;
    }

    private String buildStoragePath(UUID distributorId, String sanitizedFilename) {
        String timestamp = STORAGE_TIMESTAMP_FORMATTER.format(Instant.now());
        return "raw/" + distributorId + "/" + timestamp + "_" + sanitizedFilename;
    }

    private String extractExtensionFromOriginalName(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new SaudaException(
                    "Unsupported file type. Allowed extensions: .xlsx, .xls, .csv");
        }
        String basename = Path.of(filename).getFileName().toString();
        int dotIndex = basename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == basename.length() - 1) {
            throw new SaudaException(
                    "Unsupported file type. Allowed extensions: .xlsx, .xls, .csv");
        }
        return basename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveMimeType(String filename) {
        String extension = extractExtensionFromOriginalName(filename);
        return switch (extension) {
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "csv" -> "text/csv";
            default ->
                    throw new SaudaException(
                            "Unsupported file type. Allowed extensions: .xlsx, .xls, .csv");
        };
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

    private String computeSha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private void persistFailedUpload(RawUpload upload, String message, Exception exception) {
        upload.setStatus(RawUploadStatus.failed);
        upload.setErrorMessage(truncateError(message));
        if (upload.getChecksum() == null) {
            upload.setChecksum(computeSha256Hex(new byte[0]));
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
