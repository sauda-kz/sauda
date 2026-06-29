package com.sauda.repository;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.enums.LotStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotRepository extends JpaRepository<Lot, UUID> {

    Page<Lot> findByStatus(LotStatus status, Pageable pageable);
}
