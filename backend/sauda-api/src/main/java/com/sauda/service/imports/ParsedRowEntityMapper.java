package com.sauda.service.imports;

import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.service.imports.model.AdapterParsedRow;
import com.sauda.service.imports.model.ImportRowFields;
import com.sauda.service.imports.model.RowError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ParsedRowEntityMapper {

    private ParsedRowEntityMapper() {}

    static ParsedRow toEntity(ImportRun importRun, AdapterParsedRow row, ParsedRowStatus status) {
        ParsedRow entity = new ParsedRow();
        entity.setImportRun(importRun);
        entity.setSourceRowNumber(row.sourceRowNumber());
        entity.setRawRowData(row.rawRowData());
        entity.setParsedData(toParsedData(row.fields()));
        entity.setStatus(status);
        entity.setErrors(toErrorMaps(row.errors()));
        entity.setWarnings(toErrorMaps(row.warnings()));
        return entity;
    }

    static void applyValidationResult(
            ParsedRow entity,
            ImportRowFields fields,
            List<RowError> errors,
            List<RowError> warnings,
            ParsedRowStatus status) {
        entity.setParsedData(toParsedData(fields));
        entity.setErrors(toErrorMaps(errors));
        entity.setWarnings(toErrorMaps(warnings));
        entity.setStatus(status);
    }

    private static Map<String, Object> toParsedData(ImportRowFields fields) {
        if (fields == null) {
            return Map.of();
        }
        Map<String, Object> parsedData = new LinkedHashMap<>();
        putIfNotNull(parsedData, "sku", fields.sku());
        putIfNotNull(parsedData, "name", fields.name());
        putIfNotNull(parsedData, "brand", fields.brand());
        putIfNotNull(parsedData, "mpn", fields.mpn());
        putIfNotNull(parsedData, "price", fields.price());
        putIfNotNull(parsedData, "price_includes_vat", fields.priceIncludesVat());
        putIfNotNull(parsedData, "stock_quantity", fields.stockQuantity());
        if (fields.stockStatus() != null) {
            parsedData.put("stock_status", fields.stockStatus().name());
        }
        putIfNotNull(parsedData, "lead_time_days", fields.leadTimeDays());
        return parsedData;
    }

    private static List<Map<String, Object>> toErrorMaps(List<RowError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        return errors.stream().map(ParsedRowEntityMapper::toErrorMap).toList();
    }

    private static Map<String, Object> toErrorMap(RowError error) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("field", error.field());
        map.put("code", error.code());
        map.put("message", error.message());
        return map;
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
