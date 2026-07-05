package com.sauda.service.imports.adapter;

import com.sauda.config.ImportProperties;
import com.sauda.exception.SaudaException;
import com.sauda.service.imports.model.AdapterParseResult;
import com.sauda.service.imports.model.AdapterParsedRow;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CsvImportAdapter implements ImportAdapter {

    private static final String ADAPTER_KEY = "csv_v1";

    private final ImportRowMapper rowMapper;
    private final ImportProperties importProperties;

    public CsvImportAdapter(ImportRowMapper rowMapper, ImportProperties importProperties) {
        this.rowMapper = rowMapper;
        this.importProperties = importProperties;
    }

    @Override
    public String key() {
        return ADAPTER_KEY;
    }

    @Override
    public boolean supports(ImportSource source) {
        return "text/csv".equalsIgnoreCase(source.mimeType())
                || source.originalFilename().toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    @Override
    public AdapterParseResult parse(ImportSource source) {
        try (InputStream inputStream = source.content().get();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            char delimiter = resolveDelimiter(reader, source);
            reader.mark(1);
            if (reader.read() == -1) {
                return ImportAdapterSupport.missingHeaderResult();
            }
            reader.reset();

            CSVFormat format =
                    CSVFormat.DEFAULT
                            .builder()
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .setDelimiter(delimiter)
                            .setIgnoreEmptyLines(false)
                            .setTrim(true)
                            .build();

            try (CSVParser parser = new CSVParser(reader, format)) {
                if (parser.getHeaderMap() == null || parser.getHeaderMap().isEmpty()) {
                    return ImportAdapterSupport.missingHeaderResult();
                }

                List<AdapterParsedRow> rows = new ArrayList<>();
                int dataRowIndex = 0;
                for (CSVRecord record : parser) {
                    Map<String, String> rawCells = toRawCells(record);
                    if (ImportAdapterSupport.isBlankRow(rawCells)) {
                        continue;
                    }
                    dataRowIndex++;
                    if (dataRowIndex > importProperties.maxRows()) {
                        log.warn(
                                "CSV import row limit exceeded: filename={}, maxRows={}",
                                source.originalFilename(),
                                importProperties.maxRows());
                        return ImportAdapterSupport.rowLimitExceededResult(importProperties.maxRows());
                    }
                    rows.add(
                            ImportAdapterSupport.toParsedRow(
                                    (int) record.getRecordNumber() + 1, rawCells, rowMapper));
                }
                return new AdapterParseResult(rows);
            }
        } catch (IOException exception) {
            throw new SaudaException("Failed to read CSV import file", exception);
        }
    }

    private char resolveDelimiter(BufferedReader reader, ImportSource source) throws IOException {
        Character configuredDelimiter = importProperties.csvDelimiter();
        if (configuredDelimiter != null) {
            return configuredDelimiter;
        }

        reader.mark(8192);
        String firstLine = reader.readLine();
        reader.reset();
        if (firstLine == null || firstLine.isBlank()) {
            return ',';
        }

        int commas = countChar(firstLine, ',');
        int semicolons = countChar(firstLine, ';');
        char delimiter = semicolons > commas ? ';' : ',';
        log.debug(
                "Detected CSV delimiter '{}' for filename={}",
                delimiter,
                source.originalFilename());
        return delimiter;
    }

    private static int countChar(String line, char character) {
        int count = 0;
        for (int index = 0; index < line.length(); index++) {
            if (line.charAt(index) == character) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, String> toRawCells(CSVRecord record) {
        Map<String, String> rawCells = new LinkedHashMap<>();
        record.toMap().forEach((column, value) -> rawCells.put(column, value == null ? "" : value));
        return rawCells;
    }
}
