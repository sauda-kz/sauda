package com.sauda.service;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotAttachment;
import com.sauda.dto.lot.LotAttachmentDownload;
import com.sauda.dto.lot.LotAttachmentResponse;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.integration.storage.upload.FileUploadPolicies;
import com.sauda.integration.storage.upload.PreparedUpload;
import com.sauda.integration.storage.upload.StoredFileUploadService;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.LotAttachmentRepository;
import com.sauda.repository.LotRepository;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.LotAttachmentMapper;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class LotAttachmentService {

    private final LotRepository lotRepository;
    private final LotAttachmentRepository lotAttachmentRepository;
    private final AppUserRepository appUserRepository;
    private final StoredFileUploadService storedFileUploadService;
    private final LotAttachmentMapper lotAttachmentMapper;

    public LotAttachmentService(
            LotRepository lotRepository,
            LotAttachmentRepository lotAttachmentRepository,
            AppUserRepository appUserRepository,
            StoredFileUploadService storedFileUploadService,
            LotAttachmentMapper lotAttachmentMapper) {
        this.lotRepository = lotRepository;
        this.lotAttachmentRepository = lotAttachmentRepository;
        this.appUserRepository = appUserRepository;
        this.storedFileUploadService = storedFileUploadService;
        this.lotAttachmentMapper = lotAttachmentMapper;
    }

    @Transactional
    public LotAttachmentResponse upload(UUID lotId, MultipartFile file) {
        Lot lot = findLotOrThrow(lotId);
        PreparedUpload prepared =
                storedFileUploadService.prepare(
                        file, FileUploadPolicies.LOT_ATTACHMENT, "lots/" + lotId);
        storedFileUploadService.store(prepared);

        UUID userId = SecurityUtils.requirePrincipal().id();
        AppUser uploader = appUserRepository.getReferenceById(userId);

        LotAttachment attachment = new LotAttachment();
        attachment.setLot(lot);
        attachment.setUploadedBy(uploader);
        attachment.setOriginalFilename(prepared.originalFilename());
        attachment.setStoragePath(prepared.storagePath());
        attachment.setFileSize(prepared.contentLength());
        attachment.setMimeType(prepared.mimeType());
        attachment.setChecksum(prepared.checksumSha256());

        LotAttachment saved = lotAttachmentRepository.save(attachment);
        log.info(
                "Lot attachment uploaded: lotId={}, attachmentId={}, size={}",
                lotId,
                saved.getId(),
                prepared.contentLength());
        return lotAttachmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LotAttachmentResponse> listByLot(UUID lotId) {
        findLotOrThrow(lotId);
        return lotAttachmentRepository.findByLotIdOrderByCreatedAtDesc(lotId).stream()
                .map(lotAttachmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LotAttachmentDownload download(UUID lotId, UUID attachmentId) {
        LotAttachment attachment = findAttachmentOrThrow(lotId, attachmentId);
        return new LotAttachmentDownload(
                attachment.getOriginalFilename(),
                attachment.getMimeType(),
                storedFileUploadService.fetch(attachment.getStoragePath()));
    }

    @Transactional
    public void delete(UUID lotId, UUID attachmentId) {
        LotAttachment attachment = findAttachmentOrThrow(lotId, attachmentId);
        storedFileUploadService.delete(attachment.getStoragePath());
        lotAttachmentRepository.delete(attachment);
        log.info("Lot attachment deleted: lotId={}, attachmentId={}", lotId, attachmentId);
    }

    private Lot findLotOrThrow(UUID lotId) {
        return lotRepository
                .findById(lotId)
                .orElseThrow(() -> new SaudaNotFoundException("Lot not found: " + lotId));
    }

    private LotAttachment findAttachmentOrThrow(UUID lotId, UUID attachmentId) {
        return lotAttachmentRepository
                .findByIdAndLotId(attachmentId, lotId)
                .orElseThrow(
                        () ->
                                new SaudaNotFoundException(
                                        "Lot attachment not found: " + attachmentId));
    }
}
