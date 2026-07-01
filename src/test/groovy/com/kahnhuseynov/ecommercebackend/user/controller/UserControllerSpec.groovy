package com.kahnhuseynov.ecommercebackend.user.controller

import com.kahnhuseynov.ecommercebackend.user.dto.UserResponse
import com.kahnhuseynov.ecommercebackend.user.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.spockframework.spring.SpringBean
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    UserService userService = Mock()

    def "POST /api/v1/users/register with valid request returns 201 Created"() {
        given:
        userService.registerUser(_) >> successResponse()

        expect:
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isCreated())
    }

    def "POST /api/v1/users/register response JSON contains id, email, enabled, roles"() {
        given:
        userService.registerUser(_) >> successResponse()

        expect:
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.id').value(1))
                .andExpect(jsonPath('$.email').value("khan@example.com"))
                .andExpect(jsonPath('$.enabled').value(true))
                .andExpect(jsonPath('$.roles[0]').value("ROLE_USER"))
    }

    def "POST /api/v1/users/register with invalid email returns 400 Bad Request"() {
        when:
        def result = mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson("invalid-email")))

        then:
        result
                .andExpect(status().isBadRequest())

        0 * userService._
    }

    def "POST /api/v1/users/register with blank firstName returns 400 Bad Request"() {
        when:
        def result = mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson("khan@example.com", "")))

        then:
        result
                .andExpect(status().isBadRequest())

        0 * userService._
    }

    private static String validRequestJson(
            String email = "khan@example.com",
            String firstName = "Khan"
    ) {
        """
        {
          "first_name": "${firstName}",
          "last_name": "Huseynov",
          "email": "${email}",
          "phone_number": "0501234567",
          "password": "Password123"
        }
        """
    }

    private static UserResponse successResponse() {
        new UserResponse(
                1L,
                "Khan",
                "Huseynov",
                "khan@example.com",
                "0501234567",
                true,
                Set.of("ROLE_USER")
        )
    }
}
