package com.bautruc.ecommerce.workshop.api.response;

import java.time.Instant;
import com.bautruc.ecommerce.workshop.domain.WorkshopBooking;
import com.bautruc.ecommerce.workshop.domain.WorkshopBookingStatus;

public record WorkshopBookingResponse(
        Long id,
        Long workshopId,
        String fullName,
        String email,
        String phone,
        Instant preferredAt,
        int participants,
        String note,
        WorkshopBookingStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkshopBookingResponse from(WorkshopBooking booking) {
        return new WorkshopBookingResponse(
                booking.getId(),
                booking.getWorkshopId(),
                booking.getFullName(),
                booking.getEmail(),
                booking.getPhone(),
                booking.getPreferredAt(),
                booking.getParticipants(),
                booking.getNote(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
