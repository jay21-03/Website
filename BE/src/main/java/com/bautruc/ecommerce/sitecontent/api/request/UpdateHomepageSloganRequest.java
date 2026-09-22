package com.bautruc.ecommerce.sitecontent.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHomepageSloganRequest(
        @NotBlank @Size(max = 255) String sloganVi,
        @NotBlank @Size(max = 255) String sloganEn
) {
}
