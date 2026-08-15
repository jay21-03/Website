package com.bautruc.ecommerce.commerce.application;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.order.application.OrderWorkflowService;
import com.bautruc.ecommerce.order.domain.OrderStatus;
import com.bautruc.ecommerce.payment.application.PaymentWorkflowService;
import com.bautruc.ecommerce.payment.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualRefundApplicationService {
    private final OrderWorkflowService orders;private final PaymentWorkflowService payments;private final BusinessClock clock;
    public ManualRefundApplicationService(OrderWorkflowService o,PaymentWorkflowService p,BusinessClock c){orders=o;payments=p;clock=c;}
    @Transactional public Payment record(Long paymentId,Long adminId,String note){Payment initial=payments.required(paymentId);var order=orders.lock(initial.getOrderId());Payment payment=payments.lock(paymentId);if(payment.getStatus()==PaymentStatus.REFUNDED)return payment;if(order.getStatus()!=OrderStatus.CANCELLED||payment.getStatus()!=PaymentStatus.PAID)throw new BusinessException("REFUND_NOT_ALLOWED","Manual refund can only be recorded for a cancelled paid order.",HttpStatus.CONFLICT);payment.refund(adminId,note==null||note.isBlank()?null:note.trim(),clock.now());return payment;}
}
