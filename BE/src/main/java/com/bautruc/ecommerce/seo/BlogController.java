package com.bautruc.ecommerce.seo;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class BlogController {
    private final BlogService service;
    private final BusinessClock clock;

    public BlogController(BlogService service, BusinessClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/api/v1/blog") public ApiResponse<List<BlogService.Post>> publicPosts() { return ok(service.publicPosts()); }
    @GetMapping("/api/v1/blog/{slug}") public ApiResponse<BlogService.Post> publicPost(@PathVariable String slug) { return ok(service.publicPost(slug)); }
    @GetMapping("/api/v1/admin/blog") public ApiResponse<List<BlogService.Post>> adminPosts() { return ok(service.adminPosts()); }
    @PostMapping("/api/v1/admin/blog") public ApiResponse<BlogService.Post> create(@Valid @RequestBody BlogService.Input value) { return ok(service.save(null, value)); }
    @PutMapping("/api/v1/admin/blog/{id}") public ApiResponse<BlogService.Post> update(@PathVariable Long id,
            @Valid @RequestBody BlogService.Input value) { return ok(service.save(id, value)); }
    @PutMapping(value = "/api/v1/admin/blog/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BlogService.Post> uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws java.io.IOException {
        return ok(service.uploadImage(id, file));
    }

    private <T> ApiResponse<T> ok(T value) {
        return ApiResponse.success(value, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
