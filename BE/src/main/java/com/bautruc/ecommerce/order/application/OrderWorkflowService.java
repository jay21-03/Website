package com.bautruc.ecommerce.order.application;

import java.util.Map;
import java.util.stream.Collectors;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.order.domain.*;
import com.bautruc.ecommerce.order.infrastructure.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWorkflowService {
    private final OrderJpaRepository orders; private final OrderItemJpaRepository items;
    public OrderWorkflowService(OrderJpaRepository o, OrderItemJpaRepository i) { orders=o; items=i; }
    @Transactional public Order lock(Long id) { return orders.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException(OrderErrorCodes.ORDER_NOT_FOUND,"Order not found.")); }
    @Transactional(readOnly=true) public Order required(Long id) { return orders.findById(id).orElseThrow(() -> new ResourceNotFoundException(OrderErrorCodes.ORDER_NOT_FOUND,"Order not found.")); }
    @Transactional(readOnly=true) public Map<Long,Long> quantities(Long id) { return items.findByOrderIdOrderById(id).stream().collect(Collectors.toMap(OrderItem::getProductId,i -> (long)i.getQuantity())); }
}

