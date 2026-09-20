package com.kahnhuseynov.ecommercebackend.order.repository;

import com.kahnhuseynov.ecommercebackend.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByUserIdAndStatusNotAndItems_Product_Id(Long userId,
            com.kahnhuseynov.ecommercebackend.order.entity.OrderStatus status, Long productId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Page<Order> findAllByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
