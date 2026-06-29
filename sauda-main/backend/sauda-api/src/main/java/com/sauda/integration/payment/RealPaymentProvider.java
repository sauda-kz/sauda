package com.sauda.integration.payment;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class RealPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(RealPaymentProvider.class);

    private final String providerUrl;

    public RealPaymentProvider(
            @Value("${sauda.integrations.payment.provider-url:}") String providerUrl) {
        this.providerUrl = providerUrl;
    }

    @Override
    public String charge(String orderId, BigDecimal amount, String currency) {
        log.info(
                "[PAYMENT] Charging order {} amount {} {} via {}",
                orderId,
                amount,
                currency,
                providerUrl);
        // Future: HTTP call to payment gateway
        return "PAY-" + orderId;
    }
}
