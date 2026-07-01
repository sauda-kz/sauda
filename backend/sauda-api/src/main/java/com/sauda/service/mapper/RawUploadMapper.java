package com.sauda.service.mapper;

import com.sauda.domain.entity.RawUpload;
import com.sauda.dto.rawupload.RawUploadResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RawUploadMapper {

    @Mapping(source = "distributor.id", target = "distributorId")
    @Mapping(source = "uploadedBy.id", target = "uploadedByUserId")
    RawUploadResponse toResponse(RawUpload upload);
}
