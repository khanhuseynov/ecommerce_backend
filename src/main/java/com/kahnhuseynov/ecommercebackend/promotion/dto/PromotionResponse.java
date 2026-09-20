package com.kahnhuseynov.ecommercebackend.promotion.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kahnhuseynov.ecommercebackend.promotion.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PromotionResponse(Long id, String name, String code, DiscountType discountType,
        BigDecimal discountValue, BigDecimal minimumOrderAmount, BigDecimal maximumDiscountAmount,
        LocalDateTime startsAt, LocalDateTime endsAt, boolean active, Integer usageLimit,
        Integer perUserLimit, long usageCount, Long productId, Long categoryId) {
    public static PromotionResponse from(Promotion p) {
        return new PromotionResponse(p.getId(), p.getName(), p.getCode(), p.getDiscountType(),
                p.getDiscountValue(), p.getMinimumOrderAmount(), p.getMaximumDiscountAmount(),
                p.getStartsAt(), p.getEndsAt(), p.isActive(), p.getUsageLimit(), p.getPerUserLimit(),
                p.getUsageCount(), p.getProductId(), p.getCategoryId());
    }
}
