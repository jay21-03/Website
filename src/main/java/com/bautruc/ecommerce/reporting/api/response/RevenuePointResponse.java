package com.bautruc.ecommerce.reporting.api.response;

import java.time.LocalDate;

public record RevenuePointResponse(LocalDate periodStart, long revenue) {}
