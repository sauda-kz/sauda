package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.dto.rawupload.RawUploadResponse;
import com.sauda.integration.storage.StoredObject;
import com.sauda.service.RawUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Raw Uploads", description = "Distributor raw price/stock file uploads")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(ApiConstants.API_V1)
public class RawUploadController {

    private final RawUploadService rawUploadService;

    public RawUploadController(RawUploadService rawUploadService) {
        this.rawUploadService = rawUploadService;
    }

    @Operation(summary = "Upload raw distributor file")
    @PostMapping(
            value = "/distributors/{distributorId}/raw-uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('import:run')")
    public ResponseEntity<RawUploadResponse> upload(
            @PathVariable UUID distributorId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rawUploadService.upload(distributorId, file));
    }

    @Operation(summary = "List raw uploads for distributor")
    @GetMapping("/distributors/{distributorId}/raw-uploads")
    @PreAuthorize("hasAuthority('import:read')")
    public Page<RawUploadResponse> listForDistributor(
            @PathVariable UUID distributorId, @PageableDefault(size = 20) Pageable pageable) {
        return rawUploadService.listForDistributor(distributorId, pageable);
    }

    @Operation(summary = "Get raw upload metadata")
    @GetMapping("/distributors/{distributorId}/raw-uploads/{uploadId}")
    @PreAuthorize("hasAuthority('import:read')")
    public RawUploadResponse getUpload(
            @PathVariable UUID distributorId, @PathVariable UUID uploadId) {
        return rawUploadService.getUpload(distributorId, uploadId);
    }

    @Operation(summary = "Download raw uploaded file")
    @GetMapping("/distributors/{distributorId}/raw-uploads/{uploadId}/download")
    @PreAuthorize("hasAuthority('import:read')")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID distributorId, @PathVariable UUID uploadId) {
        var download = rawUploadService.download(distributorId, uploadId);
        StoredObject storedObject = download.storedObject();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(storedObject.contentLength())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(new InputStreamResource(storedObject.content()));
    }
}
