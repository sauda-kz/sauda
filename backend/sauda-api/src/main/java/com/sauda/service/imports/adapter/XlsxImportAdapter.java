package com.sauda.service.imports.adapter;

import com.sauda.config.ImportProperties;
import com.sauda.exception.SaudaException;
import com.sauda.service.imports.model.AdapterParseResult;
import com.sauda.service.imports.model.AdapterParsedRow;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XlsxImportAdapter implements ImportAdapter {

    private static final String ADAPTER_KEY = "xlsx_v1";
    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ImportRowMapper rowMapper;
    private final ImportProperties importProperties;

    public XlsxImportAdapter(ImportRowMapper rowMapper, ImportProperties importProperties) {
        this.rowMapper = rowMapper;
        this.importProperties = importProperties;
    }

    @Override
    public String key() {
        return ADAPTER_KEY;
    }

    @Override
    public boolean supports(ImportSource source) {
        return XLSX_MIME.equalsIgnoreCase(source.mimeType())
                || source.originalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx");
    }

    @Override
    public AdapterParseResult parse(ImportSource source) {
        ZipSecureFile.setMinInflateRatio(0.001d);
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream inputStream = source.content().get();
                Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return ImportAdapterSupport.missingHeaderResult();
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return ImportAdapterSupport.missingHeaderResult();
            }

            List<String> headers = readHeaders(headerRow, dataFormatter);
            if (headers.isEmpty()) {
                return ImportAdapterSupport.missingHeaderResult();
            }

            List<AdapterParsedRow> rows = new ArrayList<>();
            int dataRowIndex = 0;
            for (int rowIndex = sheet.getFirstRowNum() + 1;
                    rowIndex <= sheet.getLastRowNum();
                    rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Map<String, String> rawCells = readRowCells(row, headers, dataFormatter);
                if (ImportAdapterSupport.isBlankRow(rawCells)) {
                    continue;
                }

                dataRowIndex++;
                if (dataRowIndex > importProperties.maxRows()) {
                    log.warn(
                            "XLSX import row limit exceeded: filename={}, maxRows={}",
                            source.originalFilename(),
                            importProperties.maxRows());
                    return ImportAdapterSupport.rowLimitExceededResult(importProperties.maxRows());
                }

                rows.add(ImportAdapterSupport.toParsedRow(rowIndex + 1, rawCells, rowMapper));
            }
            return new AdapterParseResult(rows);
        } catch (IOException exception) {
            throw new SaudaException("Failed to read XLSX import file", exception);
        }
    }

    private static List<String> readHeaders(Row headerRow, DataFormatter dataFormatter) {
        List<String> headers = new ArrayList<>();
        for (int cellIndex = headerRow.getFirstCellNum();
                cellIndex >= 0 && cellIndex < headerRow.getLastCellNum();
                cellIndex++) {
            headers.add(formatCell(headerRow.getCell(cellIndex), dataFormatter));
        }
        return headers;
    }

    private static Map<String, String> readRowCells(
            Row row, List<String> headers, DataFormatter dataFormatter) {
        Map<String, String> rawCells = new LinkedHashMap<>();
        for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
            String header = headers.get(cellIndex);
            if (ImportRowMapper.trimToNull(header) == null) {
                continue;
            }
            rawCells.put(header, formatCell(row.getCell(cellIndex), dataFormatter));
        }
        return rawCells;
    }

    private static String formatCell(Cell cell, DataFormatter dataFormatter) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell);
    }
}
