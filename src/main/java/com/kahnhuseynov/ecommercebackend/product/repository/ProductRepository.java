package com.kahnhuseynov.ecommercebackend.product.repository;

import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
