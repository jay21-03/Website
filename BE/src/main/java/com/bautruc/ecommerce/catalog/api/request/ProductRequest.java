package com.bautruc.ecommerce.catalog.api.request;
import com.bautruc.ecommerce.catalog.domain.ProductStatus;
import jakarta.validation.constraints.*;
public record ProductRequest(@NotBlank @Size(max=255) String nameVi,@NotBlank @Size(max=255) String nameEn,
 @Size(max=10000) String descriptionVi,@Size(max=10000) String descriptionEn,@NotNull @Positive Long basePrice,
 @NotNull @Positive Long collectionId,@NotNull ProductStatus status,@Size(max=120) String sizeCm,@Positive Long weightGrams,
 @Pattern(regexp="[a-z][a-z0-9]*(?:-[a-z0-9]+)*") @Size(max=300) String slug,
 @Size(max=180) String seoTitle,@Size(max=500) String seoDescription){
 public ProductRequest(String nameVi,String nameEn,String descriptionVi,String descriptionEn,Long basePrice,Long collectionId,ProductStatus status,String sizeCm,Long weightGrams){
  this(nameVi,nameEn,descriptionVi,descriptionEn,basePrice,collectionId,status,sizeCm,weightGrams,null,null,null);
 }
 public ProductRequest(String nameVi,String nameEn,String descriptionVi,String descriptionEn,Long basePrice,Long collectionId,ProductStatus status){
  this(nameVi,nameEn,descriptionVi,descriptionEn,basePrice,collectionId,status,null,null);
 }
}
