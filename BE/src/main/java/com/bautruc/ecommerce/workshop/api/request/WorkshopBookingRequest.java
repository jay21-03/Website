package com.bautruc.ecommerce.workshop.api.request;

import java.time.OffsetDateTime;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkshopBookingRequest(
        Long workshopId,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 32) String phone,
        @NotNull OffsetDateTime preferredAt,
        @Min(1) @Max(30) int participants,
        @Size(max = 1000) String note
) {
}
