package com.sauda.integration.supplier;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev", "test"})
public class MockSupplierImportProvider implements SupplierImportProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSupplierImportProvider.class);

    @Override
    public List<String> importCatalog(String supplierCode) {
        log.info("[MOCK] Importing catalog for supplier {}", supplierCode);
        return List.of("SKU-001", "SKU-002", "SKU-003");
    }
}
