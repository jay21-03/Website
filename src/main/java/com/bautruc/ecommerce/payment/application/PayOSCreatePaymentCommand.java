package com.bautruc.ecommerce.payment.application;
import java.time.Instant;
public record PayOSCreatePaymentCommand(Long orderId,long amount,String description,String returnUrl,String cancelUrl,Instant expiresAt) {}

