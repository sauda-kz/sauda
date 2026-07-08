package com.sauda.service.imports.adapter;

import com.sauda.exception.SaudaException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImportAdapterRegistry {

    private final List<ImportAdapter> adapters;

    public ImportAdapterRegistry(List<ImportAdapter> adapters) {
        this.adapters = adapters == null ? List.of() : List.copyOf(adapters);
    }

    public ImportAdapter resolve(ImportSource source) {
        for (ImportAdapter adapter : adapters) {
            if (adapter.supports(source)) {
                log.info(
                        "Resolved import adapter: key={}, filename={}, mimeType={}",
                        adapter.key(),
                        source.originalFilename(),
                        source.mimeType());
                return adapter;
            }
        }
        throw new SaudaException(
                "Unsupported import format: "
                        + source.originalFilename()
                        + " ("
                        + source.mimeType()
                        + ")");
    }

    public ImportAdapter getByKey(String key) {
        return adapters.stream()
                .filter(adapter -> adapter.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new SaudaException("Import adapter not found: " + key));
    }
}
