package com.bautruc.ecommerce.cart.api.request;
import jakarta.validation.constraints.*;public record AddCartItemRequest(@NotNull @Positive Long productId,@NotNull @Min(1)Integer quantity){}
