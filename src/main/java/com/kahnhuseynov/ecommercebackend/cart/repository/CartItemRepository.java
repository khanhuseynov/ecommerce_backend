package com.kahnhuseynov.ecommercebackend.cart.repository;

import com.kahnhuseynov.ecommercebackend.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);
}
