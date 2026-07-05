package com.sauda.service.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.domain.enums.StockStatus;
import com.sauda.service.imports.model.AdapterParsedRow;
import com.sauda.service.imports.model.ImportRowFields;
import com.sauda.service.imports.model.RowError;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParsedRowValidatorTest {

    private ParsedRowValidator parsedRowValidator;

    @BeforeEach
    void setUp() {
        parsedRowValidator = new ParsedRowValidator();
    }

    @Test
    void resolvesValidRow() {
        ParsedRowStatus status =
                parsedRowValidator.resolveStatus(
                        parsedRow(List.of(), List.of(), sampleFields()));

        assertThat(status).isEqualTo(ParsedRowStatus.valid);
    }

    @Test
    void resolvesErrorRow() {
        ParsedRowStatus status =
                parsedRowValidator.resolveStatus(
                        parsedRow(
                                List.of(new RowError("price", "INVALID_NUMBER", "Invalid price")),
                                List.of(),
                                sampleFields()));

        assertThat(status).isEqualTo(ParsedRowStatus.error);
    }

    @Test
    void resolvesNeedsReviewWhenOnlyWarningsPresent() {
        ParsedRowStatus status =
                parsedRowValidator.resolveStatus(
                        parsedRow(
                                List.of(),
                                List.of(
                                        new RowError(
                                                "price_includes_vat",
                                                "MISSING",
                                                "price_includes_vat is not set")),
                                sampleFields()));

        assertThat(status).isEqualTo(ParsedRowStatus.needs_review);
    }

    @Test
    void resolveEditedStatusMarksValidRowAsEdited() {
        ParsedRowStatus status =
                parsedRowValidator.resolveEditedStatus(parsedRow(List.of(), List.of(), sampleFields()));

        assertThat(status).isEqualTo(ParsedRowStatus.edited);
    }

    @Test
    void resolveEditedStatusKeepsErrorWhenValidationFails() {
        ParsedRowStatus status =
                parsedRowValidator.resolveEditedStatus(
                        parsedRow(
                                List.of(new RowError("price", "INVALID_NUMBER", "Invalid price")),
                                List.of(),
                                sampleFields()));

        assertThat(status).isEqualTo(ParsedRowStatus.error);
    }

    private static AdapterParsedRow parsedRow(
            List<RowError> errors, List<RowError> warnings, ImportRowFields fields) {
        return new AdapterParsedRow(2, Map.of("sku", "SKU-001"), fields, errors, warnings);
    }

    private static ImportRowFields sampleFields() {
        return new ImportRowFields(
                "SKU-001",
                "Item",
                "Brand",
                "MPN",
                new BigDecimal("100"),
                true,
                5,
                StockStatus.in_stock,
                1);
    }
}
