package com.kahnhuseynov.ecommercebackend.promotion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "promotion_usages")
@Getter @Setter @NoArgsConstructor
public class PromotionUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long promotionId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, unique = true)
    private Long orderId;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;
}
