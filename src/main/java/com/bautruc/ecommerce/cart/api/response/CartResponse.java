package com.bautruc.ecommerce.cart.api.response;
import java.util.List;public record CartResponse(List<CartItemResponse> items,long totalAmount){}
