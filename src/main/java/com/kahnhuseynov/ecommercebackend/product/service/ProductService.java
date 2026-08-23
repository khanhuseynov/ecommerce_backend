package com.kahnhuseynov.ecommercebackend.product.service;

import com.kahnhuseynov.ecommercebackend.product.dto.ProductRequest;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductResponse;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Page<ProductResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable);

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
