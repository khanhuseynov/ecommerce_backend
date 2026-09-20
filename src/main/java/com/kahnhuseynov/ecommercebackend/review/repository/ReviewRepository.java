package com.kahnhuseynov.ecommercebackend.review.repository;

import com.kahnhuseynov.ecommercebackend.review.entity.Review;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @EntityGraph(attributePaths = "user")
    Page<Review> findAllByProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<Review> findByIdAndProductId(Long id, Long productId);
}
