package com.bautruc.ecommerce.commerce.application;

import com.bautruc.ecommerce.commerce.infrastructure.CheckoutOperationJpaRepository;
import com.bautruc.ecommerce.common.time.BusinessClock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CheckoutFinalizationRecoveryJob {
    private static final Logger log=LoggerFactory.getLogger(CheckoutFinalizationRecoveryJob.class);
    private final CheckoutOperationJpaRepository operations;private final CheckoutRecoveryService recovery;private final BusinessClock clock;
    public CheckoutFinalizationRecoveryJob(CheckoutOperationJpaRepository o,CheckoutRecoveryService r,BusinessClock c){operations=o;recovery=r;clock=c;}
    @Scheduled(fixedDelayString="${bautruc.payos.checkout-recovery-ms:15000}") public void run(){java.util.List<Long> ids=new java.util.ArrayList<>(operations.findCandidateIds("PAYOS_CREATED",50));ids.addAll(operations.findStaleCreatingIds(clock.now().minusSeconds(30),Math.max(0,50-ids.size())));ids.forEach(id->{try{recovery.recover(id);}catch(RuntimeException exception){log.error("Could not recover checkout operation id={}",id,exception);}});}
}
