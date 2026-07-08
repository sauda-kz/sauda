package com.sauda.integration.storage.upload;

import com.sauda.exception.SaudaException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class FilenameSanitizer {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final Pattern UNSAFE_FILENAME_CHARS =
            Pattern.compile("[/\\\\\0\u0000-\u001F\u007F\"';:]");

    public String sanitize(String originalFilename) {
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

    public String extractExtension(String filename, FileUploadPolicy policy) {
        if (filename == null || filename.isBlank()) {
            throw new SaudaException(policy.unsupportedTypeMessage());
        }
        String basename = Path.of(filename).getFileName().toString();
        int dotIndex = basename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == basename.length() - 1) {
            throw new SaudaException(policy.unsupportedTypeMessage());
        }
        return basename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
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
}
