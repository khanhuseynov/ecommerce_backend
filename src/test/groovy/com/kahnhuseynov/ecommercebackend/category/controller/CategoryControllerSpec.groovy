package com.kahnhuseynov.ecommercebackend.category.controller

import com.kahnhuseynov.ecommercebackend.category.dto.CategoryResponse
import com.kahnhuseynov.ecommercebackend.category.service.CategoryService
import com.kahnhuseynov.ecommercebackend.core.security.CustomUserDetailsService
import com.kahnhuseynov.ecommercebackend.core.security.jwt.JwtTokenProvider
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CategoryController)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    CategoryService categoryService = Mock()

    @SpringBean
    JwtTokenProvider jwtTokenProvider = Mock()

    @SpringBean
    CustomUserDetailsService userDetailsService = Mock()

    def "GET /api/v1/categories returns categories"() {
        given:
        categoryService.getAllCategories() >> List.of(successResponse())

        expect:
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$[0].id').value(1))
                .andExpect(jsonPath('$[0].name').value("Electronics"))
    }

    def "GET /api/v1/categories/{id} returns category"() {
        given:
        categoryService.getCategoryById(1L) >> successResponse()

        expect:
        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(1))
                .andExpect(jsonPath('$.active').value(true))
    }

    def "POST /api/v1/categories with valid request returns 201 Created"() {
        given:
        categoryService.createCategory(_) >> successResponse()

        expect:
        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.name').value("Electronics"))
    }

    def "POST /api/v1/categories with blank name returns 400 Bad Request"() {
        when:
        def result = mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson("")))

        then:
        result.andExpect(status().isBadRequest())
        0 * categoryService._
    }

    def "PUT /api/v1/categories/{id} returns updated category"() {
        given:
        categoryService.updateCategory(1L, _) >> successResponse()

        expect:
        mockMvc.perform(put("/api/v1/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(1))
                .andExpect(jsonPath('$.name').value("Electronics"))
    }

    def "PUT /api/v1/categories/{id} with invalid request returns 400 Bad Request"() {
        when:
        def result = mockMvc.perform(put("/api/v1/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson("")))

        then:
        result.andExpect(status().isBadRequest())
        0 * categoryService._
    }

    def "DELETE /api/v1/categories/{id} returns 204 No Content"() {
        when:
        def result = mockMvc.perform(delete("/api/v1/categories/1"))

        then:
        result.andExpect(status().isNoContent())
        1 * categoryService.deleteCategory(1L)
        0 * categoryService._
    }

    private static String validRequestJson(String name = "Electronics") {
        """
        {
          "name": "${name}",
          "description": "Devices",
          "active": true
        }
        """
    }

    private static CategoryResponse successResponse() {
        new CategoryResponse(1L, "Electronics", "Devices", true, null, null)
    }
}
