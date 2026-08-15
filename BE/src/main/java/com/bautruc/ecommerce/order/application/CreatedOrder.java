package com.bautruc.ecommerce.order.application;
import com.bautruc.ecommerce.order.domain.Order;import java.util.List;public record CreatedOrder(Order order,List<com.bautruc.ecommerce.order.domain.OrderItem> items){}
