package com.bautruc.ecommerce.workshop.api.response;

import java.time.Instant;
import com.bautruc.ecommerce.workshop.domain.WorkshopOffering;
import com.bautruc.ecommerce.workshop.domain.WorkshopOfferingStatus;

public record WorkshopOfferingResponse(
        Long id,
        String title,
        String description,
        long priceAmount,
        int durationMinutes,
        int maxParticipants,
        String imageUrl,
        WorkshopOfferingStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkshopOfferingResponse from(WorkshopOffering offering) {
        return new WorkshopOfferingResponse(
                offering.getId(),
                offering.getTitle(),
                offering.getDescription(),
                offering.getPriceAmount(),
                offering.getDurationMinutes(),
                offering.getMaxParticipants(),
                offering.getImageUrl(),
                offering.getStatus(),
                offering.getCreatedAt(),
                offering.getUpdatedAt()
        );
    }
}
