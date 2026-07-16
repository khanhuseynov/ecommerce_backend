package com.kahnhuseynov.ecommercebackend.category.service;

import com.kahnhuseynov.ecommercebackend.category.dto.CategoryRequest;
import com.kahnhuseynov.ecommercebackend.category.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
