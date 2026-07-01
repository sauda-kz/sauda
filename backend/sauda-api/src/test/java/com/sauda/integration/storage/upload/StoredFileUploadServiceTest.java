package com.sauda.integration.storage.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.sauda.config.StorageProperties;
import com.sauda.exception.SaudaException;
import com.sauda.integration.storage.ObjectStorageProvider;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class StoredFileUploadServiceTest {

    @Mock private ObjectStorageProvider objectStorageProvider;

    private StoredFileUploadService storedFileUploadService;

    @BeforeEach
    void setUp() {
        FilenameSanitizer filenameSanitizer = new FilenameSanitizer();
        storedFileUploadService =
                new StoredFileUploadService(
                        objectStorageProvider,
                        new FileUploadValidator(filenameSanitizer),
                        filenameSanitizer,
                        new StoragePathBuilder(),
                        new ChecksumCalculator(),
                        new StorageProperties(
                                "http://localhost:9000",
                                "key",
                                "secret",
                                "bucket",
                                "us-east-1",
                                1024));
    }

    @Test
    void prepareAndStoreRawCsvUpload() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "prices.csv",
                        "text/csv",
                        "sku,price".getBytes(StandardCharsets.UTF_8));

        PreparedUpload prepared =
                storedFileUploadService.prepare(
                        file, FileUploadPolicies.RAW_DISTRIBUTOR_PRICE, "raw/" + "dist-id");
        storedFileUploadService.store(prepared);

        assertThat(prepared.originalFilename()).isEqualTo("prices.csv");
        assertThat(prepared.mimeType()).isEqualTo("text/csv");
        assertThat(prepared.storagePath()).startsWith("raw/dist-id/");
        assertThat(prepared.checksumSha256()).isNotBlank();
        verify(objectStorageProvider)
                .putObject(
                        eq(prepared.storagePath()),
                        any(),
                        eq(prepared.contentLength()),
                        eq("text/csv"));
    }

    @Test
    void preparePreservesUnicodeFilename() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "Прайс остатки.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[] {1, 2, 3});

        PreparedUpload prepared =
                storedFileUploadService.prepare(
                        file, FileUploadPolicies.RAW_DISTRIBUTOR_PRICE, "raw/" + "dist-id");

        assertThat(prepared.originalFilename()).isEqualTo("Прайс остатки.xlsx");
        assertThat(prepared.storagePath()).endsWith("_Прайс остатки.xlsx");
    }

    @Test
    void prepareRejectsUnsupportedExtensionForLotPolicy() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "spec.exe", "application/octet-stream", new byte[] {1});

        assertThatThrownBy(
                        () ->
                                storedFileUploadService.prepare(
                                        file,
                                        FileUploadPolicies.LOT_ATTACHMENT,
                                        "lots/lot-id"))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void prepareRejectsEmptyFile() {
        MockMultipartFile file =
                new MockMultipartFile("file", "prices.csv", "text/csv", new byte[0]);

        assertThatThrownBy(
                        () ->
                                storedFileUploadService.prepare(
                                        file,
                                        FileUploadPolicies.RAW_DISTRIBUTOR_PRICE,
                                        "raw/dist-id"))
                .isInstanceOf(SaudaException.class)
                .hasMessage("File is required");
    }

    @Test
    void prepareRejectsOversizedContent() {
        byte[] content = "x".repeat(2048).getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file =
                new MockMultipartFile("file", "prices.csv", "text/csv", content);

        assertThatThrownBy(
                        () ->
                                storedFileUploadService.prepare(
                                        file,
                                        FileUploadPolicies.RAW_DISTRIBUTOR_PRICE,
                                        "raw/dist-id"))
                .isInstanceOf(SaudaException.class)
                .hasMessageContaining("maximum allowed size");
    }
}
