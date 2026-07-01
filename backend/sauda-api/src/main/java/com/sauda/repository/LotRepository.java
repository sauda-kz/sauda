package com.sauda.repository;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.enums.LotStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LotRepository extends JpaRepository<Lot, UUID>, JpaSpecificationExecutor<Lot> {

    Page<Lot> findByStatus(LotStatus status, Pageable pageable);
}
