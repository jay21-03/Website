package com.bautruc.ecommerce.identity.api;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.security.CurrentUserProvider;
import com.bautruc.ecommerce.common.security.SecurityErrorCodes;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.identity.api.response.CurrentUserResponse;
import com.bautruc.ecommerce.identity.application.UserQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {
    private final CurrentUserProvider currentUserProvider;
    private final UserQueryService userQueryService;
    private final BusinessClock businessClock;

    public MeController(
            CurrentUserProvider currentUserProvider,
            UserQueryService userQueryService,
            BusinessClock businessClock
    ) {
        this.currentUserProvider = currentUserProvider;
        this.userQueryService = userQueryService;
        this.businessClock = businessClock;
    }

    @GetMapping("/api/v1/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.success(
                CurrentUserResponse.from(userQueryService.currentUser(currentUserProvider.currentUser()
                        .orElseThrow(() -> new BusinessException(
                                SecurityErrorCodes.AUTH_TOKEN_MISSING,
                                "Authentication token is missing.",
                                HttpStatus.UNAUTHORIZED
                        )))),
                null,
                businessClock.businessNow().toOffsetDateTime(),
                LogContext.currentCorrelationId()
        );
    }
}
