package com.bautruc.ecommerce.catalog.api.response;
import java.time.Instant;import java.util.List;import com.bautruc.ecommerce.catalog.domain.*;
public record ProductResponse(Long id,String nameVi,String nameEn,String descriptionVi,String descriptionEn,long basePrice,long sellingPrice,ProductStatus status,Long collectionId,String thumbnailUrl,List<ProductImageResponse> images,Instant createdAt,Instant updatedAt){}
