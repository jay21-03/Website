package com.bautruc.ecommerce.inventory.api.response;
import com.bautruc.ecommerce.inventory.domain.*;
public record InventoryListItemResponse(Long productId,String productNameVi,String productNameEn,long quantity,long reservedQuantity,long availableQuantity,long lowStockThreshold,InventoryAvailabilityStatus status){
 public static InventoryListItemResponse from(Inventory i){return new InventoryListItemResponse(i.getProductId(),i.getProduct().getNameVi(),i.getProduct().getNameEn(),i.getQuantity(),i.getReservedQuantity(),i.availableQuantity(),i.getLowStockThreshold(),i.status());}}
