package com.bautruc.ecommerce.commerce.application;

import java.time.Instant;
import com.bautruc.ecommerce.commerce.domain.*;
import com.bautruc.ecommerce.commerce.infrastructure.CheckoutOperationJpaRepository;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.payment.application.PaymentWorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutStateService {
    private final CheckoutOperationJpaRepository operations;private final PaymentWorkflowService payments;private final BusinessClock clock;
    public CheckoutStateService(CheckoutOperationJpaRepository o,PaymentWorkflowService p,BusinessClock c){operations=o;payments=p;clock=c;}
    @Transactional public boolean claimCreate(Long id){CheckoutOperation op=lock(id);if(op.getState()!=CheckoutOperationState.LOCAL_PREPARED)return false;op.claimPayOSCreate(clock.now());return true;}
    @Transactional public void captureCreateSuccess(Long id,String link,String url,String qr){CheckoutOperation op=lock(id);payments.lock(op.getPaymentId()).captureProvider(link,url,qr,clock.now());op.payOSCreated(clock.now());}
    @Transactional public void resetStaleCreate(Long id){CheckoutOperation op=lock(id);if(op.getState()==CheckoutOperationState.PAYOS_CREATING)op.resetForRetry(clock.now());}
    @Transactional(readOnly=true) public CheckoutOperation required(Long id){return operations.findById(id).orElseThrow(()->new ResourceNotFoundException("CHECKOUT_NOT_FOUND","Checkout operation not found."));}
    private CheckoutOperation lock(Long id){return operations.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("CHECKOUT_NOT_FOUND","Checkout operation not found."));}
}

