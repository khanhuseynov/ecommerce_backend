package com.kahnhuseynov.ecommercebackend.product.service;

import com.kahnhuseynov.ecommercebackend.category.entity.Category;
import com.kahnhuseynov.ecommercebackend.category.repository.CategoryRepository;
import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException;
import com.kahnhuseynov.ecommercebackend.core.exception.InvalidRequestException;
import com.kahnhuseynov.ecommercebackend.core.pagination.PaginationValidator;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductRequest;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductResponse;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductSearchCriteria;
import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import com.kahnhuseynov.ecommercebackend.product.mapper.ProductMapper;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("name", "name"),
            Map.entry("price", "price"),
            Map.entry("stock_quantity", "stockQuantity"),
            Map.entry("stockQuantity", "stockQuantity"),
            Map.entry("active", "active"),
            Map.entry("created_at", "createdAt"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("updated_at", "updatedAt"),
            Map.entry("updatedAt", "updatedAt")
    );

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        validatePriceRange(criteria);
        Pageable normalizedPageable = PaginationValidator.normalizeSort(pageable, ALLOWED_SORT_FIELDS);
        return productRepository.findAll(ProductSpecifications.withCriteria(criteria), normalizedPageable)
                .map(productMapper::toResponse);
    }

    private void validatePriceRange(ProductSearchCriteria criteria) {
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw new InvalidRequestException("Minimum price cannot be greater than maximum price.");
        }

    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProductById(id);

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        product.setCategory(findCategoryById(request.categoryId()));
        product.setActive(request.active() == null || request.active());

        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductById(id);

        productMapper.updateEntity(request, product);
        product.setCategory(findCategoryById(request.categoryId()));
        product.setActive(request.active() == null || request.active());

        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductById(id);

        productRepository.delete(product);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product with id '%s' not found.".formatted(id))
                );
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category with id '%s' not found.".formatted(id))
                );
    }
}
