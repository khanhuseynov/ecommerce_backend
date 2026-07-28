package com.kahnhuseynov.ecommercebackend.order.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kahnhuseynov.ecommercebackend.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderResponse(
        Long id,
        OrderStatus status,
        String shippingAddress,
        List<OrderItemResponse> items,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
