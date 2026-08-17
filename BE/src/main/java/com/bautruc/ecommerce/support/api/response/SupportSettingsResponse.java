package com.bautruc.ecommerce.support.api.response;

import java.time.Instant;
import com.bautruc.ecommerce.support.domain.SupportSettings;

public record SupportSettingsResponse(
        String email,
        String zaloPhone,
        String secondaryPhone,
        String facebookUrl,
        String address,
        String mapUrl,
        String openingHours,
        Instant updatedAt
) {
    public static SupportSettingsResponse from(SupportSettings settings) {
        return new SupportSettingsResponse(
                settings.getEmail(),
                settings.getZaloPhone(),
                settings.getSecondaryPhone(),
                settings.getFacebookUrl(),
                settings.getAddress(),
                settings.getMapUrl(),
                settings.getOpeningHours(),
                settings.getUpdatedAt()
        );
    }
}
