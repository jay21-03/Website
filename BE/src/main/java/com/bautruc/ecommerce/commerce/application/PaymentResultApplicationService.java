package com.bautruc.ecommerce.commerce.application;

import java.time.Instant;
import java.util.Map;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.inventory.application.InventoryCommandService;
import com.bautruc.ecommerce.notification.application.NotificationService;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.bautruc.ecommerce.order.application.OrderWorkflowService;
import com.bautruc.ecommerce.order.domain.*;
import com.bautruc.ecommerce.payment.application.*;
import com.bautruc.ecommerce.payment.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentResultApplicationService implements PaymentResultPort {
    private final OrderWorkflowService orders;private final PaymentWorkflowService payments;private final InventoryCommandService inventory;private final NotificationService notifications;private final BusinessClock clock;
    public PaymentResultApplicationService(OrderWorkflowService o,PaymentWorkflowService p,InventoryCommandService i,NotificationService n,BusinessClock c){orders=o;payments=p;inventory=i;notifications=n;clock=c;}
    @Override @Transactional public void processSuccess(VerifiedPayOSEvent event,Instant receivedAt){Order order=orders.lock(event.orderId());Payment payment=payments.lockForOrder(order.getId());validate(event,payment,order);if(payment.getStatus()==PaymentStatus.PAID)return;if(terminal(payment.getStatus())){payment.requireManualResolution("LATE_VERIFIED_PAYMENT_SUCCESS",clock.now());return;}Instant effective=event.providerTransactionAt()!=null?event.providerTransactionAt():(receivedAt.compareTo(payment.getExpiresAt())<=0?receivedAt:null);if(effective==null||effective.isAfter(payment.getExpiresAt())){expireForLateSuccess(order,payment);return;}if(event.reference()!=null&&payments.externalReferenceUsedByAnotherPayment(event.reference(),payment.getId()))throw new BusinessException(PaymentErrorCodes.PAYMENT_EXTERNAL_ID_CONFLICT,"Provider transaction reference is already used.",HttpStatus.CONFLICT);inventory.sale(order.getId(),orders.quantities(order.getId()));payment.markPaid(event.reference(),effective);notifications.create(NotificationType.PAYMENT_SUCCESS,"Payment successful","Payment received for "+order.getOrderCode()+".","PAYMENT",payment.getId(),null,"PAYMENT_SUCCESS:PAYMENT:"+payment.getId(),clock.now());}
    @Override @Transactional public void processTerminalFailure(Long paymentId,PaymentStatus target){Payment initial=payments.required(paymentId);Order order=orders.lock(initial.getOrderId());Payment payment=payments.lock(paymentId);if(payment.getStatus()==target)return;if(payment.getStatus()==PaymentStatus.PAID||payment.getStatus()==PaymentStatus.REFUNDED)throw new BusinessException(PaymentErrorCodes.PAYMENT_INVALID_STATE,"Paid payment cannot become terminal unsuccessful.",HttpStatus.CONFLICT);if(payment.getStatus()!=PaymentStatus.PENDING)return;if(target==PaymentStatus.EXPIRED&&payment.getExpiresAt().isAfter(clock.now()))return;payment.markTerminal(target,clock.now());if(order.getStatus()==OrderStatus.NEW||order.getStatus()==OrderStatus.CONFIRMED)order.cancel(clock.now());inventory.release(order.getId(),orders.quantities(order.getId()));failedNotification(order,payment,target);}
    private void expireForLateSuccess(Order order,Payment payment){payment.markTerminal(PaymentStatus.EXPIRED,clock.now());payment.requireManualResolution("PAYMENT_SUCCESS_TIME_UNVERIFIABLE_AFTER_EXPIRY",clock.now());if(order.getStatus()==OrderStatus.NEW||order.getStatus()==OrderStatus.CONFIRMED)order.cancel(clock.now());inventory.release(order.getId(),orders.quantities(order.getId()));failedNotification(order,payment,PaymentStatus.EXPIRED);}
    private void validate(VerifiedPayOSEvent e,Payment p,Order o){if(!e.success()||e.amount()!=p.getAmount()||p.getAmount()!=o.getTotalAmount())throw new BusinessException(PaymentErrorCodes.PAYMENT_AMOUNT_MISMATCH,"Payment amount mismatch.",HttpStatus.CONFLICT);if(!"VND".equals(e.currency()))throw new BusinessException(PaymentErrorCodes.PAYOS_WEBHOOK_INVALID,"Invalid payment currency.");if(p.getProviderPaymentLinkId()!=null&&!p.getProviderPaymentLinkId().equals(e.paymentLinkId()))throw new BusinessException(PaymentErrorCodes.PAYOS_WEBHOOK_INVALID,"Payment link correlation mismatch.",HttpStatus.CONFLICT);if(p.getExternalTransactionIdentifier()!=null&&!p.getExternalTransactionIdentifier().equals(e.reference()))throw new BusinessException(PaymentErrorCodes.PAYMENT_EXTERNAL_ID_CONFLICT,"Payment reference correlation mismatch.",HttpStatus.CONFLICT);}
    private void failedNotification(Order o,Payment p,PaymentStatus s){notifications.create(NotificationType.PAYMENT_FAILED,"Payment unsuccessful","Payment for "+o.getOrderCode()+" is "+s+".","PAYMENT",p.getId(),"{\"actualStatus\":\""+s+"\"}","PAYMENT_FAILED:PAYMENT:"+p.getId()+":"+s,clock.now());}
    private boolean terminal(PaymentStatus s){return s==PaymentStatus.FAILED||s==PaymentStatus.CANCELLED||s==PaymentStatus.EXPIRED||s==PaymentStatus.REFUNDED;}
}
