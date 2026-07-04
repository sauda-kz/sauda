package com.sauda.service;

import com.sauda.domain.entity.InternalNotification;
import com.sauda.domain.enums.NotificationStatus;
import com.sauda.dto.notification.InternalNotificationResponse;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.InternalNotificationRepository;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.InternalNotificationMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side API for a user's own in-app notifications. Creation is handled by notification
 * channels.
 */
@Slf4j
@Service
public class InternalNotificationService {

    private final InternalNotificationRepository internalNotificationRepository;
    private final InternalNotificationMapper internalNotificationMapper;

    public InternalNotificationService(
            InternalNotificationRepository internalNotificationRepository,
            InternalNotificationMapper internalNotificationMapper) {
        this.internalNotificationRepository = internalNotificationRepository;
        this.internalNotificationMapper = internalNotificationMapper;
    }

    @Transactional(readOnly = true)
    public Page<InternalNotificationResponse> listForCurrentUser(
            NotificationStatus status, Pageable pageable) {
        UUID userId = SecurityUtils.requirePrincipal().id();
        Page<InternalNotification> page =
                status != null
                        ? internalNotificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                                userId, status, pageable)
                        : internalNotificationRepository.findByUserIdOrderByCreatedAtDesc(
                                userId, pageable);
        return page.map(internalNotificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        UUID userId = SecurityUtils.requirePrincipal().id();
        return internalNotificationRepository.countByUserIdAndStatus(
                userId, NotificationStatus.unread);
    }

    @Transactional
    public InternalNotificationResponse markAsRead(UUID notificationId) {
        UUID userId = SecurityUtils.requirePrincipal().id();
        InternalNotification notification =
                internalNotificationRepository
                        .findByIdAndUserId(notificationId, userId)
                        .orElseThrow(
                                () ->
                                        new SaudaNotFoundException(
                                                "Notification not found: " + notificationId));

        if (notification.getStatus() != NotificationStatus.read) {
            notification.setStatus(NotificationStatus.read);
            notification.setReadAt(Instant.now());
            internalNotificationRepository.save(notification);
        }
        return internalNotificationMapper.toResponse(notification);
    }
}
