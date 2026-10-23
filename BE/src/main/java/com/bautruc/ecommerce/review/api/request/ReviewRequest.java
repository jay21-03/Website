package com.bautruc.ecommerce.review.api.request;

import jakarta.validation.constraints.*;

public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @NotBlank @Size(min = 10, max = 1000) String body) {}
