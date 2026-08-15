package com.bautruc.ecommerce.payment.application;
public class PayOSRequestException extends RuntimeException {
    private final boolean ambiguous;
    public PayOSRequestException(String message,boolean ambiguous,Throwable cause){super(message,cause);this.ambiguous=ambiguous;}
    public boolean ambiguous(){return ambiguous;}
}

