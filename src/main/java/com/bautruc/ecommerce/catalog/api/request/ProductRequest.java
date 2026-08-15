package com.bautruc.ecommerce.catalog.api.request;
import com.bautruc.ecommerce.catalog.domain.ProductStatus;
import jakarta.validation.constraints.*;
public record ProductRequest(@NotBlank @Size(max=255) String nameVi,@NotBlank @Size(max=255) String nameEn,
 @Size(max=10000) String descriptionVi,@Size(max=10000) String descriptionEn,@NotNull @Positive Long basePrice,
 @NotNull @Positive Long collectionId,@NotNull ProductStatus status){}
