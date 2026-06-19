package com.sauda.integration.supplier;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class RealSupplierImportProvider implements SupplierImportProvider {

    private static final Logger log = LoggerFactory.getLogger(RealSupplierImportProvider.class);

    private final String importUrl;

    public RealSupplierImportProvider(
            @Value("${sauda.integrations.supplier.import-url:}") String importUrl) {
        this.importUrl = importUrl;
    }

    @Override
    public List<String> importCatalog(String supplierCode) {
        log.info("[SUPPLIER] Importing catalog for {} from {}", supplierCode, importUrl);
        // Future: HTTP/FTP integration with supplier systems
        return List.of();
    }
}
