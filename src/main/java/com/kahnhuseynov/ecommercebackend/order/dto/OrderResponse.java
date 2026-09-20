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
        LocalDateTime updatedAt,
        BigDecimal originalTotalPrice,
        BigDecimal discountAmount,
        String couponCode,
        String promotionName
) {
    public OrderResponse(Long id, OrderStatus status, String shippingAddress, List<OrderItemResponse> items,
                         BigDecimal totalPrice, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, status, shippingAddress, items, totalPrice, createdAt, updatedAt,
                totalPrice, BigDecimal.ZERO, null, null);
    }
}
