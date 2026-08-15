package com.bautruc.ecommerce.commerce.application;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.commerce.domain.CheckoutOperation;
import com.bautruc.ecommerce.order.application.OrderWorkflowService;
import com.bautruc.ecommerce.order.domain.Order;
import com.bautruc.ecommerce.payment.application.*;
import com.bautruc.ecommerce.payment.domain.Payment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CheckoutPayOSService {
    private final CheckoutStateService states;private final PayOSClient client;private final OrderWorkflowService orders;private final PaymentWorkflowService payments;private final CheckoutFailureCompensationService compensation;private final ApplicationProperties properties;
    public CheckoutPayOSService(CheckoutStateService s,PayOSClient c,OrderWorkflowService o,PaymentWorkflowService p,CheckoutFailureCompensationService f,ApplicationProperties a){states=s;client=c;orders=o;payments=p;compensation=f;properties=a;}
    public void create(Long id){if(!states.claimCreate(id))throw inProgress();CheckoutOperation op=states.required(id);Order order=orders.required(op.getOrderId());Payment payment=payments.forOrder(order.getId());String url=properties.frontendBaseUrl()+"/orders/"+order.getId();PayOSCreatePaymentCommand command=new PayOSCreatePaymentCommand(order.getId(),payment.getAmount(),description(order.getId()),url,url,payment.getExpiresAt());try{capture(id,client.createPaymentRequest(command));}catch(PayOSRequestException e){if(e.ambiguous()){try{java.util.Optional<PayOSPaymentResult> found=client.getPaymentRequest(order.getId());if(found.isPresent()){capture(id,found.get());return;}}catch(PayOSRequestException ignored){}throw inProgress();}compensation.compensate(id);throw new BusinessException(PaymentErrorCodes.PAYOS_REQUEST_FAILED,"Unable to create payOS payment request.",HttpStatus.BAD_GATEWAY);}}
    private void capture(Long id,PayOSPaymentResult r){states.captureCreateSuccess(id,r.paymentLinkId(),r.checkoutUrl(),r.qrCode());}
    private String description(Long id){String digits=Long.toUnsignedString(id);return "D"+(digits.length()>8?digits.substring(digits.length()-8):"0".repeat(8-digits.length())+digits);}
    private BusinessException inProgress(){return new BusinessException(CommerceErrorCodes.CHECKOUT_IN_PROGRESS,"Checkout is being processed.",HttpStatus.ACCEPTED);}
}

