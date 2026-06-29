package com.sauda.integration.supplier;

import java.util.List;

public interface SupplierImportProvider {

    List<String> importCatalog(String supplierCode);
}
