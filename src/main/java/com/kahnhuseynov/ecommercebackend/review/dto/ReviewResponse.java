package com.kahnhuseynov.ecommercebackend.review.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kahnhuseynov.ecommercebackend.review.entity.Review;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReviewResponse(Long id, Long productId, Long userId, String reviewerName,
                             Integer rating, String comment, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getProduct().getId(), review.getUser().getId(),
                review.getUser().getFirstName(), review.getRating(), review.getComment(),
                review.getCreatedAt(), review.getUpdatedAt());
    }
}
