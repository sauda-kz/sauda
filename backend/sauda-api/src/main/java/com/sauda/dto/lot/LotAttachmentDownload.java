package com.sauda.dto.lot;

import com.sauda.integration.storage.StoredObject;

public record LotAttachmentDownload(String filename, String mimeType, StoredObject storedObject) {}
