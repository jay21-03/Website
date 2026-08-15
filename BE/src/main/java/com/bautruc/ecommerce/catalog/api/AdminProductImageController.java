package com.bautruc.ecommerce.catalog.api;

import java.util.List;

import com.bautruc.ecommerce.catalog.api.request.ReorderProductImagesRequest;
import com.bautruc.ecommerce.catalog.api.response.ProductImageResponse;
import com.bautruc.ecommerce.catalog.application.ProductImageService;
import com.bautruc.ecommerce.catalog.domain.ProductImage;
import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/products/{productId}/images")
public class AdminProductImageController {
    private final ProductImageService service;
    private final BusinessClock clock;

    public AdminProductImageController(ProductImageService service, BusinessClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductImageResponse> upload(@PathVariable Long productId,
                                                     @RequestPart("file") MultipartFile file) {
        return ok(response(service.upload(productId, file)));
    }

    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> delete(@PathVariable Long productId, @PathVariable Long imageId) {
        service.delete(productId, imageId);
        return ok(null);
    }

    @PutMapping("/{imageId}/thumbnail")
    public ApiResponse<ProductImageResponse> thumbnail(@PathVariable Long productId, @PathVariable Long imageId) {
        return ok(response(service.setThumbnail(productId, imageId)));
    }

    @PutMapping("/order")
    public ApiResponse<List<ProductImageResponse>> reorder(@PathVariable Long productId,
                                                           @Valid @RequestBody ReorderProductImagesRequest request) {
        return ok(service.reorder(productId, request.imageIds()).stream().map(this::response).toList());
    }

    private ProductImageResponse response(ProductImage image) {
        return ProductImageResponse.from(image, service.publicUrl(image));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
