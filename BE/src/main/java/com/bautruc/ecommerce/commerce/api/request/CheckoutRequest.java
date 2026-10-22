package com.bautruc.ecommerce.commerce.api.request;
import jakarta.validation.constraints.*;public record CheckoutRequest(@NotBlank @Size(max=255)String receiverName,@NotBlank @Size(min=8,max=32)String phone,@Email @Size(max=320)String email,@NotBlank @Size(max=500)String address,@Size(max=1000)String note,@Positive Long productId,@Min(1) @Max(99) Integer quantity){
 public CheckoutRequest(String receiverName,String phone,String email,String address,String note){this(receiverName,phone,email,address,note,null,null);}
}
