package com.sauda.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.InternalNotification;
import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.enums.NotificationStatus;
import com.sauda.repository.InternalNotificationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InAppNotificationChannelTest {

    @Mock private InternalNotificationRepository internalNotificationRepository;

    @Test
    void deliverPersistsOneUnreadNotificationPerRecipient() {
        InAppNotificationChannel channel =
                new InAppNotificationChannel(internalNotificationRepository);

        Lot lot = new Lot();
        lot.setId(UUID.randomUUID());
        lot.setTitle("SSD 1TB");
        LotMatch match = new LotMatch();
        match.setId(UUID.randomUUID());
        match.setLot(lot);

        AppUser first = new AppUser();
        first.setId(UUID.randomUUID());
        AppUser second = new AppUser();
        second.setId(UUID.randomUUID());

        channel.deliver(
                new NotificationPayload(match, List.of(first, second), "Title", "Message"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InternalNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(internalNotificationRepository).saveAll(captor.capture());

        List<InternalNotification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved)
                .allSatisfy(
                        notification -> {
                            assertThat(notification.getStatus())
                                    .isEqualTo(NotificationStatus.unread);
                            assertThat(notification.getLotMatch()).isEqualTo(match);
                            assertThat(notification.getTitle()).isEqualTo("Title");
                        });
        assertThat(saved).extracting(InternalNotification::getUser).containsExactly(first, second);
    }
}
