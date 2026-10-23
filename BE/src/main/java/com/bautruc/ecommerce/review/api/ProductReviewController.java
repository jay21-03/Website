package com.bautruc.ecommerce.review.api;
import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.*;
import com.bautruc.ecommerce.common.security.CurrentUserProvider;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.review.api.request.*;
import com.bautruc.ecommerce.review.application.ProductReviewService;
import com.bautruc.ecommerce.review.application.ProductReviewService.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
@RestController public class ProductReviewController {
 private final ProductReviewService reviews; private final CurrentUserProvider current; private final BusinessClock clock;
 public ProductReviewController(ProductReviewService reviews,CurrentUserProvider current,BusinessClock clock){this.reviews=reviews;this.current=current;this.clock=clock;}
 @GetMapping("/api/v1/products/{productId}/reviews") public ApiResponse<ReviewPage> list(@PathVariable Long productId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ok(reviews.publicReviews(productId,page,size));}
 @GetMapping("/api/v1/me/products/{productId}/review") public ApiResponse<Mine> mine(@PathVariable Long productId){return ok(reviews.mine(userId(),productId));}
 @PostMapping("/api/v1/me/products/{productId}/review") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<ReviewView> create(@PathVariable Long productId,@Valid @RequestBody ReviewRequest req){return ok(reviews.create(userId(),productId,req));}
 @PutMapping("/api/v1/me/products/{productId}/review") public ApiResponse<ReviewView> update(@PathVariable Long productId,@Valid @RequestBody ReviewRequest req){return ok(reviews.update(userId(),productId,req));}
 @GetMapping("/api/v1/admin/reviews") public ApiResponse<PageResponse<AdminReviewView>> admin(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ok(reviews.adminReviews(page,size));}
 @PatchMapping("/api/v1/admin/reviews/{id}/visibility") public ApiResponse<AdminReviewView> visibility(@PathVariable Long id,@Valid @RequestBody ReviewVisibilityRequest req){return ok(reviews.setHidden(id,req.hidden()));}
 private Long userId(){return current.currentUser().orElseThrow(()->new AccessDeniedException("Authentication required")).userId();}
 private <T> ApiResponse<T> ok(T data){return ApiResponse.success(data,null,clock.businessNow().toOffsetDateTime(),LogContext.currentCorrelationId());}
}
