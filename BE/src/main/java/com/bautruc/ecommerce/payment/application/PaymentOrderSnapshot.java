package com.bautruc.ecommerce.payment.application;

import java.time.Instant;

import com.bautruc.ecommerce.payment.domain.PaymentStatus;

public record PaymentOrderSnapshot(
        Long id,
        PaymentStatus status,
        long amount,
        String checkoutUrl,
        String qrCode,
        Instant expiresAt
) {
}
