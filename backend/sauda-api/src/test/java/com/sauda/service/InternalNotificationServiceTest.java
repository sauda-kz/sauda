package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.InternalNotification;
import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.enums.NotificationStatus;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.RoleCode;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.InternalNotificationRepository;
import com.sauda.service.mapper.InternalNotificationMapper;
import com.sauda.testsupport.SecurityTestFixtures;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class InternalNotificationServiceTest {

    @Mock private InternalNotificationRepository internalNotificationRepository;

    private final InternalNotificationMapper internalNotificationMapper =
            Mappers.getMapper(InternalNotificationMapper.class);

    private InternalNotificationService internalNotificationService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        internalNotificationService =
                new InternalNotificationService(
                        internalNotificationRepository, internalNotificationMapper);
        userId = UUID.randomUUID();
        SecurityTestFixtures.setPrincipal(
                userId,
                "manager@dist.kz",
                UUID.randomUUID(),
                OrganizationType.distributor,
                Set.of(RoleCode.distributor_manager.name()),
                "notification:read");
    }

    @AfterEach
    void tearDown() {
        SecurityTestFixtures.clearPrincipal();
    }

    @Test
    void listForCurrentUserFiltersByOwnUserId() {
        InternalNotification notification = buildNotification(UUID.randomUUID());
        when(internalNotificationRepository.findByUserIdOrderByCreatedAtDesc(
                        eq(userId), eq(Pageable.ofSize(20))))
                .thenReturn(new PageImpl<>(java.util.List.of(notification)));

        var page = internalNotificationService.listForCurrentUser(null, Pageable.ofSize(20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().title()).isEqualTo("Новый подходящий лот");
        assertThat(page.getContent().getFirst().lotTitle()).isEqualTo("SSD 1TB");
    }

    @Test
    void countUnreadDelegatesToRepository() {
        when(internalNotificationRepository.countByUserIdAndStatus(
                        userId, NotificationStatus.unread))
                .thenReturn(3L);

        assertThat(internalNotificationService.countUnread()).isEqualTo(3L);
    }

    @Test
    void markAsReadSetsStatusAndTimestamp() {
        UUID notificationId = UUID.randomUUID();
        InternalNotification notification = buildNotification(notificationId);
        when(internalNotificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.of(notification));
        when(internalNotificationRepository.save(notification)).thenReturn(notification);

        var response = internalNotificationService.markAsRead(notificationId);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.read);
        assertThat(notification.getReadAt()).isNotNull();
        assertThat(response.status()).isEqualTo(NotificationStatus.read);
    }

    @Test
    void markAsReadIsIdempotentWhenAlreadyRead() {
        UUID notificationId = UUID.randomUUID();
        InternalNotification notification = buildNotification(notificationId);
        notification.setStatus(NotificationStatus.read);
        notification.setReadAt(java.time.Instant.parse("2026-06-01T00:00:00Z"));
        when(internalNotificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.of(notification));

        internalNotificationService.markAsRead(notificationId);

        verify(internalNotificationRepository, never()).save(notification);
    }

    @Test
    void markAsReadThrowsWhenNotOwnedByUser() {
        UUID notificationId = UUID.randomUUID();
        when(internalNotificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalNotificationService.markAsRead(notificationId))
                .isInstanceOf(SaudaNotFoundException.class);
    }

    private static InternalNotification buildNotification(UUID id) {
        Lot lot = new Lot();
        lot.setId(UUID.randomUUID());
        lot.setTitle("SSD 1TB");
        LotMatch match = new LotMatch();
        match.setId(UUID.randomUUID());
        match.setLot(lot);

        InternalNotification notification = new InternalNotification();
        notification.setId(id);
        notification.setLotMatch(match);
        notification.setTitle("Новый подходящий лот");
        notification.setMessage("Для вашей компании найден новый потенциально подходящий лот.");
        notification.setStatus(NotificationStatus.unread);
        return notification;
    }
}
