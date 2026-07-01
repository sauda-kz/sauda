package com.sauda.integration.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("test")
public class InMemoryObjectStorageProvider implements ObjectStorageProvider {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private final Map<String, String> contentTypes = new ConcurrentHashMap<>();

    @Override
    public void putObject(String key, InputStream content, long contentLength, String contentType) {
        log.info("[STORAGE][IN-MEMORY] Storing object: key={}, size={}", key, contentLength);
        try {
            byte[] bytes = content.readAllBytes();
            storage.put(key, bytes);
            contentTypes.put(key, contentType);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to store object in memory: " + key, exception);
        }
    }

    @Override
    public StoredObject getObject(String key) {
        byte[] bytes = storage.get(key);
        if (bytes == null) {
            throw new IllegalStateException("Stored object not found: " + key);
        }
        String contentType = contentTypes.getOrDefault(key, "application/octet-stream");
        return new StoredObject(new ByteArrayInputStream(bytes), bytes.length, contentType);
    }
}
