package com.bautruc.ecommerce.cart.api;
import com.bautruc.ecommerce.cart.api.request.*;import com.bautruc.ecommerce.cart.api.response.CartResponse;import com.bautruc.ecommerce.cart.application.CartApplicationService;import com.bautruc.ecommerce.common.logging.LogContext;import com.bautruc.ecommerce.common.response.ApiResponse;import com.bautruc.ecommerce.common.time.BusinessClock;
import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/cart")
public class CartController{
 private final CartApplicationService service;private final BusinessClock clock;public CartController(CartApplicationService s,BusinessClock c){service=s;clock=c;}
 @GetMapping public ApiResponse<CartResponse> view(){return ok(service.view());}
 @PostMapping("/items") public ApiResponse<CartResponse> add(@Valid @RequestBody AddCartItemRequest r){return ok(service.add(r.productId(),r.quantity()));}
 @PatchMapping("/items/{itemId}") public ApiResponse<CartResponse> update(@PathVariable Long itemId,@Valid @RequestBody UpdateCartItemRequest r){return ok(service.update(itemId,r.quantity()));}
 @DeleteMapping("/items/{itemId}") public ApiResponse<CartResponse> remove(@PathVariable Long itemId){return ok(service.remove(itemId));}
 private <T>ApiResponse<T> ok(T d){return ApiResponse.success(d,null,clock.businessNow().toOffsetDateTime(),LogContext.currentCorrelationId());}
}
