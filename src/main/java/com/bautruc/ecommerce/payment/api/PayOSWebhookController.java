package com.bautruc.ecommerce.payment.api;

import java.util.Map;
import com.bautruc.ecommerce.payment.application.PayOSWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PayOSWebhookController {
    private final PayOSWebhookService service;
    public PayOSWebhookController(PayOSWebhookService s){service=s;}
    @PostMapping("/api/v1/payments/webhook/payos") public ResponseEntity<Map<String,Object>> webhook(@RequestBody JsonNode body){service.handle(body);return ResponseEntity.ok(Map.of("success",true));}
}

