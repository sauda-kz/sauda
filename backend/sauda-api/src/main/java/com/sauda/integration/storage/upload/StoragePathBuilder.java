package com.sauda.integration.storage.upload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class StoragePathBuilder {

    private static final DateTimeFormatter STORAGE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    public String buildTimestampedPath(String pathPrefix, String sanitizedFilename) {
        String timestamp = STORAGE_TIMESTAMP_FORMATTER.format(Instant.now());
        return pathPrefix + "/" + timestamp + "_" + sanitizedFilename;
    }
}
