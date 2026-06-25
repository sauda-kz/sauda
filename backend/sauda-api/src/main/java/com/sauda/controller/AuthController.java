package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.dto.auth.LoginRequest;
import com.sauda.dto.auth.LoginResponse;
import com.sauda.dto.auth.MeResponse;
import com.sauda.dto.auth.RefreshTokenRequest;
import com.sauda.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication and session management")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Authenticate with email and password")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Get current authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public MeResponse me() {
        return authService.getCurrentUser();
    }
}
