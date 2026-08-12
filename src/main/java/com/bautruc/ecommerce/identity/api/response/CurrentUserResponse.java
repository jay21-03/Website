package com.bautruc.ecommerce.identity.api.response;

import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;

public record CurrentUserResponse(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        UserRole role,
        UserStatus status
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus()
        );
    }
}
