package com.bautruc.ecommerce.payment.application;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.payment.infrastructure.PayOSSignatureService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PayOSWebhookService {
    private final PayOSSignatureService signatures;private final PaymentResultPort results;private final BusinessClock clock;
    public PayOSWebhookService(PayOSSignatureService s,PaymentResultPort r,BusinessClock c){signatures=s;results=r;clock=c;}
    public void handle(JsonNode body){java.time.Instant received=clock.now();JsonNode data=body==null?null:body.get("data");String signature=body!=null&&body.hasNonNull("signature")?body.get("signature").asText():null;if(!signatures.verify(data,signature))throw invalid("Invalid payOS webhook signature.");boolean success=body.path("success").asBoolean(false)&&"00".equals(data.path("code").asText());if(!success)return;Long orderId=data.hasNonNull("orderCode")?data.get("orderCode").asLong():null;if(orderId==null||orderId<=0)throw invalid("Invalid payOS order code.");VerifiedPayOSEvent event=new VerifiedPayOSEvent(orderId,data.path("amount").asLong(),data.path("currency").asText(),text(data,"paymentLinkId"),text(data,"reference"),null,true);results.processSuccess(event,received);}
    private String text(JsonNode n,String f){return n.hasNonNull(f)?n.get(f).asText():null;}
    private BusinessException invalid(String m){return new BusinessException(PaymentErrorCodes.PAYOS_WEBHOOK_INVALID,m,HttpStatus.BAD_REQUEST);}
}

