package com.sauda.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.entity.RawUpload;
import com.sauda.domain.enums.ImportStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.dto.imports.ParsedRowErrorItem;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ImportRunMapperTest {

    private final ImportRunMapper importRunMapper = Mappers.getMapper(ImportRunMapper.class);
    private final ParsedRowMapper parsedRowMapper = Mappers.getMapper(ParsedRowMapper.class);

    @Test
    void mapsImportRunToResponse() {
        UUID runId = UUID.randomUUID();
        UUID distributorId = UUID.randomUUID();
        UUID rawUploadId = UUID.randomUUID();

        Organization distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);
        distributor.setName("Tech Distributor");

        RawUpload rawUpload = new RawUpload();
        rawUpload.setId(rawUploadId);
        rawUpload.setOriginalFilename("prices.csv");

        ImportRun importRun = new ImportRun();
        importRun.setId(runId);
        importRun.setDistributor(distributor);
        importRun.setRawUpload(rawUpload);
        importRun.setAdapterKey("csv_v1");
        importRun.setStatus(ImportStatus.awaiting_approval);
        importRun.setTotalRows(10);
        importRun.setParsedRowsCount(10);
        importRun.setErrorRowsCount(2);
        importRun.setStartedAt(Instant.parse("2026-07-01T10:00:00Z"));
        importRun.setFinishedAt(Instant.parse("2026-07-01T10:01:00Z"));
        importRun.setCreatedAt(Instant.parse("2026-07-01T10:00:00Z"));
        importRun.setUpdatedAt(Instant.parse("2026-07-01T10:01:00Z"));

        var response = importRunMapper.toResponse(importRun);

        assertThat(response.id()).isEqualTo(runId);
        assertThat(response.distributorId()).isEqualTo(distributorId);
        assertThat(response.distributorName()).isEqualTo("Tech Distributor");
        assertThat(response.rawUploadId()).isEqualTo(rawUploadId);
        assertThat(response.originalFilename()).isEqualTo("prices.csv");
        assertThat(response.adapterKey()).isEqualTo("csv_v1");
        assertThat(response.status()).isEqualTo(ImportStatus.awaiting_approval);
        assertThat(response.totalRows()).isEqualTo(10);
        assertThat(response.errorRowsCount()).isEqualTo(2);
    }

    @Test
    void mapsParsedRowToResponse() {
        UUID rowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        ImportRun importRun = new ImportRun();
        importRun.setId(runId);

        ParsedRow parsedRow = new ParsedRow();
        parsedRow.setId(rowId);
        parsedRow.setImportRun(importRun);
        parsedRow.setSourceRowNumber(3);
        parsedRow.setRawRowData(Map.of("sku", "SKU-001"));
        parsedRow.setParsedData(Map.of("sku", "SKU-001", "name", "Item"));
        parsedRow.setStatus(ParsedRowStatus.needs_review);
        parsedRow.setWarnings(
                List.of(
                        Map.of(
                                "field",
                                "price_includes_vat",
                                "code",
                                "MISSING",
                                "message",
                                "price_includes_vat is not set")));
        parsedRow.setEditedAt(Instant.parse("2026-07-01T11:00:00Z"));

        var response = parsedRowMapper.toResponse(parsedRow);

        assertThat(response.id()).isEqualTo(rowId);
        assertThat(response.importRunId()).isEqualTo(runId);
        assertThat(response.sourceRowNumber()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(ParsedRowStatus.needs_review);
        assertThat(response.rawRowData()).containsEntry("sku", "SKU-001");
        assertThat(response.parsedData()).containsEntry("name", "Item");
        assertThat(response.errors()).isEmpty();
        assertThat(response.warnings())
                .containsExactly(
                        new ParsedRowErrorItem(
                                "price_includes_vat", "MISSING", "price_includes_vat is not set"));
        assertThat(response.editedAt()).isEqualTo(Instant.parse("2026-07-01T11:00:00Z"));
    }
}
