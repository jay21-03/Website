package com.bautruc.ecommerce.commerce.api.request;
import jakarta.validation.constraints.*;public record CheckoutRequest(@NotBlank @Size(max=255)String receiverName,@NotBlank @Size(min=8,max=32)String phone,@Email @Size(max=320)String email,@NotBlank @Size(max=500)String address,@Size(max=1000)String note){}
