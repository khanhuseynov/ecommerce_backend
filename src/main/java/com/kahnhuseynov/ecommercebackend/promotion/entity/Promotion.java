package com.kahnhuseynov.ecommercebackend.promotion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter @Setter @NoArgsConstructor
public class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(unique = true, length = 50)
    private String code;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private DiscountType discountType;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount;
    @Column(precision = 12, scale = 2)
    private BigDecimal maximumDiscountAmount;
    @Column(nullable = false)
    private LocalDateTime startsAt;
    @Column(nullable = false)
    private LocalDateTime endsAt;
    @Column(nullable = false)
    private boolean active;
    private Integer usageLimit;
    private Integer perUserLimit;
    @Column(nullable = false)
    private long usageCount;
    private Long productId;
    private Long categoryId;
}
