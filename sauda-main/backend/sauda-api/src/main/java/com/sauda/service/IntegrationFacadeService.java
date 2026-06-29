package com.sauda.service;

import com.sauda.integration.notification.NotificationService;
import com.sauda.integration.payment.PaymentProvider;
import com.sauda.integration.supplier.SupplierImportProvider;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IntegrationFacadeService {

    private final NotificationService notificationService;
    private final PaymentProvider paymentProvider;
    private final SupplierImportProvider supplierImportProvider;

    public IntegrationFacadeService(
            NotificationService notificationService,
            PaymentProvider paymentProvider,
            SupplierImportProvider supplierImportProvider) {
        this.notificationService = notificationService;
        this.paymentProvider = paymentProvider;
        this.supplierImportProvider = supplierImportProvider;
    }

    public void notifyOrderSubmitted(String recipient, String orderId) {
        notificationService.send(recipient, "Order " + orderId + " submitted successfully");
    }

    public String processPayment(String orderId, BigDecimal amount) {
        return paymentProvider.charge(orderId, amount, "KZT");
    }

    public List<String> syncSupplierCatalog(String supplierCode) {
        return supplierImportProvider.importCatalog(supplierCode);
    }
}
