package com.bautruc.ecommerce.commerce.application;

import com.bautruc.ecommerce.payment.application.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;

@Service
public class OrderCancellationApplicationService {
    private static final Logger log=LoggerFactory.getLogger(OrderCancellationApplicationService.class);
    private final OrderCancellationTransactionService local;private final PayOSClient payos;
    public OrderCancellationApplicationService(OrderCancellationTransactionService l,PayOSClient p){local=l;payos=p;}
    public void cancel(Long orderId,Long adminId){ProviderCancellation c=local.cancel(orderId);if(c.required())try{payos.cancelPaymentRequest(c.providerId(),c.reason());}catch(PayOSRequestException e){log.warn("event=PAYOS_PROVIDER_CANCEL_FAILED orderId={} adminId={} result=failed",orderId,adminId,e);}}
}

