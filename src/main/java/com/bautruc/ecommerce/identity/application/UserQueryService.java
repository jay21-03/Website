package com.bautruc.ecommerce.identity.application;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.security.AuthenticatedUser;
import com.bautruc.ecommerce.common.security.SecurityErrorCodes;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserQueryService {
    private final UserJpaRepository userRepository;

    public UserQueryService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User currentUser(AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new BusinessException(
                        SecurityErrorCodes.AUTH_TOKEN_INVALID,
                        "Authentication token is invalid.",
                        HttpStatus.UNAUTHORIZED
                ));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessException(
                    SecurityErrorCodes.USER_BLOCKED,
                    "User is blocked.",
                    HttpStatus.UNAUTHORIZED
            );
        }
        return user;
    }
}
