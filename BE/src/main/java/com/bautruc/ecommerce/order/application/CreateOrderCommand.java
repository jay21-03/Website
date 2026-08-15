package com.bautruc.ecommerce.order.application;
import java.util.List;public record CreateOrderCommand(Long userId,String receiverName,String phone,String email,String address,String note,List<OrderLineCommand> lines){}
