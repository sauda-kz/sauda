package com.sauda.integration.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class WhatsAppNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    private final String apiUrl;

    public WhatsAppNotificationService(
            @Value("${sauda.integrations.whatsapp.api-url:}") String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @Override
    public void send(String recipient, String message) {
        log.info("[WHATSAPP] Sending notification to {} via {}", recipient, apiUrl);
        // Future: HTTP call to WhatsApp Business API
    }
}
