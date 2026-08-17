package com.bautruc.ecommerce.payment.application;

import java.time.Instant;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.payment.domain.Payment;
import com.bautruc.ecommerce.payment.infrastructure.PaymentJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWorkflowService {
    private final PaymentJpaRepository payments;
    public PaymentWorkflowService(PaymentJpaRepository p) { payments=p; }
    @Transactional public Payment lock(Long id) { return payments.findByIdForUpdate(id).orElseThrow(this::missing); }
    @Transactional(readOnly=true) public Payment required(Long id) { return payments.findById(id).orElseThrow(this::missing); }
    @Transactional(readOnly=true) public boolean externalReferenceUsedByAnotherPayment(String reference,Long currentPaymentId){return payments.findByExternalTransactionIdentifier(reference).filter(p->!p.getId().equals(currentPaymentId)).isPresent();}
    @Transactional public Payment lockForOrder(Long orderId) { return payments.findByOrderIdForUpdate(orderId).orElseThrow(this::missing); }
    @Transactional(readOnly=true) public Payment forOrder(Long orderId) { return payments.findByOrderId(orderId).orElseThrow(this::missing); }
    @Transactional public void captureProvider(Long id,String link,String url,String qr,Instant now){lock(id).captureProvider(link,url,qr,now);}
    private ResourceNotFoundException missing(){return new ResourceNotFoundException(PaymentErrorCodes.PAYMENT_NOT_FOUND,"Payment not found.");}
}
