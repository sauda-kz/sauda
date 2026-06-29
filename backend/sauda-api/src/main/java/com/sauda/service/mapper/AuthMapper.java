package com.sauda.service.mapper;

import com.sauda.dto.auth.MeResponse;
import com.sauda.security.principal.SaudaPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "roles", source = "roleCodes")
    MeResponse toMeResponse(SaudaPrincipal principal);
}
