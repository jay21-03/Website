package com.bautruc.ecommerce.workshop.api.request;

import com.bautruc.ecommerce.workshop.domain.WorkshopOfferingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkshopOfferingRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 2000) String description,
        @Min(0) long priceAmount,
        @Min(1) @Max(1440) int durationMinutes,
        @Min(1) @Max(100) int maxParticipants,
        @Size(max = 1024) String imageUrl,
        @NotNull WorkshopOfferingStatus status
) {
}
