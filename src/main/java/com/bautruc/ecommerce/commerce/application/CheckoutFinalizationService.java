package com.bautruc.ecommerce.commerce.application;

import java.util.List;
import com.bautruc.ecommerce.cart.application.*;
import com.bautruc.ecommerce.commerce.domain.*;
import com.bautruc.ecommerce.commerce.infrastructure.*;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.notification.application.NotificationService;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.bautruc.ecommerce.order.application.OrderWorkflowService;
import com.bautruc.ecommerce.order.domain.*;
import com.bautruc.ecommerce.payment.application.PaymentWorkflowService;
import com.bautruc.ecommerce.payment.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutFinalizationService {
    private final CheckoutOperationJpaRepository operations;private final CheckoutOperationItemJpaRepository snapshots;private final OrderWorkflowService orders;private final PaymentWorkflowService payments;private final CartCheckoutPort cart;private final NotificationService notifications;private final BusinessClock clock;
    public CheckoutFinalizationService(CheckoutOperationJpaRepository o,CheckoutOperationItemJpaRepository s,OrderWorkflowService os,PaymentWorkflowService ps,CartCheckoutPort c,NotificationService n,BusinessClock b){operations=o;snapshots=s;orders=os;payments=ps;cart=c;notifications=n;clock=b;}
    @Transactional public void finalizeCheckout(Long id){CheckoutOperation op=operations.findByIdForUpdate(id).orElseThrow();if(op.getState()==CheckoutOperationState.COMPLETED)return;if(op.getState()!=CheckoutOperationState.PAYOS_CREATED)throw pending();Order order=orders.lock(op.getOrderId());Payment payment=payments.lock(op.getPaymentId());if(order.getStatus()!=OrderStatus.NEW||terminal(payment.getStatus()))throw pending();List<CartItemSnapshot> items=snapshots.findByCheckoutOperationId(op.getId()).stream().map(i->new CartItemSnapshot(i.getCartItemId(),i.getCartItemVersion(),i.getProductId(),i.getQuantity())).toList();cart.clearCheckedOutItems(op.getUserId(),items);notifications.create(NotificationType.NEW_ORDER,"New order","Order "+order.getOrderCode()+" was created.","ORDER",order.getId(),null,"NEW_ORDER:ORDER:"+order.getId(),clock.now());op.complete(clock.now());}
    private boolean terminal(PaymentStatus s){return s==PaymentStatus.FAILED||s==PaymentStatus.CANCELLED||s==PaymentStatus.EXPIRED||s==PaymentStatus.REFUNDED;}
    private BusinessException pending(){return new BusinessException(CommerceErrorCodes.CHECKOUT_FINALIZATION_PENDING,"Checkout finalization is pending.",HttpStatus.SERVICE_UNAVAILABLE);}
}

