package com.bautruc.ecommerce.identity.application;

import com.bautruc.ecommerce.identity.domain.User;

public record GoogleAuthenticationResult(User user, boolean created) {
}
