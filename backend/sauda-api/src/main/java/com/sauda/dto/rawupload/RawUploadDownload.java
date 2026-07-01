package com.sauda.dto.rawupload;

import com.sauda.integration.storage.StoredObject;

public record RawUploadDownload(String filename, String mimeType, StoredObject storedObject) {}
