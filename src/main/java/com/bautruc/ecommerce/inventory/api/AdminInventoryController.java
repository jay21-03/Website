package com.bautruc.ecommerce.inventory.api;
import com.bautruc.ecommerce.common.logging.LogContext;import com.bautruc.ecommerce.common.response.*;import com.bautruc.ecommerce.common.time.BusinessClock;import com.bautruc.ecommerce.inventory.api.request.AdjustInventoryRequest;import com.bautruc.ecommerce.inventory.api.response.*;import com.bautruc.ecommerce.inventory.application.*;import com.bautruc.ecommerce.inventory.domain.InventoryAvailabilityStatus;
import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;
@RestController
public class AdminInventoryController{
 private final InventoryQueryService queries;private final InventoryAdminService admin;private final BusinessClock clock;public AdminInventoryController(InventoryQueryService q,InventoryAdminService a,BusinessClock c){queries=q;admin=a;clock=c;}
 @GetMapping("/api/v1/admin/inventory") public ApiResponse<PageResponse<InventoryListItemResponse>> list(@RequestParam(required=false)String keyword,@RequestParam(required=false)InventoryAvailabilityStatus status,@RequestParam(required=false)Integer page,@RequestParam(required=false)Integer size,@RequestParam(required=false)String sort){return ok(PageResponse.from(queries.list(keyword,status,page,size,sort).map(InventoryListItemResponse::from)));}
 @GetMapping("/api/v1/admin/inventory/{productId}/transactions") public ApiResponse<PageResponse<InventoryTransactionResponse>> history(@PathVariable Long productId,@RequestParam(required=false)Integer page,@RequestParam(required=false)Integer size){return ok(PageResponse.from(queries.history(productId,page,size).map(InventoryTransactionResponse::from)));}
 @PostMapping("/api/v1/admin/inventory/{productId}/adjust") public ApiResponse<InventoryListItemResponse> adjust(@PathVariable Long productId,@Valid @RequestBody AdjustInventoryRequest r){return ok(InventoryListItemResponse.from(admin.adjust(productId,r)));}
 private <T>ApiResponse<T> ok(T d){return ApiResponse.success(d,null,clock.businessNow().toOffsetDateTime(),LogContext.currentCorrelationId());}
}
