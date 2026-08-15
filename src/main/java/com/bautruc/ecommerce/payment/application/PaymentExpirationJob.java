package com.bautruc.ecommerce.payment.application;

import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.payment.infrastructure.PaymentJpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PaymentExpirationJob {
    private static final Logger log=LoggerFactory.getLogger(PaymentExpirationJob.class);
    private final PaymentJpaRepository payments;private final PaymentExpirationWorker worker;private final BusinessClock clock;
    public PaymentExpirationJob(PaymentJpaRepository p,PaymentExpirationWorker w,BusinessClock c){payments=p;worker=w;clock=c;}
    @Scheduled(fixedDelayString="${bautruc.payos.expiration-scan-ms:30000}") public void run(){payments.findExpiredCandidateIds(clock.now(),100).forEach(id->{try{worker.expire(id);}catch(RuntimeException exception){log.error("Could not expire payment id={}",id,exception);}});}
}
