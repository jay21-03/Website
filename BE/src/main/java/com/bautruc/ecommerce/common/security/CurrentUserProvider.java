package com.bautruc.ecommerce.common.security;

import java.util.Optional;

public interface CurrentUserProvider {
    Optional<AuthenticatedUser> currentUser();
}
