package com.kahnhuseynov.ecommercebackend.promotion.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kahnhuseynov.ecommercebackend.promotion.entity.DiscountType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PromotionRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 50) @Pattern(regexp = "[a-zA-Z0-9_-]+") String code,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal discountValue,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal minimumOrderAmount,
        @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal maximumDiscountAmount,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        @NotNull Boolean active,
        @Positive Integer usageLimit,
        @Positive Integer perUserLimit,
        @Positive Long productId,
        @Positive Long categoryId
) {}
