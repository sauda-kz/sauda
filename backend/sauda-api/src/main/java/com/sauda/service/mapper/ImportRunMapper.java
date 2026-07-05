package com.sauda.service.mapper;

import com.sauda.domain.entity.ImportRun;
import com.sauda.dto.imports.ImportRunResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ImportRunMapper {

    @Mapping(source = "distributor.id", target = "distributorId")
    @Mapping(source = "rawUpload.id", target = "rawUploadId")
    @Mapping(source = "rawUpload.originalFilename", target = "originalFilename")
    ImportRunResponse toResponse(ImportRun importRun);
}
