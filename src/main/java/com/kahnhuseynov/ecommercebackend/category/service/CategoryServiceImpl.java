package com.kahnhuseynov.ecommercebackend.category.service;

import com.kahnhuseynov.ecommercebackend.category.dto.CategoryRequest;
import com.kahnhuseynov.ecommercebackend.category.dto.CategoryResponse;
import com.kahnhuseynov.ecommercebackend.category.entity.Category;
import com.kahnhuseynov.ecommercebackend.category.mapper.CategoryMapper;
import com.kahnhuseynov.ecommercebackend.category.repository.CategoryRepository;
import com.kahnhuseynov.ecommercebackend.core.exception.BusinessException;
import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository;
import com.kahnhuseynov.ecommercebackend.core.pagination.PaginationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("name", "name"),
            Map.entry("active", "active"),
            Map.entry("created_at", "createdAt"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("updated_at", "updatedAt"),
            Map.entry("updatedAt", "updatedAt")
    );

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        Pageable normalizedPageable = PaginationValidator.normalizeSort(pageable, ALLOWED_SORT_FIELDS);
        return categoryRepository.findAll(normalizedPageable).map(categoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(findCategoryById(id));
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        validateUniqueName(name);

        Category category = categoryMapper.toEntity(request);
        category.setName(name);
        category.setActive(request.active() == null || request.active());

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryById(id);
        String name = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Category name is already in use.");
        }

        Boolean currentActive = category.getActive();
        categoryMapper.updateEntity(request, category);
        category.setName(name);
        category.setActive(request.active() == null ? currentActive : request.active());

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryById(id);

        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException("Category cannot be deleted because it contains products.");
        }

        categoryRepository.delete(category);
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with id '%s' not found.".formatted(id)
                ));
    }

    private void validateUniqueName(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Category name is already in use.");
        }
    }
}
