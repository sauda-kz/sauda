package com.sauda.integration.storage.upload;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record FileUploadPolicy(
        Set<String> allowedExtensions,
        Map<String, String> mimeTypesByExtension,
        String contextLabel) {

    public String unsupportedTypeMessage() {
        String extensions =
                allowedExtensions.stream()
                        .sorted()
                        .map(extension -> "." + extension)
                        .collect(Collectors.joining(", "));
        return "Unsupported file type. Allowed extensions: " + extensions;
    }

    public String mimeTypeForExtension(String extension) {
        return mimeTypesByExtension.get(extension);
    }
}
