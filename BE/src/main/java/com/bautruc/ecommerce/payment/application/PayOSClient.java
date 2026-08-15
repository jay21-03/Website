package com.bautruc.ecommerce.payment.application;
public interface PayOSClient {
    PayOSPaymentResult createPaymentRequest(PayOSCreatePaymentCommand command);
    java.util.Optional<PayOSPaymentResult> getPaymentRequest(Long orderId);
    void cancelPaymentRequest(String id,String reason);
}

