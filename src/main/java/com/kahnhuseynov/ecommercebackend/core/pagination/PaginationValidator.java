package com.kahnhuseynov.ecommercebackend.core.pagination;

import com.kahnhuseynov.ecommercebackend.core.exception.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

public final class PaginationValidator {

    private PaginationValidator() {
    }

    public static Pageable normalizeSort(Pageable pageable, Map<String, String> allowedFields) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        Sort normalizedSort = Sort.by(pageable.getSort().stream().map(order -> {
            String entityField = allowedFields.get(order.getProperty());
            if (entityField == null) {
                throw new InvalidRequestException(
                        "Unsupported sort field '%s'. Allowed fields: %s."
                                .formatted(order.getProperty(), String.join(", ", allowedFields.keySet()))
                );
            }
            return new Sort.Order(order.getDirection(), entityField);
        }).toList());

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizedSort);
    }
}
