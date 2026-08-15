package com.bautruc.ecommerce.payment.infrastructure;

import java.net.SocketTimeoutException;
import java.util.Optional;
import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.payment.application.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class PayOSRestClient implements PayOSClient {
    private final RestClient client; private final PayOSSignatureService signatures;
    public PayOSRestClient(ApplicationProperties p,PayOSSignatureService s){
        String base=p.payos().baseUrl();if(base==null||base.isBlank())base="https://api-merchant.payos.vn";
        this.client=RestClient.builder().baseUrl(base).defaultHeader("x-client-id",java.util.Objects.toString(p.payos().clientId(),""))
                .defaultHeader("x-api-key",java.util.Objects.toString(p.payos().apiKey(),"")).build();signatures=s;
    }
    @Override public PayOSPaymentResult createPaymentRequest(PayOSCreatePaymentCommand c){
        CreateBody body=new CreateBody(c.orderId(),c.amount(),c.description(),c.cancelUrl(),c.returnUrl(),c.expiresAt().getEpochSecond(),signatures.create(c.amount(),c.cancelUrl(),c.description(),c.orderId(),c.returnUrl()));
        try{return map(client.post().uri("/v2/payment-requests").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class));}
        catch(ResourceAccessException e){throw new PayOSRequestException("Ambiguous payOS create result",true,e);}catch(RestClientResponseException e){throw new PayOSRequestException("payOS create failed",e.getStatusCode().value()==429||e.getStatusCode().is5xxServerError(),e);}catch(RestClientException e){throw new PayOSRequestException("payOS create failed",true,e);}
    }
    @Override public Optional<PayOSPaymentResult> getPaymentRequest(Long orderId){try{return Optional.of(map(client.get().uri("/v2/payment-requests/{id}",orderId).retrieve().body(JsonNode.class)));}catch(HttpClientErrorException.NotFound e){return Optional.empty();}catch(RestClientException e){throw new PayOSRequestException("payOS lookup failed",true,e);}}
    @Override public void cancelPaymentRequest(String id,String reason){try{client.post().uri("/v2/payment-requests/{id}/cancel",id).contentType(MediaType.APPLICATION_JSON).body(new CancelBody(reason)).retrieve().toBodilessEntity();}catch(RestClientException e){throw new PayOSRequestException("payOS cancellation failed",false,e);}}
    private PayOSPaymentResult map(JsonNode r){if(r==null||!"00".equals(r.path("code").asText())||!r.has("data")||!signatures.verify(r.get("data"),r.path("signature").asText()))throw new PayOSRequestException("Invalid payOS response",false,null);JsonNode d=r.get("data");String link=d.hasNonNull("paymentLinkId")?d.get("paymentLinkId").asText():d.path("id").asText(null);return new PayOSPaymentResult(d.path("orderCode").asLong(),d.path("amount").asLong(),link,d.path("status").asText(),d.path("checkoutUrl").asText(null),d.path("qrCode").asText(null));}
    record CreateBody(Long orderCode,long amount,String description,String cancelUrl,String returnUrl,long expiredAt,String signature){}
    record CancelBody(String cancellationReason){}
}
