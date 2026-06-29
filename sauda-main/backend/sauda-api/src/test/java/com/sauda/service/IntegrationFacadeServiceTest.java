package com.sauda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sauda.integration.notification.NotificationService;
import com.sauda.integration.payment.PaymentProvider;
import com.sauda.integration.supplier.SupplierImportProvider;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationFacadeServiceTest {

    @Mock private NotificationService notificationService;

    @Mock private PaymentProvider paymentProvider;

    @Mock private SupplierImportProvider supplierImportProvider;

    @InjectMocks private IntegrationFacadeService integrationFacadeService;

    @Test
    void notifyOrderSubmittedDelegatesToNotificationService() {
        integrationFacadeService.notifyOrderSubmitted("+77001234567", "ORD-1");

        verify(notificationService).send("+77001234567", "Order ORD-1 submitted successfully");
    }

    @Test
    void processPaymentDelegatesToPaymentProvider() {
        when(paymentProvider.charge("ORD-1", new BigDecimal("100.00"), "KZT")).thenReturn("TX-123");

        String transactionId =
                integrationFacadeService.processPayment("ORD-1", new BigDecimal("100.00"));

        assertThat(transactionId).isEqualTo("TX-123");
    }

    @Test
    void syncSupplierCatalogDelegatesToImportProvider() {
        when(supplierImportProvider.importCatalog("SUP-1")).thenReturn(List.of("SKU-1"));

        List<String> skus = integrationFacadeService.syncSupplierCatalog("SUP-1");

        assertThat(skus).containsExactly("SKU-1");
    }
}
