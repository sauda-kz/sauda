package com.sauda.service.imports.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.domain.enums.StockStatus;
import com.sauda.service.imports.model.RowError;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportRowMapperTest {

    private ImportRowMapper rowMapper;

    @BeforeEach
    void setUp() {
        rowMapper = new ImportRowMapper();
    }

    @Test
    void mapValidRow() {
        ImportRowMapper.ImportRowMappingResult result =
                rowMapper.map(
                        Map.of(
                                "sku", " SKU-001 ",
                                "name", " SSD Samsung ",
                                "brand", "Samsung",
                                "mpn", "990",
                                "price", "125000,50",
                                "price_includes_vat", "да",
                                "stock_quantity", "15",
                                "stock_status", "в наличии",
                                "lead_time_days", "3"));

        assertThat(result.errors()).isEmpty();
        assertThat(result.fields().sku()).isEqualTo("SKU-001");
        assertThat(result.fields().name()).isEqualTo("SSD Samsung");
        assertThat(result.fields().price()).isEqualByComparingTo(new BigDecimal("125000.50"));
        assertThat(result.fields().priceIncludesVat()).isTrue();
        assertThat(result.fields().stockQuantity()).isEqualTo(15);
        assertThat(result.fields().stockStatus()).isEqualTo(StockStatus.in_stock);
        assertThat(result.fields().leadTimeDays()).isEqualTo(3);
    }

    @Test
    void mapReportsRequiredFields() {
        ImportRowMapper.ImportRowMappingResult result = rowMapper.map(Map.of("price", "100"));

        assertThat(result.errors())
                .extracting(RowError::field, RowError::code)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("sku", "REQUIRED"),
                        org.assertj.core.groups.Tuple.tuple("name", "REQUIRED"));
    }

    @Test
    void mapReportsInvalidPriceAndNegativeStock() {
        ImportRowMapper.ImportRowMappingResult result =
                rowMapper.map(
                        Map.of(
                                "sku", "SKU-002",
                                "name", "Broken item",
                                "price", "abc",
                                "stock_quantity", "-5"));

        assertThat(result.errors())
                .extracting(RowError::field, RowError::code)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("price", "INVALID_NUMBER"),
                        org.assertj.core.groups.Tuple.tuple("stock_quantity", "NEGATIVE"));
    }

    @Test
    void mapReportsInvalidBooleanAndMissingVatWarning() {
        ImportRowMapper.ImportRowMappingResult result =
                rowMapper.map(
                        Map.of(
                                "sku", "SKU-003",
                                "name", "Item",
                                "price_includes_vat", "maybe"));

        assertThat(result.errors())
                .containsExactly(
                        new RowError(
                                "price_includes_vat",
                                "INVALID_BOOLEAN",
                                "Invalid boolean value: maybe"));
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void mapAddsWarningWhenVatMissing() {
        ImportRowMapper.ImportRowMappingResult result =
                rowMapper.map(Map.of("sku", "SKU-004", "name", "Item"));

        assertThat(result.warnings())
                .containsExactly(
                        new RowError(
                                "price_includes_vat", "MISSING", "price_includes_vat is not set"));
    }

    @Test
    void mapUsesUnknownStatusForBlankValue() {
        ImportRowMapper.ImportRowMappingResult result =
                rowMapper.map(Map.of("sku", "SKU-005", "name", "Item", "stock_status", "   "));

        assertThat(result.fields().stockStatus()).isEqualTo(StockStatus.unknown);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void mapReportsInvalidEnumForUnknownStatusText() {
        ImportRowMapper.ImportRowMappingResult result =
                rowMapper.map(
                        Map.of("sku", "SKU-006", "name", "Item", "stock_status", "not_a_status"));

        assertThat(result.errors())
                .containsExactly(
                        new RowError(
                                "stock_status",
                                "INVALID_ENUM",
                                "Unknown stock status: not_a_status"));
        assertThat(result.fields().stockStatus()).isEqualTo(StockStatus.unknown);
    }

    @Test
    void normalizeColumnNameLowercasesAndUnderscores() {
        assertThat(ImportRowMapper.normalizeColumnName(" Price-Includes-VAT "))
                .isEqualTo("price_includes_vat");
    }
}
