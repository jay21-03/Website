package com.bautruc.ecommerce.commerce.api.request;
import jakarta.validation.constraints.Size;
public record RecordManualRefundRequest(@Size(max=500) String note) {}

