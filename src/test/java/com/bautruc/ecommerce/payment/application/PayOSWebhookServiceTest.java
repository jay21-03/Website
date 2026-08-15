package com.bautruc.ecommerce.payment.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.payment.infrastructure.PayOSSignatureService;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class PayOSWebhookServiceTest {
    private static final String SECRET="test-secret";
    private final ObjectMapper json=new ObjectMapper();
    @Test void verifiesCanonicalWebhookBeforeDispatching()throws Exception{PaymentResultPort port=mock(PaymentResultPort.class);BusinessClock clock=mock(BusinessClock.class);when(clock.now()).thenReturn(Instant.parse("2026-08-14T08:00:00Z"));PayOSWebhookService service=new PayOSWebhookService(signatures(),port,clock);JsonNode body=payload();service.handle(body);verify(port).processSuccess(argThat(e->e.orderId()==123L&&e.amount()==3000&&"REF-1".equals(e.reference())),eq(Instant.parse("2026-08-14T08:00:00Z")));}
    @Test void rejectsInvalidSignature()throws Exception{PayOSWebhookService service=new PayOSWebhookService(signatures(),mock(PaymentResultPort.class),mock(BusinessClock.class));ObjectNode body=(ObjectNode)payload();body.put("signature","00");assertThatThrownBy(()->service.handle(body)).isInstanceOf(BusinessException.class).extracting(e->((BusinessException)e).code()).isEqualTo(PaymentErrorCodes.PAYOS_WEBHOOK_INVALID);}
    private PayOSSignatureService signatures(){ApplicationProperties p=new ApplicationProperties("http://front",List.of("http://front"),"",List.of(),new ApplicationProperties.Jwt("","",1,"HS256"),new ApplicationProperties.Payos("","",SECRET,"http://pay"),new ApplicationProperties.Aws("",new ApplicationProperties.S3("","")),new ApplicationProperties.Image(1),new ApplicationProperties.Auth(""));return new PayOSSignatureService(p);}
    private JsonNode payload()throws Exception{ObjectNode data=json.createObjectNode();data.put("orderCode",123);data.put("amount",3000);data.put("currency","VND");data.put("paymentLinkId","LINK-1");data.put("reference","REF-1");data.put("code","00");ObjectNode root=json.createObjectNode();root.put("code","00");root.put("success",true);root.set("data",data);root.put("signature",sign(data));return root;}
    private String sign(JsonNode data)throws Exception{List<String> names=new ArrayList<>();data.fieldNames().forEachRemaining(names::add);Collections.sort(names);String value=names.stream().map(n->n+"="+(data.get(n).isTextual()?data.get(n).asText():data.get(n).asText())).collect(java.util.stream.Collectors.joining("&"));Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}
}
