package com.kahnhuseynov.ecommercebackend.product.repository;

import com.kahnhuseynov.ecommercebackend.product.dto.ProductSearchCriteria;
import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withCriteria(ProductSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.search() != null && !criteria.search().isBlank()) {
                String pattern = "%" + criteria.search().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)
                ));
            }
            if (criteria.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), criteria.categoryId()));
            }
            if (criteria.active() != null) {
                predicates.add(builder.equal(root.get("active"), criteria.active()));
            }
            if (criteria.minPrice() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
