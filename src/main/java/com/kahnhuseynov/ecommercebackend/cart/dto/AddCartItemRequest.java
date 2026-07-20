package com.kahnhuseynov.ecommercebackend.cart.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AddCartItemRequest(
        @NotNull(message = "Product cannot be empty") Long productId,
        @NotNull(message = "Quantity cannot be empty") @Positive(message = "Quantity must be greater than 0") Integer quantity
) {}
