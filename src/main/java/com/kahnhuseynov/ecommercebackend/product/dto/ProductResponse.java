package com.kahnhuseynov.ecommercebackend.product.dto;

import com.kahnhuseynov.ecommercebackend.category.dto.CategoryResponse;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProductResponse(

    Long id,

    String name,

    String description,

    BigDecimal price,

    Integer stockQuantity,

    CategoryResponse category,

    Boolean active,

    LocalDateTime createdAt,

    LocalDateTime updatedAt,

    BigDecimal averageRating,

    Long reviewCount
) {
    public ProductResponse(Long id, String name, String description, BigDecimal price,
                           Integer stockQuantity, CategoryResponse category, Boolean active,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, description, price, stockQuantity, category, active, createdAt, updatedAt,
                BigDecimal.ZERO, 0L);
    }
}
