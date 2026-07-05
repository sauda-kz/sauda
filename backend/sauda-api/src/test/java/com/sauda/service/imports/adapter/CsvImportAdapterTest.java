package com.sauda.service.imports.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.config.ImportProperties;
import com.sauda.service.imports.model.AdapterParseResult;
import com.sauda.service.imports.model.AdapterParsedRow;
import com.sauda.service.imports.model.RowError;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CsvImportAdapterTest {

    private CsvImportAdapter csvImportAdapter;

    @BeforeEach
    void setUp() {
        csvImportAdapter = new CsvImportAdapter(new ImportRowMapper(), new ImportProperties(100_000, null, 2, 4, 50, 500));
    }

    @Test
    void parseSampleFixtureCountsRowsAndErrors() throws Exception {
        byte[] content = Files.readAllBytes(Path.of("src/test/resources/imports/sample_price.csv"));

        AdapterParseResult result =
                csvImportAdapter.parse(
                        new ImportSource(
                                "sample_price.csv",
                                "text/csv",
                                () -> new ByteArrayInputStream(content)));

        assertThat(result.totalRows()).isEqualTo(6);
        assertThat(result.errorRows()).isEqualTo(5);

        AdapterParsedRow validRow = result.rows().get(0);
        assertThat(validRow.sourceRowNumber()).isEqualTo(2);
        assertThat(validRow.fields().sku()).isEqualTo("SKU-001");
        assertThat(validRow.fields().price()).isEqualByComparingTo(new BigDecimal("125000.50"));
        assertThat(validRow.fields().priceIncludesVat()).isTrue();
        assertThat(validRow.errors()).isEmpty();

        AdapterParsedRow invalidPriceRow = result.rows().get(1);
        assertThat(invalidPriceRow.errors())
                .extracting(RowError::field, RowError::code)
                .contains(org.assertj.core.groups.Tuple.tuple("price", "INVALID_NUMBER"));

        AdapterParsedRow negativeStockRow = result.rows().get(2);
        assertThat(negativeStockRow.errors())
                .extracting(RowError::field, RowError::code)
                .contains(org.assertj.core.groups.Tuple.tuple("stock_quantity", "NEGATIVE"));

        AdapterParsedRow missingSkuRow = result.rows().get(3);
        assertThat(missingSkuRow.errors())
                .extracting(RowError::field, RowError::code)
                .contains(org.assertj.core.groups.Tuple.tuple("sku", "REQUIRED"));

        AdapterParsedRow invalidBooleanRow = result.rows().get(4);
        assertThat(invalidBooleanRow.errors())
                .extracting(RowError::field, RowError::code)
                .contains(org.assertj.core.groups.Tuple.tuple("price_includes_vat", "INVALID_BOOLEAN"));
    }

    @Test
    void supportsCsvMimeAndExtension() {
        ImportSource source =
                new ImportSource("prices.csv", "text/csv", () -> emptyStream());

        assertThat(csvImportAdapter.supports(source)).isTrue();
        assertThat(csvImportAdapter.key()).isEqualTo("csv_v1");
    }

    @Test
    void parseReturnsFileLevelErrorWhenHeaderMissing() {
        AdapterParseResult result =
                csvImportAdapter.parse(
                        new ImportSource(
                                "empty.csv",
                                "text/csv",
                                () -> new ByteArrayInputStream(new byte[0])));

        assertThat(result.totalRows()).isOne();
        assertThat(result.rows().getFirst().errors())
                .containsExactly(
                        new RowError(null, ImportAdapterSupport.MISSING_HEADER, "Header row is required"));
    }

    @Test
    void parseDetectsSemicolonDelimiter() {
        String content = "sku;name;price\nSKU-010;Item;100\n";
        AdapterParseResult result =
                csvImportAdapter.parse(
                        new ImportSource(
                                "prices.csv",
                                "text/csv",
                                () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));

        assertThat(result.totalRows()).isOne();
        assertThat(result.rows().getFirst().fields().sku()).isEqualTo("SKU-010");
        assertThat(result.rows().getFirst().fields().price()).isEqualByComparingTo(new BigDecimal("100"));
    }

    private static InputStream emptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
