package com.bautruc.ecommerce.commerce.application;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.inventory.application.InventoryCommandService;
import com.bautruc.ecommerce.notification.application.NotificationService;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.bautruc.ecommerce.order.application.*;
import com.bautruc.ecommerce.order.domain.*;
import com.bautruc.ecommerce.payment.application.PaymentWorkflowService;
import com.bautruc.ecommerce.payment.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCancellationTransactionService {
    private final OrderWorkflowService orders;private final PaymentWorkflowService payments;private final InventoryCommandService inventory;private final NotificationService notifications;private final BusinessClock clock;
    public OrderCancellationTransactionService(OrderWorkflowService o,PaymentWorkflowService p,InventoryCommandService i,NotificationService n,BusinessClock c){orders=o;payments=p;inventory=i;notifications=n;clock=c;}
    @Transactional public ProviderCancellation cancel(Long orderId){Order order=orders.lock(orderId);Payment payment=payments.lockForOrder(orderId);if(order.getStatus()==OrderStatus.CANCELLED)return ProviderCancellation.none();if(order.getStatus()==OrderStatus.COMPLETED)throw new BusinessException(OrderErrorCodes.ORDER_INVALID_TRANSITION,"Completed order cannot be cancelled.",HttpStatus.CONFLICT);if(payment.getStatus()==PaymentStatus.PENDING){payment.markTerminal(PaymentStatus.CANCELLED,clock.now());order.cancel(clock.now());inventory.release(orderId,orders.quantities(orderId));notify(order,payment,PaymentStatus.CANCELLED);String providerId=payment.getProviderPaymentLinkId()!=null?payment.getProviderPaymentLinkId():String.valueOf(orderId);return new ProviderCancellation(providerId,"Admin cancelled order");}if(payment.getStatus()==PaymentStatus.PAID){inventory.restore(orderId,orders.quantities(orderId));order.cancel(clock.now());return ProviderCancellation.none();}throw new BusinessException("ORDER_CANCELLATION_NOT_ALLOWED","Order cannot be cancelled for current payment state.",HttpStatus.CONFLICT);}
    private void notify(Order o,Payment p,PaymentStatus s){notifications.create(NotificationType.PAYMENT_FAILED,"Payment cancelled","Payment for "+o.getOrderCode()+" was cancelled.","PAYMENT",p.getId(),"{\"actualStatus\":\""+s+"\"}","PAYMENT_FAILED:PAYMENT:"+p.getId()+":"+s,clock.now());}
}

