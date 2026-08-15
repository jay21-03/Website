package com.bautruc.ecommerce.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiError;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.response.FieldErrorDetail;
import com.bautruc.ecommerce.common.time.BusinessClock;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private final BusinessClock businessClock;

    public GlobalExceptionHandler(BusinessClock businessClock) {
        this.businessClock = businessClock;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.of(exception.code(), exception.getMessage());
        return ResponseEntity.status(exception.status()).body(failure(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorDetail> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDetail)
                .toList();
        ApiError error = new ApiError(
                VALIDATION_FAILED,
                "Validation failed.",
                fieldErrors
        );
        return ResponseEntity.badRequest().body(failure(error));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.of(
                VALIDATION_FAILED,
                "Validation failed."
        );
        return ResponseEntity.badRequest().body(failure(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.of(
                VALIDATION_FAILED,
                "Malformed JSON request."
        );
        return ResponseEntity.badRequest().body(failure(error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.of(
                VALIDATION_FAILED,
                "Validation failed."
        );
        return ResponseEntity.badRequest().body(failure(error));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.of("PRODUCT_IMAGE_TOO_LARGE", "Image exceeds the configured maximum size.");
        return ResponseEntity.badRequest().body(failure(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.of(
                INTERNAL_ERROR,
                "Unexpected server error"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failure(error));
    }

    private FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ApiResponse<Void> failure(ApiError error) {
        return ApiResponse.failure(error, responseTimestamp(), LogContext.currentCorrelationId());
    }

    private OffsetDateTime responseTimestamp() {
        return businessClock.businessNow().toOffsetDateTime();
    }
}
