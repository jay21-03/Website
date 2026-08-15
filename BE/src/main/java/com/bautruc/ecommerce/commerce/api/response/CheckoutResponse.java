package com.bautruc.ecommerce.commerce.api.response;
import java.time.Instant;
import com.bautruc.ecommerce.payment.domain.PaymentStatus;
public record CheckoutResponse(Long checkoutOperationId,Long orderId,String orderCode,Long paymentId,PaymentStatus paymentStatus,long totalAmount,String checkoutUrl,String qrCode,Instant expiresAt) {}
