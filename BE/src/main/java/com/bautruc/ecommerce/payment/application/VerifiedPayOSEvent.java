package com.bautruc.ecommerce.payment.application;
import java.time.Instant;
public record VerifiedPayOSEvent(Long orderId,long amount,String currency,String paymentLinkId,String reference,Instant providerTransactionAt,boolean success) {}

