package com.sauda.repository;

import com.sauda.domain.entity.LotAttachment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotAttachmentRepository extends JpaRepository<LotAttachment, UUID> {

    List<LotAttachment> findByLotIdOrderByCreatedAtDesc(UUID lotId);

    Optional<LotAttachment> findByIdAndLotId(UUID id, UUID lotId);
}
