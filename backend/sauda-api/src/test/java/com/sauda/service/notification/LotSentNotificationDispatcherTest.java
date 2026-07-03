package com.sauda.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.Lot;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.entity.Organization;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.domain.enums.RoleCode;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.LotMatchRepository;
import com.sauda.service.notification.event.LotSentToDistributorEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotSentNotificationDispatcherTest {

    @Mock private LotMatchRepository lotMatchRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private NotificationChannel channelA;
    @Mock private NotificationChannel channelB;

    @Test
    void dispatchesPayloadToAllChannelsForDistributorManagers() {
        UUID matchId = UUID.randomUUID();
        UUID distributorId = UUID.randomUUID();
        LotMatch match = buildMatch(matchId, distributorId);
        AppUser manager = new AppUser();
        manager.setId(UUID.randomUUID());

        when(lotMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(appUserRepository.findActiveByOrganizationAndRoleCode(
                        eq(distributorId), eq(RoleCode.distributor_manager.name())))
                .thenReturn(List.of(manager));

        dispatcher().onLotSent(new LotSentToDistributorEvent(matchId));

        ArgumentCaptor<NotificationPayload> captor =
                ArgumentCaptor.forClass(NotificationPayload.class);
        verify(channelA).deliver(captor.capture());
        verify(channelB).deliver(captor.getValue());

        NotificationPayload payload = captor.getValue();
        assertThat(payload.recipients()).containsExactly(manager);
        assertThat(payload.title()).isEqualTo("Новый подходящий лот");
        assertThat(payload.message()).contains("SSD 1TB");
    }

    @Test
    void skipsDeliveryWhenNoRecipients() {
        UUID matchId = UUID.randomUUID();
        UUID distributorId = UUID.randomUUID();
        when(lotMatchRepository.findById(matchId))
                .thenReturn(Optional.of(buildMatch(matchId, distributorId)));
        when(appUserRepository.findActiveByOrganizationAndRoleCode(
                        eq(distributorId), eq(RoleCode.distributor_manager.name())))
                .thenReturn(List.of());

        dispatcher().onLotSent(new LotSentToDistributorEvent(matchId));

        verify(channelA, never()).deliver(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresMissingMatch() {
        UUID matchId = UUID.randomUUID();
        when(lotMatchRepository.findById(matchId)).thenReturn(Optional.empty());

        dispatcher().onLotSent(new LotSentToDistributorEvent(matchId));

        verify(channelA, never()).deliver(org.mockito.ArgumentMatchers.any());
    }

    private LotSentNotificationDispatcher dispatcher() {
        return new LotSentNotificationDispatcher(
                lotMatchRepository, appUserRepository, List.of(channelA, channelB));
    }

    private static LotMatch buildMatch(UUID matchId, UUID distributorId) {
        Organization distributor = new Organization();
        distributor.setId(distributorId);
        distributor.setType(OrganizationType.distributor);
        Lot lot = new Lot();
        lot.setId(UUID.randomUUID());
        lot.setTitle("SSD 1TB");
        LotMatch match = new LotMatch();
        match.setId(matchId);
        match.setLot(lot);
        match.setDistributor(distributor);
        return match;
    }
}
