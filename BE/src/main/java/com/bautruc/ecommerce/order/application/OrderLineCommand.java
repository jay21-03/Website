package com.bautruc.ecommerce.order.application;
public record OrderLineCommand(Long productId,String productNameVi,String productNameEn,long basePrice,long sellingPrice,int quantity){}
