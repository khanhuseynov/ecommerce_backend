package com.kahnhuseynov.ecommercebackend.product.repository;

import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCategoryId(Long categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findWithLockById(Long id);
}
