package com.kahnhuseynov.ecommercebackend.product.controller;

import com.kahnhuseynov.ecommercebackend.product.dto.ProductRequest;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductResponse;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductSearchCriteria;
import com.kahnhuseynov.ecommercebackend.core.dto.PageResponse;
import com.kahnhuseynov.ecommercebackend.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(name = "category_id", required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(name = "min_price", required = false)
            @DecimalMin(value = "0.0", message = "Minimum price cannot be negative") BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false)
            @DecimalMin(value = "0.0", message = "Maximum price cannot be negative") BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(
                search, categoryId, active, minPrice, maxPrice
        );
        return ResponseEntity.ok(PageResponse.from(productService.searchProducts(criteria, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse response = productService.createProduct(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
}
