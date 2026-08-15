package com.bautruc.ecommerce.payment.application;
public record PayOSPaymentResult(Long orderCode,long amount,String paymentLinkId,String status,String checkoutUrl,String qrCode) {}

