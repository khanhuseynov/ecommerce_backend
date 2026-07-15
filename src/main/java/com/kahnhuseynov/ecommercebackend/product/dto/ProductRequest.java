package com.kahnhuseynov.ecommercebackend.product.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProductRequest(

    @NotBlank(message = "Product name cannot be empty")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    String name,

    @Size(max = 1000, message = "Product description cannot exceed 1000 characters")
    String description,

    @NotNull(message = "Product price cannot be empty")
    @DecimalMin(value = "0.01", message = "Product price must be greater than 0")
    BigDecimal price,

    @NotNull(message = "Stock quantity cannot be empty")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    Integer stockQuantity,

    Boolean active
) {
}
