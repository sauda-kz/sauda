package com.sauda.repository;

import com.sauda.domain.entity.RawUpload;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawUploadRepository extends JpaRepository<RawUpload, UUID> {

    Page<RawUpload> findByDistributorIdOrderByCreatedAtDesc(UUID distributorId, Pageable pageable);

    Optional<RawUpload> findByIdAndDistributorId(UUID id, UUID distributorId);
}
