package com.sauda.integration.storage;

import java.io.InputStream;

public interface ObjectStorageProvider {

    void putObject(String key, InputStream content, long contentLength, String contentType);

    StoredObject getObject(String key);
}
