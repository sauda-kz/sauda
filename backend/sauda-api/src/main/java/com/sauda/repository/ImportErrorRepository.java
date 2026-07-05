package com.sauda.repository;

import com.sauda.domain.entity.ImportError;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportErrorRepository extends JpaRepository<ImportError, UUID> {}
