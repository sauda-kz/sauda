package com.sauda.repository;

import com.sauda.domain.entity.ParsedRow;
import com.sauda.domain.enums.ParsedRowStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParsedRowRepository extends JpaRepository<ParsedRow, UUID> {

    Page<ParsedRow> findByImportRunId(UUID importRunId, Pageable pageable);

    Page<ParsedRow> findByImportRunIdAndStatus(
            UUID importRunId, ParsedRowStatus status, Pageable pageable);

    List<ParsedRow> findByImportRunIdAndStatusIn(
            UUID importRunId, Collection<ParsedRowStatus> statuses);

    Optional<ParsedRow> findByIdAndImportRunId(UUID id, UUID importRunId);
}
