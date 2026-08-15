package com.bautruc.ecommerce.payment.application;
import java.time.Instant;
import com.bautruc.ecommerce.payment.domain.PaymentStatus;
public interface PaymentResultPort {
    void processSuccess(VerifiedPayOSEvent event, Instant webhookReceivedAt);
    void processTerminalFailure(Long paymentId, PaymentStatus actualStatus);
}

