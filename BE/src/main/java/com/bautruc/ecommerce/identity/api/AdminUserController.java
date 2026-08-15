package com.bautruc.ecommerce.identity.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.identity.api.response.AdminUserResponse;
import com.bautruc.ecommerce.identity.application.UserAdminService;
import com.bautruc.ecommerce.identity.application.UserQueryService;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUserController {
    private final UserQueryService userQueryService;
    private final UserAdminService userAdminService;
    private final BusinessClock businessClock;

    public AdminUserController(
            UserQueryService userQueryService,
            UserAdminService userAdminService,
            BusinessClock businessClock
    ) {
        this.userQueryService = userQueryService;
        this.userAdminService = userAdminService;
        this.businessClock = businessClock;
    }

    @GetMapping("/api/v1/admin/users")
    public ApiResponse<PageResponse<AdminUserResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        Page<User> users = userQueryService.listUsers(keyword, role, status, page, size, sort);
        return success(PageResponse.from(users.map(AdminUserResponse::from)));
    }

    @GetMapping("/api/v1/admin/users/{id}")
    public ApiResponse<AdminUserResponse> detail(@PathVariable Long id) {
        return success(AdminUserResponse.from(userQueryService.detail(id)));
    }

    @PostMapping("/api/v1/admin/users/{id}/promote")
    public ApiResponse<AdminUserResponse> promote(@PathVariable Long id) {
        return success(AdminUserResponse.from(userAdminService.promote(id)));
    }

    @PostMapping("/api/v1/admin/users/{id}/demote")
    public ApiResponse<AdminUserResponse> demote(@PathVariable Long id) {
        return success(AdminUserResponse.from(userAdminService.demote(id)));
    }

    @PostMapping("/api/v1/admin/users/{id}/block")
    public ApiResponse<AdminUserResponse> block(@PathVariable Long id) {
        return success(AdminUserResponse.from(userAdminService.block(id)));
    }

    @PostMapping("/api/v1/admin/users/{id}/unblock")
    public ApiResponse<AdminUserResponse> unblock(@PathVariable Long id) {
        return success(AdminUserResponse.from(userAdminService.unblock(id)));
    }

    private <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(
                data,
                null,
                businessClock.businessNow().toOffsetDateTime(),
                LogContext.currentCorrelationId()
        );
    }
}
