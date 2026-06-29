package com.sauda.service;

import com.sauda.dto.organization.OrganizationResponse;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.OrganizationRepository;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.OrganizationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationService(
            OrganizationRepository organizationRepository, OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        var organizationId = SecurityUtils.requirePrincipal().organizationId();
        return organizationRepository
                .findById(organizationId)
                .map(organizationMapper::toResponse)
                .orElseThrow(
                        () ->
                                new SaudaNotFoundException(
                                        "Organization not found: " + organizationId));
    }
}
