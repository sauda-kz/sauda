package com.sauda.integration.storage;

import java.io.InputStream;

public record StoredObject(InputStream content, long contentLength, String contentType) {}
