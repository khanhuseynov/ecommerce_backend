package com.kahnhuseynov.ecommercebackend.order.repository;

import com.kahnhuseynov.ecommercebackend.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
