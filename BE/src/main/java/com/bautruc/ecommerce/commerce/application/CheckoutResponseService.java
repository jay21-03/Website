package com.bautruc.ecommerce.commerce.application;

import com.bautruc.ecommerce.commerce.api.response.CheckoutResponse;
import com.bautruc.ecommerce.commerce.domain.CheckoutOperation;
import com.bautruc.ecommerce.order.application.OrderWorkflowService;
import com.bautruc.ecommerce.order.domain.Order;
import com.bautruc.ecommerce.payment.application.PaymentWorkflowService;
import com.bautruc.ecommerce.payment.domain.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutResponseService {
    private final OrderWorkflowService orders;private final PaymentWorkflowService payments;
    public CheckoutResponseService(OrderWorkflowService o,PaymentWorkflowService p){orders=o;payments=p;}
    @Transactional(readOnly=true) public CheckoutResponse build(CheckoutOperation op){Order o=orders.required(op.getOrderId());Payment p=payments.forOrder(o.getId());return new CheckoutResponse(op.getId(),o.getId(),o.getOrderCode(),p.getId(),p.getStatus(),o.getTotalAmount(),p.getCheckoutUrl(),p.getQrCode(),p.getExpiresAt());}
}

