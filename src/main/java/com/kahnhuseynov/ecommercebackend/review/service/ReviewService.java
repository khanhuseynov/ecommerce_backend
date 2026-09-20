package com.kahnhuseynov.ecommercebackend.review.service;

import com.kahnhuseynov.ecommercebackend.review.dto.*;
import org.springframework.data.domain.*;

public interface ReviewService {
    ReviewResponse create(Long userId, Long productId, ReviewRequest request);
    ReviewResponse update(Long userId, Long productId, Long reviewId, ReviewRequest request);
    void delete(Long userId, boolean admin, Long productId, Long reviewId);
    ReviewResponse get(Long productId, Long reviewId);
    Page<ReviewResponse> getAll(Long productId, Pageable pageable);
}
