package com.bautruc.ecommerce.sitecontent.api.request;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SitePresentationRequest(
        @NotNull Map<String, @Valid Map<String, @NotNull @Valid TextPair>> pages,
        @NotNull @Valid CheckoutLayout checkout,
        @NotBlank String colorPreset,
        @NotBlank String fontPreset
) {
    public record TextPair(
            @NotBlank @Size(max = 5000) String vi,
            @NotBlank @Size(max = 5000) String en,
            String font,
            String color
    ) {}

    public record CheckoutLayout(
            @NotNull List<String> fieldOrder,
            @NotNull List<String> sectionOrder,
            boolean showNote,
            boolean showEmail
    ) {}
}
