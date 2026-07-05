package com.sauda.service.imports.adapter;

import java.io.InputStream;
import java.util.function.Supplier;

public record ImportSource(
        String originalFilename, String mimeType, Supplier<InputStream> content) {}
