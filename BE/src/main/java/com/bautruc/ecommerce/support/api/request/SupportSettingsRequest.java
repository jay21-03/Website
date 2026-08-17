package com.bautruc.ecommerce.support.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportSettingsRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 32) String zaloPhone,
        @Size(max = 32) String secondaryPhone,
        @Size(max = 1024) String facebookUrl,
        @NotBlank @Size(max = 500) String address,
        @Size(max = 1024) String mapUrl,
        @Size(max = 255) String openingHours
) {
}
