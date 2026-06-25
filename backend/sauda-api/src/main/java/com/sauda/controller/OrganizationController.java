package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.dto.organization.OrganizationResponse;
import com.sauda.service.OrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('org:read')")
    public OrganizationResponse getCurrentOrganization() {
        return organizationService.getCurrentOrganization();
    }
}
