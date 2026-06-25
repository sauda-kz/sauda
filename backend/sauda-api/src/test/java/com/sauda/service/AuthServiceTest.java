package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.entity.Permission;
import com.sauda.domain.entity.Role;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.dto.auth.LoginRequest;
import com.sauda.dto.auth.LoginResponse;
import com.sauda.dto.auth.MeResponse;
import com.sauda.dto.auth.RefreshTokenRequest;
import com.sauda.exception.SaudaUnauthorizedException;
import com.sauda.repository.AppUserRepository;
import com.sauda.security.jwt.JwtTokenProvider;
import com.sauda.security.principal.SaudaPrincipal;
import com.sauda.security.user.SaudaUserDetailsService;
import com.sauda.service.mapper.AuthMapper;
import com.sauda.testsupport.SecurityTestFixtures;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private SaudaUserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthMapper authMapper = Mappers.getMapper(AuthMapper.class);
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    private AppUser user;
    private SaudaPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtTokenProvider =
                new JwtTokenProvider(
                        new com.sauda.config.JwtProperties(
                                "test-jwt-secret-key-min-32-chars!!", 3600000, 604800000));
        authService =
                new AuthService(
                        appUserRepository,
                        userDetailsService,
                        jwtTokenProvider,
                        passwordEncoder,
                        authMapper);

        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setType(OrganizationType.buyer);
        organization.setName("Buyer Shop A");

        Role role = new Role();
        role.setCode("buyer");
        Permission permission = new Permission();
        permission.setCode("org:read");
        role.setPermissions(Set.of(permission));

        user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("buyer-a@shop.kz");
        user.setPasswordHash(passwordEncoder.encode("Sauda123!"));
        user.setOrganization(organization);
        user.setRoles(Set.of(role));
        user.setActive(true);

        principal =
                new SaudaPrincipal(
                        user.getId(),
                        user.getEmail(),
                        organization.getId(),
                        OrganizationType.buyer,
                        Set.of("buyer"),
                        SaudaPrincipal.toAuthorities(Set.of("org:read")));
    }

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void loginReturnsTokensForValidCredentials() {
        when(appUserRepository.findWithOrganizationAndRolesByEmailAndActiveTrue("buyer-a@shop.kz"))
                .thenReturn(Optional.of(user));
        when(userDetailsService.buildPrincipal(user)).thenReturn(principal);

        LoginResponse response =
                authService.login(new LoginRequest("buyer-a@shop.kz", "Sauda123!"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isPositive();
    }

    @Test
    void loginFailsForInvalidPassword() {
        when(appUserRepository.findWithOrganizationAndRolesByEmailAndActiveTrue("buyer-a@shop.kz"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                authService.login(
                                        new LoginRequest("buyer-a@shop.kz", "wrong-password")))
                .isInstanceOf(SaudaUnauthorizedException.class);
    }

    @Test
    void loginFailsForUnknownEmail() {
        when(appUserRepository.findWithOrganizationAndRolesByEmailAndActiveTrue(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> authService.login(new LoginRequest("missing@shop.kz", "Sauda123!")))
                .isInstanceOf(SaudaUnauthorizedException.class);
    }

    @Test
    void refreshReturnsNewTokens() {
        when(userDetailsService.loadPrincipalById(principal.id())).thenReturn(principal);
        String refreshToken = jwtTokenProvider.createRefreshToken(principal);

        LoginResponse response = authService.refresh(new RefreshTokenRequest(refreshToken));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        verify(userDetailsService).loadPrincipalById(principal.id());
    }

    @Test
    void getCurrentUserReturnsPrincipalData() {
        SecurityTestFixtures.setPrincipal(
                principal.id(),
                principal.email(),
                principal.organizationId(),
                principal.organizationType(),
                principal.roleCodes(),
                "org:read");

        MeResponse response = authService.getCurrentUser();

        assertThat(response.userId()).isEqualTo(principal.id());
        assertThat(response.organizationId()).isEqualTo(principal.organizationId());
        assertThat(response.roles()).containsExactly("buyer");
    }
}
