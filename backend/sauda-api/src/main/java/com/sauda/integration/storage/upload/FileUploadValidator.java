package com.sauda.integration.storage.upload;

import com.sauda.exception.SaudaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileUploadValidator {

    private final FilenameSanitizer filenameSanitizer;

    public FileUploadValidator(FilenameSanitizer filenameSanitizer) {
        this.filenameSanitizer = filenameSanitizer;
    }

    public void validatePresent(MultipartFile file, FileUploadPolicy policy, long maxFileSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new SaudaException("File is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new SaudaException(
                    "File exceeds maximum allowed size of " + maxFileSizeBytes + " bytes");
        }
        String extension = filenameSanitizer.extractExtension(file.getOriginalFilename(), policy);
        if (!policy.allowedExtensions().contains(extension)) {
            throw new SaudaException(policy.unsupportedTypeMessage());
        }
    }

    public void validateContentSize(byte[] content, long maxFileSizeBytes) {
        if (content.length > maxFileSizeBytes) {
            throw new SaudaException(
                    "File exceeds maximum allowed size of " + maxFileSizeBytes + " bytes");
        }
    }
}
