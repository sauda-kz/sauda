package com.sauda.service.imports.adapter;

import com.sauda.service.imports.model.AdapterParseResult;

public interface ImportAdapter {

    /** Stable adapter key persisted in import_run.adapter_key. */
    String key();

    /** Whether this adapter can handle the file (mime, extension, signature). */
    boolean supports(ImportSource source);

    /**
     * Read the source and return a unified parse result. Does not throw on bad rows — errors go
     * into {@code RowError}; throws only when the file is unreadable.
     */
    AdapterParseResult parse(ImportSource source);
}
