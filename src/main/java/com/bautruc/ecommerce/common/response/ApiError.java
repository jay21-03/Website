package com.bautruc.ecommerce.common.response;

import java.util.List;

public record ApiError(
        String code,
        String message,
        List<FieldErrorDetail> fieldErrors
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
