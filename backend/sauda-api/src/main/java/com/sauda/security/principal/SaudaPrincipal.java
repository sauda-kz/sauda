package com.sauda.security.principal;

import com.sauda.domain.enums.OrganizationType;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record SaudaPrincipal(
        UUID id,
        String email,
        UUID organizationId,
        OrganizationType organizationType,
        Set<String> roleCodes,
        Collection<? extends GrantedAuthority> authorities)
        implements UserDetails {

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public static Collection<GrantedAuthority> toAuthorities(Set<String> permissionCodes) {
        return permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
