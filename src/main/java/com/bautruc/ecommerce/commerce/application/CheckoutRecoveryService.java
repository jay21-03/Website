package com.bautruc.ecommerce.commerce.application;

import com.bautruc.ecommerce.commerce.domain.*;
import com.bautruc.ecommerce.payment.application.*;
import org.springframework.stereotype.Service;

@Service
public class CheckoutRecoveryService {
    private final CheckoutStateService states;private final CheckoutFinalizationService finalization;private final PayOSClient payos;
    public CheckoutRecoveryService(CheckoutStateService s,CheckoutFinalizationService f,PayOSClient p){states=s;finalization=f;payos=p;}
    public void recover(Long id){CheckoutOperation op=states.required(id);if(op.getState()==CheckoutOperationState.PAYOS_CREATED){finalization.finalizeCheckout(id);return;}if(op.getState()!=CheckoutOperationState.PAYOS_CREATING)return;java.util.Optional<PayOSPaymentResult> found=payos.getPaymentRequest(op.getOrderId());if(found.isPresent()){PayOSPaymentResult r=found.get();states.captureCreateSuccess(id,r.paymentLinkId(),r.checkoutUrl(),r.qrCode());finalization.finalizeCheckout(id);}else states.resetStaleCreate(id);}
}

