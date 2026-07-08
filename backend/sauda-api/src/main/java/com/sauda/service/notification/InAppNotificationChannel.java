package com.sauda.service.notification;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.InternalNotification;
import com.sauda.domain.enums.NotificationStatus;
import com.sauda.repository.InternalNotificationRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Persists an in-app {@link InternalNotification} row for each recipient. */
@Slf4j
@Component
public class InAppNotificationChannel implements NotificationChannel {

    private final InternalNotificationRepository internalNotificationRepository;

    public InAppNotificationChannel(InternalNotificationRepository internalNotificationRepository) {
        this.internalNotificationRepository = internalNotificationRepository;
    }

    @Override
    public void deliver(NotificationPayload payload) {
        List<InternalNotification> notifications =
                payload.recipients().stream().map(recipient -> build(payload, recipient)).toList();
        internalNotificationRepository.saveAll(notifications);
        log.info(
                "In-app notifications created: lotMatchId={}, recipients={}",
                payload.lotMatch().getId(),
                notifications.size());
    }

    private static InternalNotification build(NotificationPayload payload, AppUser recipient) {
        InternalNotification notification = new InternalNotification();
        notification.setUser(recipient);
        notification.setLotMatch(payload.lotMatch());
        notification.setTitle(payload.title());
        notification.setMessage(payload.message());
        notification.setStatus(NotificationStatus.unread);
        return notification;
    }
}
