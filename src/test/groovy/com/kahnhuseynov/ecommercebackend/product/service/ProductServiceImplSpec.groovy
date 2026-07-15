package com.kahnhuseynov.ecommercebackend.product.service

import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException
import com.kahnhuseynov.ecommercebackend.product.dto.ProductRequest
import com.kahnhuseynov.ecommercebackend.product.dto.ProductResponse
import com.kahnhuseynov.ecommercebackend.product.entity.Product
import com.kahnhuseynov.ecommercebackend.product.mapper.ProductMapper
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository
import spock.lang.Specification

class ProductServiceImplSpec extends Specification {

    ProductRepository productRepository = Mock()
    ProductMapper productMapper = Mock()

    ProductServiceImpl productService = new ProductServiceImpl(
            productRepository,
            productMapper
    )

    def "getAllProducts should return mapped products"() {
        given:
        def product = savedProduct()
        def response = successResponse()

        when:
        def result = productService.getAllProducts()

        then:
        1 * productRepository.findAll() >> List.of(product)
        1 * productMapper.toResponse(product) >> response
        0 * _

        result == List.of(response)
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
        new ProductRequest("Laptop", "Development laptop", new BigDecimal("1200.00"), 10, true)
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

    private static ProductResponse successResponse() {
        new ProductResponse(
                1L,
                "Laptop",
                "Development laptop",
                new BigDecimal("1200.00"),
                10,
                true,
                null,
                null
        )
    }
}
