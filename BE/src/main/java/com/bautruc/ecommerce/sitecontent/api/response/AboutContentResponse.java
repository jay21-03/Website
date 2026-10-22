package com.bautruc.ecommerce.sitecontent.api.response;

import com.bautruc.ecommerce.sitecontent.api.request.AboutContentRequest;

public record AboutContentResponse(
        AboutContentRequest content,
        String heroUrl,
        String portraitUrl,
        String certificateUrl
) {}
