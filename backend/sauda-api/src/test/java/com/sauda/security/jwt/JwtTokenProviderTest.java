package com.sauda.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sauda.config.JwtProperties;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.exception.SaudaUnauthorizedException;
import com.sauda.security.principal.SaudaPrincipal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private SaudaPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtTokenProvider =
                new JwtTokenProvider(
                        new JwtProperties(
                                "test-jwt-secret-key-min-32-chars!!", 3600000, 604800000));
        principal =
                new SaudaPrincipal(
                        UUID.randomUUID(),
                        "buyer-a@shop.kz",
                        UUID.randomUUID(),
                        OrganizationType.buyer,
                        Set.of("buyer"),
                        Set.of(new SimpleGrantedAuthority("org:read")));
    }

    @Test
    void createAndParseAccessToken() {
        String token = jwtTokenProvider.createAccessToken(principal);

        UUID userId = jwtTokenProvider.parseUserId(token, JwtTokenProvider.TOKEN_TYPE_ACCESS);

        assertThat(userId).isEqualTo(principal.id());
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refreshToken = jwtTokenProvider.createRefreshToken(principal);

        assertThatThrownBy(
                        () ->
                                jwtTokenProvider.parseUserId(
                                        refreshToken, JwtTokenProvider.TOKEN_TYPE_ACCESS))
                .isInstanceOf(SaudaUnauthorizedException.class)
                .hasMessage("Invalid token type");
    }

    @Test
    void invalidTokenIsRejected() {
        assertThatThrownBy(
                        () ->
                                jwtTokenProvider.parseUserId(
                                        "invalid.token.value", JwtTokenProvider.TOKEN_TYPE_ACCESS))
                .isInstanceOf(SaudaUnauthorizedException.class);
    }
}
