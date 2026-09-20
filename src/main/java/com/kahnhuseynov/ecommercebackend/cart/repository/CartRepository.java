package com.kahnhuseynov.ecommercebackend.cart.repository;

import com.kahnhuseynov.ecommercebackend.cart.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.user.id = :userId")
    Optional<Cart> findWithLockByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserId(Long userId);
}
