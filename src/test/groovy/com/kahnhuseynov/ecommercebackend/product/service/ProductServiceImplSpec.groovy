package com.kahnhuseynov.ecommercebackend.product.service

import com.kahnhuseynov.ecommercebackend.category.entity.Category
import com.kahnhuseynov.ecommercebackend.category.repository.CategoryRepository
import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException
import com.kahnhuseynov.ecommercebackend.product.dto.ProductRequest
import com.kahnhuseynov.ecommercebackend.product.dto.ProductResponse
import com.kahnhuseynov.ecommercebackend.product.dto.ProductSearchCriteria
import com.kahnhuseynov.ecommercebackend.product.entity.Product
import com.kahnhuseynov.ecommercebackend.product.mapper.ProductMapper
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import spock.lang.Specification

class ProductServiceImplSpec extends Specification {

    ProductRepository productRepository = Mock()
    ProductMapper productMapper = Mock()
    CategoryRepository categoryRepository = Mock()

    ProductServiceImpl productService = new ProductServiceImpl(
            productRepository,
            productMapper,
            categoryRepository
    )

    def "searchProducts should return mapped product page"() {
        given:
        def product = savedProduct()
        def response = successResponse()
        def criteria = new ProductSearchCriteria(
                "lap", 1L, true, new BigDecimal("100"), new BigDecimal("2000")
        )
        def pageable = PageRequest.of(0, 10)

        when:
        def result = productService.searchProducts(criteria, pageable)

        then:
        1 * productRepository.findAll(_, pageable) >> new PageImpl<>(List.of(product), pageable, 1)
        1 * productMapper.toResponse(product) >> response
        0 * _

        result.content == List.of(response)
        result.totalElements == 1
    }

    def "getProductById should return mapped product when product exists"() {
        given:
        def product = savedProduct()
        def response = successResponse()

        when:
        def result = productService.getProductById(1L)

        then:
        1 * productRepository.findById(1L) >> Optional.of(product)
        1 * productMapper.toResponse(product) >> response
        0 * _

        result == response
    }

    def "getProductById should fail when product does not exist"() {
        when:
        productService.getProductById(99L)

        then:
        1 * productRepository.findById(99L) >> Optional.empty()

        def exception = thrown(ResourceNotFoundException)
        exception.message == "Product with id '99' not found."

        0 * _
    }

    def "createProduct should save and return mapped product"() {
        given:
        def request = validRequest()
        def product = mappedProduct()
        def savedProduct = savedProduct()
        def response = successResponse()

        when:
        def result = productService.createProduct(request)

        then:
        1 * productMapper.toEntity(request) >> product
        1 * categoryRepository.findById(1L) >> Optional.of(category())
        1 * productRepository.save({
            it.name == "Laptop" &&
                    it.price == new BigDecimal("1200.00") &&
                    it.stockQuantity == 10 &&
                    it.active == true
        }) >> savedProduct
        1 * productMapper.toResponse(savedProduct) >> response
        0 * _

        result == response
    }

    def "createProduct should fail when category does not exist"() {
        given:
        def request = validRequest()
        def product = mappedProduct()

        when:
        productService.createProduct(request)

        then:
        1 * productMapper.toEntity(request) >> product
        1 * categoryRepository.findById(1L) >> Optional.empty()

        def exception = thrown(ResourceNotFoundException)
        exception.message == "Category with id '1' not found."

        0 * _
    }

    def "updateProduct should update existing product"() {
        given:
        def request = validRequest()
        def product = savedProduct()
        def response = successResponse()

        when:
        def result = productService.updateProduct(1L, request)

        then:
        1 * productRepository.findById(1L) >> Optional.of(product)
        1 * productMapper.updateEntity(request, product)
        1 * categoryRepository.findById(1L) >> Optional.of(category())
        1 * productRepository.save(product) >> product
        1 * productMapper.toResponse(product) >> response
        0 * _

        result == response
    }

    def "updateProduct should fail when product does not exist"() {
        given:
        def request = validRequest()

        when:
        productService.updateProduct(99L, request)

        then:
        1 * productRepository.findById(99L) >> Optional.empty()

        def exception = thrown(ResourceNotFoundException)
        exception.message == "Product with id '99' not found."

        0 * _
    }

    def "updateProduct should fail when category does not exist"() {
        given:
        def request = validRequest()
        def product = savedProduct()

        when:
        productService.updateProduct(1L, request)

        then:
        1 * productRepository.findById(1L) >> Optional.of(product)
        1 * productMapper.updateEntity(request, product)
        1 * categoryRepository.findById(1L) >> Optional.empty()

        def exception = thrown(ResourceNotFoundException)
        exception.message == "Category with id '1' not found."

        0 * _
    }

    def "deleteProduct should delete existing product"() {
        given:
        def product = savedProduct()

        when:
        productService.deleteProduct(1L)

        then:
        1 * productRepository.findById(1L) >> Optional.of(product)
        1 * productRepository.delete(product)
        0 * _
    }

    def "deleteProduct should fail when product does not exist"() {
        when:
        productService.deleteProduct(99L)

        then:
        1 * productRepository.findById(99L) >> Optional.empty()

        def exception = thrown(ResourceNotFoundException)
        exception.message == "Product with id '99' not found."

        0 * _
    }

    private static ProductRequest validRequest() {
        new ProductRequest("Laptop", "Development laptop", new BigDecimal("1200.00"), 10, 1L, true)
    }

    private static Product mappedProduct() {
        def product = new Product()
        product.setName("Laptop")
        product.setDescription("Development laptop")
        product.setPrice(new BigDecimal("1200.00"))
        product.setStockQuantity(10)
        product.setActive(true)
        product
    }

    private static Product savedProduct() {
        def product = mappedProduct()
        product.setId(1L)
        product
    }

    private static Category category() {
        def category = new Category()
        category.setId(1L)
        category.setName("Electronics")
        category.setActive(true)
        category
    }

    private static ProductResponse successResponse() {
        new ProductResponse(
                1L,
                "Laptop",
                "Development laptop",
                new BigDecimal("1200.00"),
                10,
                null,
                true,
                null,
                null
        )
    }
}
