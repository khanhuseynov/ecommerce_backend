package com.kahnhuseynov.ecommercebackend.order.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CheckoutRequest(
        @NotBlank(message = "Shipping address cannot be empty")
        @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
        String shippingAddress
) {
}
