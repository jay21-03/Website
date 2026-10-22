package com.bautruc.ecommerce.commerce.application;public record NormalizedCheckoutRequest(String receiverName,String phone,String email,String address,String note,Long productId,Integer quantity){
 public NormalizedCheckoutRequest(String receiverName,String phone,String email,String address,String note){this(receiverName,phone,email,address,note,null,null);}
 public boolean isBuyNow(){return productId!=null;}
}
