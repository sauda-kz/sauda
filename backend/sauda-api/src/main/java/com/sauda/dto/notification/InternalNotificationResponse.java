package com.sauda.dto.notification;

import com.sauda.domain.enums.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record InternalNotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationStatus status,
        UUID lotMatchId,
        UUID lotId,
        String lotTitle,
        Instant createdAt,
        Instant readAt) {}
