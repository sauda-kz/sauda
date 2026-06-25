package com.sauda.testsupport;

import com.sauda.domain.enums.OrganizationType;
import com.sauda.security.principal.SaudaPrincipal;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityTestFixtures {

    private SecurityTestFixtures() {}

    public static void setPrincipal(
            UUID userId,
            String email,
            UUID organizationId,
            OrganizationType organizationType,
            Set<String> roleCodes,
            String... authorities) {
        var grantedAuthorities =
                java.util.Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                        .toList();
        SaudaPrincipal principal =
                new SaudaPrincipal(
                        userId,
                        email,
                        organizationId,
                        organizationType,
                        roleCodes,
                        grantedAuthorities);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
    }

    public static void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }
}
