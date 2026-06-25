package com.sauda.service.mapper;

import com.sauda.domain.entity.Organization;
import com.sauda.dto.organization.OrganizationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationResponse toResponse(Organization organization);
}
