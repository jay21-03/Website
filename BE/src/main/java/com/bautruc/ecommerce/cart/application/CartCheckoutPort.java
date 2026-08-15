package com.bautruc.ecommerce.cart.application;
public interface CartCheckoutPort{CartSnapshot snapshot(Long userId);CartSnapshot lockAndSnapshot(Long userId);void clearCheckedOutItems(Long userId,CartSnapshot snapshot);void clearCheckedOutItems(Long userId,java.util.List<CartItemSnapshot> items);}
