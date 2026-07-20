package com.kahnhuseynov.ecommercebackend.cart.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CartResponse(Long id, List<CartItemResponse> items, Integer totalQuantity, BigDecimal totalPrice) {}
