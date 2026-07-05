package com.sauda.repository;

import com.sauda.domain.entity.ImportRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRunRepository extends JpaRepository<ImportRun, UUID> {

    Page<ImportRun> findByDistributorIdOrderByCreatedAtDesc(UUID distributorId, Pageable pageable);

    Optional<ImportRun> findByIdAndDistributorId(UUID id, UUID distributorId);

    @EntityGraph(attributePaths = {"distributor", "rawUpload"})
    Optional<ImportRun> findWithDistributorById(UUID id);
}
