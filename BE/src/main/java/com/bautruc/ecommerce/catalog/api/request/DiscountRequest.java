package com.bautruc.ecommerce.catalog.api.request;
import java.math.BigDecimal;import java.time.OffsetDateTime;
import com.bautruc.ecommerce.catalog.domain.DiscountType;import jakarta.validation.constraints.*;
public record DiscountRequest(@NotNull DiscountType discountType,@NotNull @Positive BigDecimal discountValue,
 @NotNull OffsetDateTime startAt,@NotNull OffsetDateTime endAt,@NotNull Boolean isActive){}
