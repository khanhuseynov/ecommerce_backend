package com.kahnhuseynov.ecommercebackend.product.service;

import com.kahnhuseynov.ecommercebackend.product.dto.ProductRequest;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
