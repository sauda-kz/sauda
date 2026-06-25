package com.sauda.security.jwt;

import com.sauda.config.JwtProperties;
import com.sauda.exception.SaudaUnauthorizedException;
import com.sauda.security.principal.SaudaPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public static final String CLAIM_TOKEN_TYPE = "type";
    public static final String CLAIM_ORG_ID = "orgId";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey =
                Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(SaudaPrincipal principal) {
        return createToken(principal, TOKEN_TYPE_ACCESS, jwtProperties.expirationMs());
    }

    public String createRefreshToken(SaudaPrincipal principal) {
        return createToken(principal, TOKEN_TYPE_REFRESH, jwtProperties.refreshExpirationMs());
    }

    public UUID parseUserId(String token, String expectedType) {
        Claims claims = parseClaims(token);
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedType.equals(tokenType)) {
            throw new SaudaUnauthorizedException("Invalid token type");
        }
        return UUID.fromString(claims.getSubject());
    }

    public long accessTokenExpiresInSeconds() {
        return jwtProperties.expirationMs() / 1000;
    }

    public long refreshTokenExpiresInSeconds() {
        return jwtProperties.refreshExpirationMs() / 1000;
    }

    private String createToken(SaudaPrincipal principal, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(principal.id().toString())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_ORG_ID, principal.organizationId().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new SaudaUnauthorizedException("Token expired");
        } catch (JwtException | IllegalArgumentException exception) {
            throw new SaudaUnauthorizedException("Invalid token");
        }
    }
}
