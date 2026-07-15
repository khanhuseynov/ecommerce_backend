package com.kahnhuseynov.ecommercebackend.auth.controller

import com.kahnhuseynov.ecommercebackend.auth.dto.TokenResponse
import com.kahnhuseynov.ecommercebackend.auth.service.AuthService
import com.kahnhuseynov.ecommercebackend.core.security.CustomUserDetailsService
import com.kahnhuseynov.ecommercebackend.core.security.jwt.JwtTokenProvider
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    AuthService authService = Mock()

    @SpringBean
    JwtTokenProvider jwtTokenProvider = Mock()

    @SpringBean
    CustomUserDetailsService userDetailsService = Mock()

    def "POST /api/v1/auth/login with valid request returns bearer token"() {
        given:
        authService.login(_) >> new TokenResponse("access-token", "Bearer")

        expect:
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validLoginJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.accessToken').value("access-token"))
                .andExpect(jsonPath('$.tokenType').value("Bearer"))
    }

    def "POST /api/v1/auth/login with invalid email returns 400 Bad Request"() {
        when:
        def result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validLoginJson("invalid-email")))

        then:
        result.andExpect(status().isBadRequest())
        0 * authService._
    }

    def "POST /api/v1/auth/login with blank password returns 400 Bad Request"() {
        when:
        def result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validLoginJson("khan@example.com", "")))

        then:
        result.andExpect(status().isBadRequest())
        0 * authService._
    }

    private static String validLoginJson(
            String email = "khan@example.com",
            String password = "Password123"
    ) {
        """
        {
          "email": "${email}",
          "password": "${password}"
        }
        """
    }
}
