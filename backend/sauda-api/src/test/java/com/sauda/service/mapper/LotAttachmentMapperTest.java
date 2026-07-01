package com.sauda.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotAttachment;
import com.sauda.domain.enums.LotStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class LotAttachmentMapperTest {

    private final LotAttachmentMapper lotAttachmentMapper =
            Mappers.getMapper(LotAttachmentMapper.class);

    @Test
    void mapsAttachmentToResponse() {
        UUID lotId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Lot lot = new Lot();
        lot.setId(lotId);
        lot.setStatus(LotStatus.active);

        AppUser uploader = new AppUser();
        uploader.setId(userId);

        LotAttachment attachment = new LotAttachment();
        attachment.setId(UUID.randomUUID());
        attachment.setLot(lot);
        attachment.setUploadedBy(uploader);
        attachment.setOriginalFilename("spec.pdf");
        attachment.setStoragePath("lots/" + lotId + "/file.pdf");
        attachment.setFileSize(100);
        attachment.setMimeType("application/pdf");
        attachment.setChecksum("abc");

        var response = lotAttachmentMapper.toResponse(attachment);

        assertThat(response.lotId()).isEqualTo(lotId);
        assertThat(response.uploadedById()).isEqualTo(userId);
        assertThat(response.originalFilename()).isEqualTo("spec.pdf");
    }
}
