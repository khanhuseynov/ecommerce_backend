package com.kahnhuseynov.ecommercebackend.user.service

import com.kahnhuseynov.ecommercebackend.user.dto.UserRegisterRequest
import com.kahnhuseynov.ecommercebackend.user.dto.UserResponse
import com.kahnhuseynov.ecommercebackend.user.entity.Role
import com.kahnhuseynov.ecommercebackend.user.entity.User
import com.kahnhuseynov.ecommercebackend.user.exception.BusinessException
import com.kahnhuseynov.ecommercebackend.user.exception.ResourceNotFoundException
import com.kahnhuseynov.ecommercebackend.user.mapper.UserMapper
import com.kahnhuseynov.ecommercebackend.user.repository.RoleRepository
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository
import spock.lang.Specification

class UserServiceImplSpec extends Specification {

    UserRepository userRepository = Mock()
    RoleRepository roleRepository = Mock()
    UserMapper userMapper = Mock()

    UserServiceImpl userService = new UserServiceImpl(
            userRepository,
            roleRepository,
            userMapper
    )

    def "registerUser should register new user with default role and enabled status"() {
        given:
        def request = validRequest()
        def role = userRole()
        def user = mappedUser()
        def savedUser = savedUser(role)
        def response = successResponse()

        when:
        def result = userService.registerUser(request)

        then:
        1 * userRepository.existsByEmail("khan@example.com") >> false
        1 * userMapper.toEntity(request) >> user
        1 * roleRepository.findByName("ROLE_USER") >> Optional.of(role)

        1 * userRepository.save({
            it.email == "khan@example.com" &&
                    it.enabled == true &&
                    it.roles == Set.of(role)
        }) >> savedUser

        1 * userMapper.toResponse(savedUser) >> response
        0 * _

        result == response
    }

    def "registerUser should fail when email already exists"() {
        given:
        def request = validRequest()

        when:
        userService.registerUser(request)

        then:
        1 * userRepository.existsByEmail("khan@example.com") >> true

        def exception = thrown(BusinessException)
        exception.message == "Email is already in use."

        0 * _
    }

    def "registerUser should fail when default role does not exist"() {
        given:
        def request = validRequest()
        def user = mappedUser()

        when:
        userService.registerUser(request)

        then:
        1 * userRepository.existsByEmail("khan@example.com") >> false
        1 * userMapper.toEntity(request) >> user
        1 * roleRepository.findByName("ROLE_USER") >> Optional.empty()

        def exception = thrown(ResourceNotFoundException)
        exception.message == "Role 'ROLE_USER' not found."

        0 * _
    }

    private static UserRegisterRequest validRequest() {
        new UserRegisterRequest(
                "Khan",
                "Huseynov",
                "khan@example.com",
                "0501234567",
                "Password123"
        )
    }

    private static Role userRole() {
        new Role(1L, "ROLE_USER")
    }

    private static User mappedUser() {
        def user = new User()
        user.setFirstName("Khan")
        user.setLastName("Huseynov")
        user.setEmail("khan@example.com")
        user.setPhoneNumber("0501234567")
        user.setPassword("Password123")
        user
    }

    private static User savedUser(Role role) {
        def user = mappedUser()
        user.setId(1L)
        user.setEnabled(true)
        user.setRoles(Set.of(role))
        user
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