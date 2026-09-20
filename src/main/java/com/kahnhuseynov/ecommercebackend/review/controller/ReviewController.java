package com.kahnhuseynov.ecommercebackend.review.controller;

import com.kahnhuseynov.ecommercebackend.core.dto.PageResponse;
import com.kahnhuseynov.ecommercebackend.core.security.CustomUserPrincipal;
import com.kahnhuseynov.ecommercebackend.review.dto.*;
import com.kahnhuseynov.ecommercebackend.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public PageResponse<ReviewResponse> getAll(@PathVariable Long productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(reviewService.getAll(productId, pageable));
    }

    @GetMapping("/{reviewId}")
    public ReviewResponse get(@PathVariable Long productId, @PathVariable Long reviewId) {
        return reviewService.get(productId, reviewId);
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(principal.getId(), productId, request));
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse update(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long productId,
            @PathVariable Long reviewId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.update(principal.getId(), productId, reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long productId, @PathVariable Long reviewId) {
        boolean admin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reviewService.delete(principal.getId(), admin, productId, reviewId);
        return ResponseEntity.noContent().build();
    }
}
