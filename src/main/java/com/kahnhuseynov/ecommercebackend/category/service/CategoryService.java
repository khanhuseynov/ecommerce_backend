package com.kahnhuseynov.ecommercebackend.category.service;

import com.kahnhuseynov.ecommercebackend.category.dto.CategoryRequest;
import com.kahnhuseynov.ecommercebackend.category.dto.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    Page<CategoryResponse> getAllCategories(Pageable pageable);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
