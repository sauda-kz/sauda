package com.sauda.integration.payment;

import java.math.BigDecimal;

public interface PaymentProvider {

    String charge(String orderId, BigDecimal amount, String currency);
}
