package com.kahnhuseynov.ecommercebackend.cart.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CartItemResponse(Long id, Long productId, String productName, BigDecimal unitPrice,
                               Integer quantity, BigDecimal subtotal) {
}
