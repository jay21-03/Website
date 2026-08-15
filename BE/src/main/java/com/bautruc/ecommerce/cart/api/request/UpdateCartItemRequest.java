package com.bautruc.ecommerce.cart.api.request;
import jakarta.validation.constraints.*;public record UpdateCartItemRequest(@NotNull @Min(1)Integer quantity){}
