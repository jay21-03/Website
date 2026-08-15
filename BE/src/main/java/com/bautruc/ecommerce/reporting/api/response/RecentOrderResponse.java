package com.bautruc.ecommerce.reporting.api.response;

import java.time.Instant;
import com.bautruc.ecommerce.order.domain.OrderStatus;
import com.bautruc.ecommerce.payment.domain.PaymentStatus;

public record RecentOrderResponse(Long id, String orderCode, Instant createdAt, long totalAmount,
                                  OrderStatus orderStatus, PaymentStatus paymentStatus) {}
