package com.sauda.repository;

import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.OrganizationType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsByIdAndType(UUID id, OrganizationType type);
}
