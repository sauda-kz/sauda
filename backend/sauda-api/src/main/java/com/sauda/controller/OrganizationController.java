package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.dto.organization.OrganizationResponse;
import com.sauda.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Organizations", description = "Organization profile")
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(ApiConstants.API_V1 + "/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Operation(summary = "Get current user's organization")
    @GetMapping("/current")
    @PreAuthorize("hasAuthority('org:read')")
    public OrganizationResponse getCurrentOrganization() {
        return organizationService.getCurrentOrganization();
    }
}
