package com.sauda.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.integration.notification.MockNotificationService;
import com.sauda.integration.payment.FakePaymentProvider;
import com.sauda.integration.supplier.MockSupplierImportProvider;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class IntegrationImplementationsTest {

    @Test
    void mockNotificationServiceDoesNotThrow() {
        MockNotificationService service = new MockNotificationService();
        service.send("+77001234567", "Hello");
    }

    @Test
    void fakePaymentProviderReturnsTransactionId() {
        FakePaymentProvider provider = new FakePaymentProvider();

        String transactionId = provider.charge("ORD-1", new BigDecimal("50.00"), "KZT");

        assertThat(transactionId).startsWith("FAKE-");
    }

    @Test
    void mockSupplierImportProviderReturnsSkus() {
        MockSupplierImportProvider provider = new MockSupplierImportProvider();

        assertThat(provider.importCatalog("SUP-1")).hasSize(3);
    }
}
