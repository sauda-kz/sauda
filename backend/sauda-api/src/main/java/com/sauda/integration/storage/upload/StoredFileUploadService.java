package com.sauda.integration.storage.upload;

import com.sauda.config.StorageProperties;
import com.sauda.exception.SaudaException;
import com.sauda.integration.storage.ObjectStorageProvider;
import com.sauda.integration.storage.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class StoredFileUploadService {

    private final ObjectStorageProvider objectStorageProvider;
    private final FileUploadValidator fileUploadValidator;
    private final FilenameSanitizer filenameSanitizer;
    private final StoragePathBuilder storagePathBuilder;
    private final ChecksumCalculator checksumCalculator;
    private final long maxFileSizeBytes;

    public StoredFileUploadService(
            ObjectStorageProvider objectStorageProvider,
            FileUploadValidator fileUploadValidator,
            FilenameSanitizer filenameSanitizer,
            StoragePathBuilder storagePathBuilder,
            ChecksumCalculator checksumCalculator,
            StorageProperties storageProperties) {
        this.objectStorageProvider = objectStorageProvider;
        this.fileUploadValidator = fileUploadValidator;
        this.filenameSanitizer = filenameSanitizer;
        this.storagePathBuilder = storagePathBuilder;
        this.checksumCalculator = checksumCalculator;
        this.maxFileSizeBytes = storageProperties.maxFileSizeBytes();
    }

    public PreparedUpload prepare(MultipartFile file, FileUploadPolicy policy, String pathPrefix) {
        fileUploadValidator.validatePresent(file, policy, maxFileSizeBytes);

        String originalFilename = filenameSanitizer.sanitize(file.getOriginalFilename());
        String extension = filenameSanitizer.extractExtension(originalFilename, policy);
        String mimeType = resolveMimeType(extension, policy);
        String storagePath = storagePathBuilder.buildTimestampedPath(pathPrefix, originalFilename);

        try {
            byte[] content = file.getBytes();
            fileUploadValidator.validateContentSize(content, maxFileSizeBytes);
            return new PreparedUpload(
                    originalFilename,
                    mimeType,
                    storagePath,
                    content,
                    checksumCalculator.sha256Hex(content));
        } catch (IOException exception) {
            throw new SaudaException("Failed to read uploaded file");
        }
    }

    public void store(PreparedUpload preparedUpload) {
        try {
            objectStorageProvider.putObject(
                    preparedUpload.storagePath(),
                    new ByteArrayInputStream(preparedUpload.content()),
                    preparedUpload.contentLength(),
                    preparedUpload.mimeType());
            log.debug(
                    "[STORAGE] Stored file: path={}, size={}, context={}",
                    preparedUpload.storagePath(),
                    preparedUpload.contentLength(),
                    preparedUpload.originalFilename());
        } catch (RuntimeException exception) {
            log.error(
                    "[STORAGE] Failed to store file: path={}, filename={}",
                    preparedUpload.storagePath(),
                    preparedUpload.originalFilename(),
                    exception);
            throw new SaudaException("Failed to store uploaded file");
        }
    }

    public StoredObject fetch(String storagePath) {
        return objectStorageProvider.getObject(storagePath);
    }

    public void delete(String storagePath) {
        objectStorageProvider.deleteObject(storagePath);
    }

    public String emptyChecksum() {
        return checksumCalculator.sha256Hex(new byte[0]);
    }

    private static String resolveMimeType(String extension, FileUploadPolicy policy) {
        String mimeType = policy.mimeTypeForExtension(extension);
        if (mimeType == null) {
            throw new SaudaException(policy.unsupportedTypeMessage());
        }
        return mimeType;
    }
}
