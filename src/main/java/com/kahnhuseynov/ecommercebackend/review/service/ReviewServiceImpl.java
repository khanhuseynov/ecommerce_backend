package com.kahnhuseynov.ecommercebackend.review.service;

import com.kahnhuseynov.ecommercebackend.core.exception.*;
import com.kahnhuseynov.ecommercebackend.core.pagination.PaginationValidator;
import com.kahnhuseynov.ecommercebackend.order.entity.OrderStatus;
import com.kahnhuseynov.ecommercebackend.order.repository.OrderRepository;
import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository;
import com.kahnhuseynov.ecommercebackend.review.dto.*;
import com.kahnhuseynov.ecommercebackend.review.entity.Review;
import com.kahnhuseynov.ecommercebackend.review.repository.ReviewRepository;
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id", "rating", "rating", "created_at", "createdAt", "createdAt", "createdAt",
            "updated_at", "updatedAt", "updatedAt", "updatedAt");
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public ReviewResponse create(Long userId, Long productId, ReviewRequest request) {
        // All review writes lock the product first, serializing duplicate checks and changes.
        Product product = lockProduct(productId);
        if (!orderRepository.existsByUserIdAndStatusNotAndItems_Product_Id(userId, OrderStatus.CANCELLED, productId)) {
            throw new BusinessException("Only users who ordered this product can review it.");
        }
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException("You have already reviewed this product.");
        }
        Review review = new Review();
        review.setProduct(product);
        review.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found.")));
        setContent(review, request);
        return ReviewResponse.from(reviewRepository.saveAndFlush(review));
    }

    @Override
    public ReviewResponse update(Long userId, Long productId, Long reviewId, ReviewRequest request) {
        lockProduct(productId);
        Review review = find(productId, reviewId);
        requireOwner(review, userId);
        setContent(review, request);
        return ReviewResponse.from(reviewRepository.saveAndFlush(review));
    }

    @Override
    public void delete(Long userId, boolean admin, Long productId, Long reviewId) {
        lockProduct(productId);
        Review review = find(productId, reviewId);
        if (!admin) requireOwner(review, userId);
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse get(Long productId, Long reviewId) {
        return ReviewResponse.from(find(productId, reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAll(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) throw new ResourceNotFoundException("Product not found.");
        Pageable normalized = PaginationValidator.normalizeSort(pageable, SORT_FIELDS);
        if (normalized.getSort().getOrderFor("id") == null) {
            normalized = PageRequest.of(normalized.getPageNumber(), normalized.getPageSize(),
                    normalized.getSort().and(Sort.by(Sort.Direction.DESC, "id")));
        }
        return reviewRepository.findAllByProductId(productId, normalized).map(ReviewResponse::from);
    }

    private Product lockProduct(Long id) {
        return productRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private Review find(Long productId, Long reviewId) {
        return reviewRepository.findByIdAndProductId(reviewId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
    }

    private void requireOwner(Review review, Long userId) {
        if (!review.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Review not found.");
        }
    }

    private void setContent(Review review, ReviewRequest request) {
        review.setRating(request.rating());
        review.setComment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
    }
}
