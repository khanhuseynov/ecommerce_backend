package com.kahnhuseynov.ecommercebackend.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity cannot be empty") @Positive(message = "Quantity must be greater than 0") Integer quantity
) {}
