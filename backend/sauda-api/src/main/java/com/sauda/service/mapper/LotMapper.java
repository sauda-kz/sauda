package com.sauda.service.mapper;

import com.sauda.domain.entity.Lot;
import com.sauda.dto.lot.CreateLotRequest;
import com.sauda.dto.lot.LotResponse;
import com.sauda.dto.lot.UpdateLotRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LotMapper {

    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(target = "dataQualityStatus", ignore = true)
    @Mapping(target = "missingKeyFields", ignore = true)
    @Mapping(target = "matchCount", constant = "0L")
    LotResponse toResponse(Lot lot);

    Lot toEntity(CreateLotRequest request);

    void updateEntity(@MappingTarget Lot lot, UpdateLotRequest request);
}
