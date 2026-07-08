package com.sauda.service.notification;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.enums.RoleCode;
import com.sauda.repository.AppUserRepository;
import com.sauda.repository.LotMatchRepository;
import com.sauda.service.notification.event.LotSentToDistributorEvent;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Observer of {@link LotSentToDistributorEvent}. Resolves the recipients once, builds a
 * channel-agnostic payload and fans it out to every registered {@link NotificationChannel}.
 */
@Slf4j
@Component
public class LotSentNotificationDispatcher {

    private static final String TITLE = "Новый подходящий лот";

    private final LotMatchRepository lotMatchRepository;
    private final AppUserRepository appUserRepository;
    private final List<NotificationChannel> channels;

    public LotSentNotificationDispatcher(
            LotMatchRepository lotMatchRepository,
            AppUserRepository appUserRepository,
            List<NotificationChannel> channels) {
        this.lotMatchRepository = lotMatchRepository;
        this.appUserRepository = appUserRepository;
        this.channels = channels;
    }

    @Transactional
    @EventListener
    public void onLotSent(LotSentToDistributorEvent event) {
        LotMatch match = lotMatchRepository.findById(event.lotMatchId()).orElse(null);
        if (match == null) {
            log.warn("Lot sent event for missing match: lotMatchId={}", event.lotMatchId());
            return;
        }

        List<AppUser> recipients =
                appUserRepository.findActiveByOrganizationAndRoleCode(
                        match.getDistributor().getId(), RoleCode.distributor_manager.name());
        if (recipients.isEmpty()) {
            log.info(
                    "No distributor managers to notify: distributorId={}",
                    match.getDistributor().getId());
            return;
        }

        NotificationPayload payload =
                new NotificationPayload(match, recipients, TITLE, buildMessage(match));
        channels.forEach(channel -> channel.deliver(payload));
    }

    private static String buildMessage(LotMatch match) {
        String lotTitle = match.getLot().getTitle();
        return "Для вашей компании найден новый потенциально подходящий лот: "
                + lotTitle
                + ". Требуется проверка условий и наличия.";
    }
}
