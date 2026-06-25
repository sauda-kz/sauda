package com.sauda.service;

import com.sauda.dto.auth.LoginRequest;
import com.sauda.dto.auth.LoginResponse;
import com.sauda.dto.auth.MeResponse;
import com.sauda.dto.auth.RefreshTokenRequest;
import com.sauda.exception.SaudaUnauthorizedException;
import com.sauda.repository.AppUserRepository;
import com.sauda.security.jwt.JwtTokenProvider;
import com.sauda.security.principal.SaudaPrincipal;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.security.user.SaudaUserDetailsService;
import com.sauda.service.mapper.AuthMapper;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final SaudaUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;

    public AuthService(
            AppUserRepository appUserRepository,
            SaudaUserDetailsService userDetailsService,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            AuthMapper authMapper) {
        this.appUserRepository = appUserRepository;
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.authMapper = authMapper;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        var user =
                appUserRepository
                        .findWithOrganizationAndRolesByEmailAndActiveTrue(request.email())
                        .orElseThrow(
                                () -> {
                                    log.warn("Failed login attempt for email={}", request.email());
                                    return new SaudaUnauthorizedException(
                                            "Invalid email or password");
                                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email={}", request.email());
            throw new SaudaUnauthorizedException("Invalid email or password");
        }

        SaudaPrincipal principal = userDetailsService.buildPrincipal(user);
        return buildTokenResponse(principal);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshTokenRequest request) {
        UUID userId =
                jwtTokenProvider.parseUserId(
                        request.refreshToken(), JwtTokenProvider.TOKEN_TYPE_REFRESH);
        SaudaPrincipal principal = userDetailsService.loadPrincipalById(userId);
        log.info("Refreshed tokens for userId={}", principal.id());
        return buildTokenResponse(principal);
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser() {
        return authMapper.toMeResponse(SecurityUtils.requirePrincipal());
    }

    private LoginResponse buildTokenResponse(SaudaPrincipal principal) {
        log.info(
                "User logged in: userId={}, organizationId={}, roles={}",
                principal.id(),
                principal.organizationId(),
                principal.roleCodes());
        return new LoginResponse(
                jwtTokenProvider.createAccessToken(principal),
                jwtTokenProvider.createRefreshToken(principal),
                "Bearer",
                jwtTokenProvider.accessTokenExpiresInSeconds(),
                jwtTokenProvider.refreshTokenExpiresInSeconds());
    }
}
