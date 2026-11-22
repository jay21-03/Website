package com.bautruc.ecommerce.catalog.api.response;
import java.time.Instant;import com.bautruc.ecommerce.catalog.domain.*;
public record CollectionResponse(Long id,String slug,String seoTitle,String seoDescription,String nameVi,String nameEn,String descriptionVi,String descriptionEn,CollectionStatus status,Instant createdAt,Instant updatedAt){
 public static CollectionResponse from(ProductCollection c){return new CollectionResponse(c.getId(),c.getSlug(),c.getSeoTitle(),c.getSeoDescription(),c.getNameVi(),c.getNameEn(),c.getDescriptionVi(),c.getDescriptionEn(),c.getStatus(),c.getCreatedAt(),c.getUpdatedAt());}}
