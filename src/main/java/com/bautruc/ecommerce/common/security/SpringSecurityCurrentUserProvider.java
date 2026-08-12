package com.bautruc.ecommerce.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {
    @Override
    public Optional<AuthenticatedUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return Optional.of(authenticatedUser);
        }
        return Optional.empty();
    }
}
