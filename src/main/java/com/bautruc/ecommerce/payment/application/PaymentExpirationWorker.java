package com.bautruc.ecommerce.payment.application;

import com.bautruc.ecommerce.payment.domain.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentExpirationWorker {
    private final PaymentResultPort results;
    public PaymentExpirationWorker(PaymentResultPort r){results=r;}
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void expire(Long id){results.processTerminalFailure(id,PaymentStatus.EXPIRED);}
}

