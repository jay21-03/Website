package com.bautruc.ecommerce.catalog.api.request;
import com.bautruc.ecommerce.catalog.domain.CollectionStatus;
import jakarta.validation.constraints.*;
public record CollectionRequest(@NotBlank @Size(max=255) String nameVi,@NotBlank @Size(max=255) String nameEn,
 @Size(max=10000) String descriptionVi,@Size(max=10000) String descriptionEn,@NotNull CollectionStatus status){}
