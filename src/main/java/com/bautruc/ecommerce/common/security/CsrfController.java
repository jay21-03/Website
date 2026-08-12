package com.bautruc.ecommerce.common.security;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    private final BusinessClock businessClock;

    public CsrfController(BusinessClock businessClock) {
        this.businessClock = businessClock;
    }

    @GetMapping("/api/v1/auth/csrf")
    public ApiResponse<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        String token = csrfToken.getToken();
        return ApiResponse.success(
                new CsrfTokenResponse(token),
                null,
                businessClock.businessNow().toOffsetDateTime(),
                LogContext.currentCorrelationId()
        );
    }
}
