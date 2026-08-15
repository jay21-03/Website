package com.bautruc.ecommerce.commerce.api;

import com.bautruc.ecommerce.commerce.api.request.RecordManualRefundRequest;
import com.bautruc.ecommerce.commerce.application.*;
import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.security.*;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.order.api.response.OrderDetailResponse;
import com.bautruc.ecommerce.order.application.OrderQueryService;
import com.bautruc.ecommerce.payment.api.response.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminOrderWorkflowController {
    private final OrderCancellationApplicationService cancellations;private final ManualRefundApplicationService refunds;private final OrderQueryService orders;private final CurrentUserProvider current;private final BusinessClock clock;
    public AdminOrderWorkflowController(OrderCancellationApplicationService c,ManualRefundApplicationService r,OrderQueryService o,CurrentUserProvider u,BusinessClock b){cancellations=c;refunds=r;orders=o;current=u;clock=b;}
    @PostMapping("/api/v1/admin/orders/{id}/cancel") public ApiResponse<OrderDetailResponse> cancel(@PathVariable Long id){cancellations.cancel(id,admin());return ok(orders.adminDetail(id));}
    @PostMapping("/api/v1/admin/payments/{id}/manual-refund") public ApiResponse<PaymentResponse> refund(@PathVariable Long id,@Valid @RequestBody RecordManualRefundRequest request){return ok(PaymentResponse.from(refunds.record(id,admin(),request.note())));}
    private Long admin(){return current.currentUser().orElseThrow(()->new AccessDeniedException("Authentication required")).userId();}
    private <T>ApiResponse<T> ok(T data){return ApiResponse.success(data,null,clock.businessNow().toOffsetDateTime(),LogContext.currentCorrelationId());}
}

