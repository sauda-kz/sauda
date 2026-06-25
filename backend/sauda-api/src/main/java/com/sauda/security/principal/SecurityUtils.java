package com.sauda.security.principal;

import com.sauda.exception.SaudaUnauthorizedException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<SaudaPrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof SaudaPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static SaudaPrincipal requirePrincipal() {
        return getCurrentPrincipal()
                .orElseThrow(() -> new SaudaUnauthorizedException("Authentication required"));
    }
}
