package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.dto.lot.LotAttachmentResponse;
import com.sauda.integration.storage.StoredObject;
import com.sauda.service.LotAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Lot Attachments", description = "Procurement documents attached to lots")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(ApiConstants.API_V1 + "/lots/{lotId}/attachments")
public class LotAttachmentController {

    private final LotAttachmentService lotAttachmentService;

    public LotAttachmentController(LotAttachmentService lotAttachmentService) {
        this.lotAttachmentService = lotAttachmentService;
    }

    @Operation(summary = "Upload document to lot")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('lot:manage')")
    public ResponseEntity<LotAttachmentResponse> upload(
            @PathVariable UUID lotId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lotAttachmentService.upload(lotId, file));
    }

    @Operation(summary = "List lot document attachments")
    @GetMapping
    @PreAuthorize("hasAuthority('lot:read')")
    public List<LotAttachmentResponse> list(@PathVariable UUID lotId) {
        return lotAttachmentService.listByLot(lotId);
    }

    @Operation(summary = "Download lot document attachment")
    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("hasAuthority('lot:read')")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID lotId, @PathVariable UUID attachmentId) {
        var download = lotAttachmentService.download(lotId, attachmentId);
        StoredObject storedObject = download.storedObject();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(storedObject.contentLength())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(new InputStreamResource(storedObject.content()));
    }

    @Operation(summary = "Delete lot document attachment")
    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('lot:manage')")
    public void delete(@PathVariable UUID lotId, @PathVariable UUID attachmentId) {
        lotAttachmentService.delete(lotId, attachmentId);
    }
}
