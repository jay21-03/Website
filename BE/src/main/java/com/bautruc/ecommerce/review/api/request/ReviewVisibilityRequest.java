package com.bautruc.ecommerce.review.api.request;

import jakarta.validation.constraints.NotNull;

public record ReviewVisibilityRequest(@NotNull Boolean hidden) {}
