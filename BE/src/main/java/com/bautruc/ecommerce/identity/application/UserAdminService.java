package com.bautruc.ecommerce.identity.application;

import java.time.Instant;
import java.util.List;

import com.bautruc.ecommerce.common.exception.ConflictException;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {
    private final UserJpaRepository userRepository;
    private final BusinessClock businessClock;

    public UserAdminService(UserJpaRepository userRepository, BusinessClock businessClock) {
        this.userRepository = userRepository;
        this.businessClock = businessClock;
    }

    @Transactional
    public User promote(Long id) {
        User user = findRequired(id);
        if (user.getRole() != UserRole.USER) {
            throw invalidTransition();
        }
        user.promote(now());
        return user;
    }

    @Transactional
    public User demote(Long id) {
        List<User> activeAdmins = userRepository.findActiveAdminsForUpdate();
        User user = findRequired(id);
        if (user.getRole() != UserRole.ADMIN) {
            throw invalidTransition();
        }
        if (user.getStatus() == UserStatus.ACTIVE && activeAdmins.size() <= 1) {
            throw lastAdminProtected();
        }
        user.demote(now());
        return user;
    }

    @Transactional
    public User block(Long id) {
        List<User> activeAdmins = userRepository.findActiveAdminsForUpdate();
        User user = findRequired(id);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw invalidTransition();
        }
        if (user.getRole() == UserRole.ADMIN && activeAdmins.size() <= 1) {
            throw lastAdminProtected();
        }
        user.block(now());
        return user;
    }

    @Transactional
    public User unblock(Long id) {
        User user = findRequired(id);
        if (user.getStatus() != UserStatus.BLOCKED) {
            throw invalidTransition();
        }
        user.unblock(now());
        return user;
    }

    private User findRequired(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        IdentityErrorCodes.USER_NOT_FOUND,
                        "User not found."
                ));
    }

    private Instant now() {
        return businessClock.now();
    }

    private ConflictException invalidTransition() {
        return new ConflictException(
                IdentityErrorCodes.USER_INVALID_ROLE_TRANSITION,
                "Invalid user role/status transition."
        );
    }

    private ConflictException lastAdminProtected() {
        return new ConflictException(
                IdentityErrorCodes.LAST_ADMIN_PROTECTED,
                "At least one active admin must remain."
        );
    }
}
