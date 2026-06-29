package com.sauda.integration.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev", "test"})
public class FakePaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentProvider.class);

    @Override
    public String charge(String orderId, BigDecimal amount, String currency) {
        String transactionId = "FAKE-" + UUID.randomUUID();
        log.info(
                "[FAKE] Payment charged for order {} amount {} {} -> {}",
                orderId,
                amount,
                currency,
                transactionId);
        return transactionId;
    }
}
