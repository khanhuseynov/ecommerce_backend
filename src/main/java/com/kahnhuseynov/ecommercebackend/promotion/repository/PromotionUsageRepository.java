package com.kahnhuseynov.ecommercebackend.promotion.repository;

import com.kahnhuseynov.ecommercebackend.promotion.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    long countByPromotionIdAndUserId(Long promotionId, Long userId);
}
