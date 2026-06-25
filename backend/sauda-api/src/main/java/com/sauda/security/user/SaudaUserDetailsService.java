package com.sauda.security.user;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Permission;
import com.sauda.domain.entity.Role;
import com.sauda.exception.SaudaUnauthorizedException;
import com.sauda.repository.AppUserRepository;
import com.sauda.security.principal.SaudaPrincipal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaudaUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public SaudaUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user =
                appUserRepository
                        .findWithOrganizationAndRolesByEmailAndActiveTrue(username)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("User not found: " + username));
        return buildPrincipal(user);
    }

    @Transactional(readOnly = true)
    public SaudaPrincipal loadPrincipalById(UUID userId) {
        AppUser user =
                appUserRepository
                        .findWithOrganizationAndRolesByIdAndActiveTrue(userId)
                        .orElseThrow(
                                () -> new SaudaUnauthorizedException("User not found or inactive"));
        return buildPrincipal(user);
    }

    public SaudaPrincipal buildPrincipal(AppUser user) {
        Set<String> roleCodes =
                user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        Set<String> permissionCodes =
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getCode)
                        .collect(Collectors.toSet());

        return new SaudaPrincipal(
                user.getId(),
                user.getEmail(),
                user.getOrganization().getId(),
                user.getOrganization().getType(),
                roleCodes,
                SaudaPrincipal.toAuthorities(permissionCodes));
    }
}
