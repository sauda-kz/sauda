package com.sauda.integration.storage.upload;

public record PreparedUpload(
        String originalFilename,
        String mimeType,
        String storagePath,
        byte[] content,
        String checksumSha256) {

    public long contentLength() {
        return content.length;
    }
}
