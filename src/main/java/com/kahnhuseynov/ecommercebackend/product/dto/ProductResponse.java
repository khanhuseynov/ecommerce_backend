package com.kahnhuseynov.ecommercebackend.product.dto;

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

    Boolean active,

    LocalDateTime createdAt,

    LocalDateTime updatedAt
) {
}
