package com.bautruc.ecommerce.chat.api.request;
import jakarta.validation.constraints.*;
public record ChatMessageRequest(@NotBlank @Size(max=2000) String body,@Size(max=100) String guestName,@Email @Size(max=320) String guestEmail) {}
