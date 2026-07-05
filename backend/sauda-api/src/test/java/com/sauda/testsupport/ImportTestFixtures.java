package com.sauda.testsupport;

import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.entity.RawUpload;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.domain.enums.RawUploadStatus;
import com.sauda.dto.imports.UpdateParsedRowRequest;
import com.sauda.service.imports.model.ImportRowFields;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ImportTestFixtures {

    private ImportTestFixtures() {}

    public static Organization sampleDistributor(UUID distributorId) {
        Organization distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);
        distributor.setName("Demo Distributor");
        return distributor;
    }

    public static RawUpload sampleRawUpload(UUID uploadId, Organization distributor) {
        RawUpload rawUpload = new RawUpload();
        rawUpload.setId(uploadId);
        rawUpload.setDistributor(distributor);
        rawUpload.setOriginalFilename("demo_prices.csv");
        rawUpload.setMimeType("text/csv");
        rawUpload.setStoragePath("raw/" + distributor.getId() + "/demo_prices.csv");
        rawUpload.setStatus(RawUploadStatus.processed);
        return rawUpload;
    }

    public static ImportRun sampleImportRun(
            UUID runId, Organization distributor, RawUpload rawUpload, ImportStatus status) {
        ImportRun importRun = new ImportRun();
        importRun.setId(runId);
        importRun.setDistributor(distributor);
        importRun.setRawUpload(rawUpload);
        importRun.setAdapterKey("csv_v1");
        importRun.setStatus(status);
        importRun.setSourceFilename(rawUpload.getOriginalFilename());
        importRun.setTotalRows(3);
        importRun.setParsedRowsCount(3);
        importRun.setErrorRowsCount(1);
        importRun.setStartedAt(Instant.parse("2026-07-01T10:00:00Z"));
        importRun.setFinishedAt(Instant.parse("2026-07-01T10:01:00Z"));
        importRun.setCreatedAt(Instant.parse("2026-07-01T10:00:00Z"));
        importRun.setUpdatedAt(Instant.parse("2026-07-01T10:01:00Z"));
        return importRun;
    }

    public static ParsedRow sampleParsedRow(
            UUID rowId, ImportRun importRun, int rowNumber, ParsedRowStatus status) {
        ParsedRow parsedRow = new ParsedRow();
        parsedRow.setId(rowId);
        parsedRow.setImportRun(importRun);
        parsedRow.setSourceRowNumber(rowNumber);
        parsedRow.setStatus(status);
        parsedRow.setRawRowData(Map.of("sku", "SKU-" + rowNumber));
        parsedRow.setParsedData(toParsedDataMap(sampleImportRowFields("SKU-" + rowNumber)));
        parsedRow.setErrors(null);
        parsedRow.setWarnings(null);
        return parsedRow;
    }

    public static ImportRowFields sampleImportRowFields(String sku) {
        return new ImportRowFields(
                sku,
                "Demo item " + sku,
                "Samsung",
                "MPN-1",
                new BigDecimal("125000"),
                true,
                10,
                com.sauda.domain.enums.StockStatus.in_stock,
                3);
    }

    public static UpdateParsedRowRequest sampleUpdateParsedRowRequest() {
        return new UpdateParsedRowRequest(
                "SKU-001",
                "Updated item",
                "Samsung",
                "MPN-1",
                new BigDecimal("130000"),
                true,
                12,
                com.sauda.domain.enums.StockStatus.in_stock,
                2);
    }

    public static Map<String, Object> toParsedDataMap(ImportRowFields fields) {
        Map<String, Object> parsedData = new LinkedHashMap<>();
        parsedData.put("sku", fields.sku());
        parsedData.put("name", fields.name());
        parsedData.put("brand", fields.brand());
        parsedData.put("mpn", fields.mpn());
        parsedData.put("price", fields.price());
        parsedData.put("price_includes_vat", fields.priceIncludesVat());
        parsedData.put("stock_quantity", fields.stockQuantity());
        parsedData.put("stock_status", fields.stockStatus().name());
        parsedData.put("lead_time_days", fields.leadTimeDays());
        return parsedData;
    }

    public static List<Map<String, Object>> sampleRowErrors() {
        return List.of(
                Map.of(
                        "field", "price",
                        "code", "INVALID_NUMBER",
                        "message", "Invalid price"));
    }
}
