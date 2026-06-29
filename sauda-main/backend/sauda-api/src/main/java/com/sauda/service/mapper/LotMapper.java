package com.sauda.service.mapper;

import com.sauda.domain.entity.Lot;
import com.sauda.dto.lot.CreateLotRequest;
import com.sauda.dto.lot.LotResponse;
import com.sauda.dto.lot.UpdateLotRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LotMapper {

    LotResponse toResponse(Lot lot);

    Lot toEntity(CreateLotRequest request);

    void updateEntity(@MappingTarget Lot lot, UpdateLotRequest request);
}
