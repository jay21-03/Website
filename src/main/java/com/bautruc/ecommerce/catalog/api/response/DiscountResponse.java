package com.bautruc.ecommerce.catalog.api.response;
import java.math.BigDecimal;import java.time.Instant;import com.bautruc.ecommerce.catalog.domain.*;
public record DiscountResponse(Long id,Long productId,DiscountType discountType,BigDecimal discountValue,Instant startAt,Instant endAt,boolean isActive){
 public static DiscountResponse from(Discount d){return new DiscountResponse(d.getId(),d.getProductId(),d.getType(),d.getValue(),d.getStartAt(),d.getEndAt(),d.isActive());}}
