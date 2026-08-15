package com.bautruc.ecommerce.commerce.application;

import java.util.Map;
import com.bautruc.ecommerce.commerce.domain.*;
import com.bautruc.ecommerce.commerce.infrastructure.CheckoutOperationJpaRepository;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.inventory.application.InventoryCommandService;
import com.bautruc.ecommerce.notification.application.NotificationService;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.bautruc.ecommerce.order.application.OrderWorkflowService;
import com.bautruc.ecommerce.order.domain.*;
import com.bautruc.ecommerce.payment.application.*;
import com.bautruc.ecommerce.payment.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutFailureCompensationService {
    private final CheckoutOperationJpaRepository operations;private final OrderWorkflowService orders;private final PaymentWorkflowService payments;private final InventoryCommandService inventory;private final NotificationService notifications;private final BusinessClock clock;
    public CheckoutFailureCompensationService(CheckoutOperationJpaRepository o,OrderWorkflowService os,PaymentWorkflowService ps,InventoryCommandService i,NotificationService n,BusinessClock c){operations=o;orders=os;payments=ps;inventory=i;notifications=n;clock=c;}
    @Transactional public void compensate(Long id){CheckoutOperation op=operations.findByIdForUpdate(id).orElseThrow();if(op.getState()==CheckoutOperationState.FAILED)return;Order order=orders.lock(op.getOrderId());Payment payment=payments.lock(op.getPaymentId());Map<Long,Long> quantities=orders.quantities(order.getId());if(payment.getStatus()==PaymentStatus.PENDING)payment.markTerminal(PaymentStatus.FAILED,clock.now());if(order.getStatus()==OrderStatus.NEW)order.cancel(clock.now());inventory.release(order.getId(),quantities);notifications.create(NotificationType.PAYMENT_FAILED,"Payment failed","Payment creation failed for "+order.getOrderCode()+".","PAYMENT",payment.getId(),"{\"actualStatus\":\"FAILED\"}","PAYMENT_FAILED:PAYMENT:"+payment.getId()+":FAILED",clock.now());op.fail(PaymentErrorCodes.PAYOS_REQUEST_FAILED,clock.now());}
}

