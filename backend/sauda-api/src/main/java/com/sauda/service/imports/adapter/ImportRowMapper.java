package com.sauda.service.imports.adapter;

import com.sauda.domain.enums.StockStatus;
import com.sauda.domain.stock.StockTextMapper;
import com.sauda.service.imports.model.ImportRowFields;
import com.sauda.service.imports.model.RowError;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ImportRowMapper {

    private static final Set<String> TRUE_VALUES = Set.of("true", "1", "yes", "да");
    private static final Set<String> FALSE_VALUES = Set.of("false", "0", "no", "нет");

    public ImportRowMappingResult map(Map<String, String> rawCells) {
        List<RowError> errors = new ArrayList<>();
        List<RowError> warnings = new ArrayList<>();

        String sku = trimToNull(rawCells.get("sku"));
        if (sku == null) {
            errors.add(new RowError("sku", "REQUIRED", "SKU is required"));
        }

        String name = trimToNull(rawCells.get("name"));
        if (name == null) {
            errors.add(new RowError("name", "REQUIRED", "Name is required"));
        }

        String brand = trimToNull(rawCells.get("brand"));
        String mpn = trimToNull(rawCells.get("mpn"));
        BigDecimal price = parsePrice(rawCells.get("price"), errors);
        Boolean priceIncludesVat =
                parseBoolean(rawCells.get("price_includes_vat"), "price_includes_vat", errors, warnings);
        Integer stockQuantity =
                parseNonNegativeInteger(rawCells.get("stock_quantity"), "stock_quantity", errors);
        StockStatus stockStatus = parseStockStatus(rawCells.get("stock_status"), errors);
        Integer leadTimeDays =
                parseOptionalNonNegativeInteger(rawCells.get("lead_time_days"), "lead_time_days", errors);

        ImportRowFields fields =
                new ImportRowFields(
                        sku,
                        name,
                        brand,
                        mpn,
                        price,
                        priceIncludesVat,
                        stockQuantity,
                        stockStatus,
                        leadTimeDays);
        return new ImportRowMappingResult(fields, List.copyOf(errors), List.copyOf(warnings));
    }

    static String normalizeColumnName(String columnName) {
        if (columnName == null) {
            return "";
        }
        return columnName
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replaceAll("_+", "_");
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal parsePrice(String raw, List<RowError> errors) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(value.replace(" ", "").replace(',', '.'));
            if (price.signum() < 0) {
                errors.add(new RowError("price", "NEGATIVE", "Price must not be negative"));
                return null;
            }
            return price;
        } catch (NumberFormatException exception) {
            errors.add(new RowError("price", "INVALID_NUMBER", "Invalid price: " + raw));
            return null;
        }
    }

    private static Boolean parseBoolean(
            String raw, String field, List<RowError> errors, List<RowError> warnings) {
        String value = trimToNull(raw);
        if (value == null) {
            warnings.add(new RowError(field, "MISSING", field + " is not set"));
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (TRUE_VALUES.contains(normalized)) {
            return true;
        }
        if (FALSE_VALUES.contains(normalized)) {
            return false;
        }
        errors.add(new RowError(field, "INVALID_BOOLEAN", "Invalid boolean value: " + raw));
        return null;
    }

    private static Integer parseNonNegativeInteger(String raw, String field, List<RowError> errors) {
        return parseInteger(raw, field, errors);
    }

    private static Integer parseOptionalNonNegativeInteger(
            String raw, String field, List<RowError> errors) {
        return parseInteger(raw, field, errors);
    }

    private static Integer parseInteger(String raw, String field, List<RowError> errors) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal number = new BigDecimal(value.replace(" ", "").replace(',', '.'));
            int intValue = number.intValueExact();
            if (intValue < 0) {
                errors.add(new RowError(field, "NEGATIVE", field + " must not be negative"));
                return null;
            }
            return intValue;
        } catch (ArithmeticException | NumberFormatException exception) {
            errors.add(new RowError(field, "INVALID_NUMBER", "Invalid number for " + field + ": " + raw));
            return null;
        }
    }

    private static StockStatus parseStockStatus(String raw, List<RowError> errors) {
        String value = trimToNull(raw);
        if (value == null) {
            return StockStatus.unknown;
        }

        String enumKey = value.toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        try {
            return StockStatus.valueOf(enumKey);
        } catch (IllegalArgumentException ignored) {
            // fall through to synonym mapping
        }

        StockStatus mapped = StockTextMapper.parseStatus(value);
        if (mapped != StockStatus.unknown) {
            return mapped;
        }

        errors.add(new RowError("stock_status", "INVALID_ENUM", "Unknown stock status: " + raw));
        return StockStatus.unknown;
    }

    public record ImportRowMappingResult(
            ImportRowFields fields, List<RowError> errors, List<RowError> warnings) {}
}
