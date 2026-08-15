package com.bautruc.ecommerce.inventory.api.request;
import com.bautruc.ecommerce.inventory.domain.InventoryTransactionType;import jakarta.validation.constraints.*;
public record AdjustInventoryRequest(@NotNull InventoryTransactionType type,@NotNull Long quantityChange,@NotBlank @Size(max=500)String reason){}
