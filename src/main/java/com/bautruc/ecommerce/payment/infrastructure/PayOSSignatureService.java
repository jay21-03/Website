package com.bautruc.ecommerce.payment.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class PayOSSignatureService {
    private final byte[] key;
    public PayOSSignatureService(ApplicationProperties properties){this.key=java.util.Objects.toString(properties.payos().checksumKey(),"").getBytes(StandardCharsets.UTF_8);}
    public String create(long amount,String cancelUrl,String description,long orderCode,String returnUrl){
        return sign("amount="+amount+"&cancelUrl="+cancelUrl+"&description="+description+"&orderCode="+orderCode+"&returnUrl="+returnUrl);
    }
    public boolean verify(JsonNode data,String signature){
        if(data==null||!data.isObject()||signature==null)return false;
        List<String> names=new ArrayList<>();data.fieldNames().forEachRemaining(names::add);Collections.sort(names);
        String canonical=names.stream().map(n->n+"="+value(data.get(n))).collect(java.util.stream.Collectors.joining("&"));
        return MessageDigest.isEqual(sign(canonical).getBytes(StandardCharsets.US_ASCII),signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
    private String value(JsonNode n){if(n==null||n.isNull())return "";if(n.isTextual())return n.textValue();return n.isValueNode()?n.asText():n.toString();}
    private String sign(String value){if(key.length==0)throw new IllegalStateException("PAYOS_CHECKSUM_KEY is required");try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Cannot create payOS signature",e);}}
}
