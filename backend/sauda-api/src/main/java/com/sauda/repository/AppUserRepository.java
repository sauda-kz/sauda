package com.sauda.repository;

import com.sauda.domain.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @Query(
            """
      SELECT DISTINCT u FROM AppUser u
      JOIN FETCH u.organization
      LEFT JOIN FETCH u.roles r
      LEFT JOIN FETCH r.permissions
      WHERE LOWER(u.email) = LOWER(:email) AND u.active = true
      """)
    Optional<AppUser> findWithOrganizationAndRolesByEmailAndActiveTrue(
            @Param("email") String email);

    @Query(
            """
      SELECT DISTINCT u FROM AppUser u
      JOIN FETCH u.organization
      LEFT JOIN FETCH u.roles r
      LEFT JOIN FETCH r.permissions
      WHERE u.id = :id AND u.active = true
      """)
    Optional<AppUser> findWithOrganizationAndRolesByIdAndActiveTrue(@Param("id") UUID id);
}
