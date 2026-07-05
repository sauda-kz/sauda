package com.sauda.service.imports.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sauda.exception.SaudaException;
import com.sauda.service.imports.model.AdapterParseResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportAdapterRegistryTest {

    private ImportAdapterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ImportAdapterRegistry(List.of(new CsvImportAdapter(), new ExcelImportAdapter()));
    }

    @Test
    void resolveReturnsFirstMatchingAdapter() {
        ImportSource source = csvSource("prices.csv", "text/csv");

        ImportAdapter adapter = registry.resolve(source);

        assertThat(adapter.key()).isEqualTo("csv");
    }

    @Test
    void resolveThrowsWhenNoAdapterSupportsSource() {
        ImportSource source = csvSource("prices.json", "application/json");

        assertThatThrownBy(() -> registry.resolve(source))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("Unsupported import format")
                .hasMessageContaining("prices.json");
    }

    @Test
    void getByKeyReturnsAdapter() {
        ImportAdapter adapter = registry.getByKey("excel");

        assertThat(adapter.key()).isEqualTo("excel");
    }

    @Test
    void getByKeyThrowsWhenAdapterMissing() {
        assertThatThrownBy(() -> registry.getByKey("xml"))
                .isInstanceOf(SaudaException.class)
                .hasMessage("Import adapter not found: xml");
    }

    private static ImportSource csvSource(String filename, String mimeType) {
        return new ImportSource(
                filename,
                mimeType,
                () -> new ByteArrayInputStream("sku,price".getBytes(StandardCharsets.UTF_8)));
    }

    private static final class CsvImportAdapter implements ImportAdapter {

        @Override
        public String key() {
            return "csv";
        }

        @Override
        public boolean supports(ImportSource source) {
            return "text/csv".equals(source.mimeType())
                    || source.originalFilename().endsWith(".csv");
        }

        @Override
        public AdapterParseResult parse(ImportSource source) {
            return new AdapterParseResult(List.of());
        }
    }

    private static final class ExcelImportAdapter implements ImportAdapter {

        @Override
        public String key() {
            return "excel";
        }

        @Override
        public boolean supports(ImportSource source) {
            return source.originalFilename().endsWith(".xlsx");
        }

        @Override
        public AdapterParseResult parse(ImportSource source) {
            return new AdapterParseResult(List.of());
        }
    }
}
