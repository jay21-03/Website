package com.bautruc.ecommerce.sitecontent.api.request;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record UpdateHomepageCopyRequest(
        @NotNull Map<String, String> copy
) {
}
