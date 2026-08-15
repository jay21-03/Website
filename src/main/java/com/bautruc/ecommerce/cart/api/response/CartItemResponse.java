package com.bautruc.ecommerce.cart.api.response;
public record CartItemResponse(Long id,Long productId,String nameVi,String nameEn,String thumbnailUrl,int quantity,long basePrice,long sellingPrice,long lineTotal,long availableQuantity){}
