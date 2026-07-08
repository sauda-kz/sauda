package com.sauda.repository;

import com.sauda.domain.entity.InternalNotification;
import com.sauda.domain.enums.NotificationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternalNotificationRepository extends JpaRepository<InternalNotification, UUID> {

    @EntityGraph(attributePaths = {"lotMatch", "lotMatch.lot"})
    Page<InternalNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"lotMatch", "lotMatch.lot"})
    Page<InternalNotification> findByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, NotificationStatus status, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

    @EntityGraph(attributePaths = {"lotMatch", "lotMatch.lot"})
    Optional<InternalNotification> findByIdAndUserId(UUID id, UUID userId);
}
