package com.kahnhuseynov.ecommercebackend.category.service

import com.kahnhuseynov.ecommercebackend.category.dto.CategoryRequest
import com.kahnhuseynov.ecommercebackend.category.dto.CategoryResponse
import com.kahnhuseynov.ecommercebackend.category.entity.Category
import com.kahnhuseynov.ecommercebackend.category.mapper.CategoryMapper
import com.kahnhuseynov.ecommercebackend.category.repository.CategoryRepository
import com.kahnhuseynov.ecommercebackend.core.exception.BusinessException
import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository
import spock.lang.Specification
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class CategoryServiceImplSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    CategoryMapper categoryMapper = Mock()
    ProductRepository productRepository = Mock()

    CategoryServiceImpl categoryService = new CategoryServiceImpl(
            categoryRepository,
            categoryMapper,
            productRepository
    )

    def "getAllCategories should return mapped categories"() {
        given:
        def category = savedCategory()
        def response = successResponse()

        def pageable = PageRequest.of(0, 20)

        when:
        def result = categoryService.getAllCategories(pageable)

        then:
        1 * categoryRepository.findAll(pageable) >> new PageImpl<>(List.of(category), pageable, 1)
        1 * categoryMapper.toResponse(category) >> response
        0 * _
        result.content == List.of(response)
        result.totalElements == 1
    }

    def "getCategoryById should return mapped category"() {
        given:
        def category = savedCategory()
        def response = successResponse()

        when:
        def result = categoryService.getCategoryById(1L)

        then:
        1 * categoryRepository.findById(1L) >> Optional.of(category)
        1 * categoryMapper.toResponse(category) >> response
        0 * _
        result == response
    }

    def "getCategoryById should fail when category does not exist"() {
        when:
        categoryService.getCategoryById(99L)

        then:
        1 * categoryRepository.findById(99L) >> Optional.empty()
        def exception = thrown(ResourceNotFoundException)
        exception.message == "Category with id '99' not found."
        0 * _
    }

    def "createCategory should trim name save and return category"() {
        given:
        def request = new CategoryRequest(" Electronics ", "Devices", null)
        def category = new Category()
        def saved = savedCategory()
        def response = successResponse()

        when:
        def result = categoryService.createCategory(request)

        then:
        1 * categoryRepository.existsByNameIgnoreCase("Electronics") >> false
        1 * categoryMapper.toEntity(request) >> category
        1 * categoryRepository.save({
            it.name == "Electronics" && it.active == true
        }) >> saved
        1 * categoryMapper.toResponse(saved) >> response
        0 * _
        result == response
    }

    def "createCategory should fail when name already exists"() {
        given:
        def request = validRequest()

        when:
        categoryService.createCategory(request)

        then:
        1 * categoryRepository.existsByNameIgnoreCase("Electronics") >> true
        def exception = thrown(BusinessException)
        exception.message == "Category name is already in use."
        0 * _
    }

    def "updateCategory should update existing category"() {
        given:
        def request = validRequest()
        def category = savedCategory()
        def response = successResponse()

        when:
        def result = categoryService.updateCategory(1L, request)

        then:
        1 * categoryRepository.findById(1L) >> Optional.of(category)
        1 * categoryRepository.existsByNameIgnoreCaseAndIdNot("Electronics", 1L) >> false
        1 * categoryMapper.updateEntity(request, category)
        1 * categoryRepository.save(category) >> category
        1 * categoryMapper.toResponse(category) >> response
        0 * _
        result == response
    }

    def "updateCategory should fail when another category has same name"() {
        given:
        def request = validRequest()
        def category = savedCategory()

        when:
        categoryService.updateCategory(1L, request)

        then:
        1 * categoryRepository.findById(1L) >> Optional.of(category)
        1 * categoryRepository.existsByNameIgnoreCaseAndIdNot("Electronics", 1L) >> true
        def exception = thrown(BusinessException)
        exception.message == "Category name is already in use."
        0 * _
    }

    def "deleteCategory should delete category without products"() {
        given:
        def category = savedCategory()

        when:
        categoryService.deleteCategory(1L)

        then:
        1 * categoryRepository.findById(1L) >> Optional.of(category)
        1 * productRepository.existsByCategoryId(1L) >> false
        1 * categoryRepository.delete(category)
        0 * _
    }

    def "deleteCategory should fail when category contains products"() {
        given:
        def category = savedCategory()

        when:
        categoryService.deleteCategory(1L)

        then:
        1 * categoryRepository.findById(1L) >> Optional.of(category)
        1 * productRepository.existsByCategoryId(1L) >> true
        def exception = thrown(BusinessException)
        exception.message == "Category cannot be deleted because it contains products."
        0 * _
    }

    private static CategoryRequest validRequest() {
        new CategoryRequest("Electronics", "Devices", true)
    }

    private static Category savedCategory() {
        def category = new Category()
        category.setId(1L)
        category.setName("Electronics")
        category.setDescription("Devices")
        category.setActive(true)
        category
    }

    private static CategoryResponse successResponse() {
        new CategoryResponse(1L, "Electronics", "Devices", true, null, null)
    }
}
