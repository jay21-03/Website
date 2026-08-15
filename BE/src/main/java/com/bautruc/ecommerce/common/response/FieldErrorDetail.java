package com.bautruc.ecommerce.common.response;

public record FieldErrorDetail(
        String field,
        String message
) {
}
