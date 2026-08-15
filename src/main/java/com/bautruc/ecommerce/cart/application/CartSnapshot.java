package com.bautruc.ecommerce.cart.application;
import java.util.List;public record CartSnapshot(Long cartId,Long userId,List<CartItemSnapshot> items){}
