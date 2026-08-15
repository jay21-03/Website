package com.bautruc.ecommerce.cart.application;
public record CartItemSnapshot(Long cartItemId,long cartItemVersion,Long productId,int quantity){}
