package com.kahnhuseynov.ecommercebackend.product.controller

import com.kahnhuseynov.ecommercebackend.core.security.CustomUserDetailsService
import com.kahnhuseynov.ecommercebackend.core.security.jwt.JwtTokenProvider
import com.kahnhuseynov.ecommercebackend.product.dto.ProductResponse
import com.kahnhuseynov.ecommercebackend.product.service.ProductService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.time.LocalDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProductController)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    ProductService productService = Mock()

    @SpringBean
    JwtTokenProvider jwtTokenProvider = Mock()

    @SpringBean
    CustomUserDetailsService userDetailsService = Mock()

    def "GET /api/v1/products returns products"() {
        given:
        productService.getAllProducts() >> List.of(successResponse())

        expect:
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$[0].id').value(1))
                .andExpect(jsonPath('$[0].name').value("Laptop"))
                .andExpect(jsonPath('$[0].stock_quantity').value(10))
    }

    def "GET /api/v1/products/{id} returns product"() {
        given:
        productService.getProductById(1L) >> successResponse()

        expect:
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(1))
                .andExpect(jsonPath('$.name').value("Laptop"))
                .andExpect(jsonPath('$.price').value(1200.0))
    }

    def "POST /api/v1/products with valid request returns 201 Created"() {
        given:
        productService.createProduct(_) >> successResponse()

        expect:
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.id').value(1))
                .andExpect(jsonPath('$.name').value("Laptop"))
    }

    def "POST /api/v1/products with blank name returns 400 Bad Request"() {
        when:
        def result = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson("")))

        then:
        result.andExpect(status().isBadRequest())
        0 * productService._
    }

    def "PUT /api/v1/products/{id} with valid request returns updated product"() {
        given:
        productService.updateProduct(1L, _) >> successResponse()

        expect:
        mockMvc.perform(put("/api/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(1))
                .andExpect(jsonPath('$.name').value("Laptop"))
                .andExpect(jsonPath('$.stock_quantity').value(10))
    }

    def "PUT /api/v1/products/{id} with invalid request returns 400 Bad Request"() {
        when:
        def result = mockMvc.perform(put("/api/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson("")))

        then:
        result.andExpect(status().isBadRequest())
        0 * productService._
    }

    def "DELETE /api/v1/products/{id} returns 204 No Content"() {
        when:
        def result = mockMvc.perform(delete("/api/v1/products/1"))

        then:
        result.andExpect(status().isNoContent())
        1 * productService.deleteProduct(1L)
        0 * productService._
    }

    private static String validRequestJson(String name = "Laptop") {
        """
        {
          "name": "${name}",
          "description": "Development laptop",
          "price": 1200.00,
          "stock_quantity": 10,
          "category_id": 1,
          "active": true
        }
        """
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
                LocalDateTime.parse("2026-07-15T10:00:00"),
                LocalDateTime.parse("2026-07-15T10:00:00")
        )
    }
}
