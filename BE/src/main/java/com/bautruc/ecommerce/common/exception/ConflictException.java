package com.bautruc.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(GlobalExceptionHandler.CONFLICT, message, HttpStatus.CONFLICT);
    }

    public ConflictException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}
