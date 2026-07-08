package com.sauda.service.mapper;

import com.sauda.domain.entity.LotAttachment;
import com.sauda.dto.lot.LotAttachmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LotAttachmentMapper {

    @Mapping(source = "lot.id", target = "lotId")
    @Mapping(source = "uploadedBy.id", target = "uploadedById")
    LotAttachmentResponse toResponse(LotAttachment attachment);
}
