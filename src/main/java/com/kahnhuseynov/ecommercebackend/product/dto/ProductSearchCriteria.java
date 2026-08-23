package com.kahnhuseynov.ecommercebackend.product.dto;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        String search,
        Long categoryId,
        Boolean active,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
