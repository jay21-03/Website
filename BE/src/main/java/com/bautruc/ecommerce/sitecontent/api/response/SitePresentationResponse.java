package com.bautruc.ecommerce.sitecontent.api.response;

import java.util.Map;
import com.bautruc.ecommerce.sitecontent.api.request.SitePresentationRequest;

public record SitePresentationResponse(
        SitePresentationRequest content,
        Map<String, String> media
) {}
