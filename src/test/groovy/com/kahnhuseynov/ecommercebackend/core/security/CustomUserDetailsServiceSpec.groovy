package com.kahnhuseynov.ecommercebackend.core.security

import com.kahnhuseynov.ecommercebackend.user.entity.Role
import com.kahnhuseynov.ecommercebackend.user.entity.User
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import spock.lang.Specification

class CustomUserDetailsServiceSpec extends Specification {

    UserRepository userRepository = Mock()

    CustomUserDetailsService userDetailsService = new CustomUserDetailsService(userRepository)

    def "loadUserByUsername should return principal with user authorities"() {
        given:
        def user = enabledUser()

        when:
        def result = userDetailsService.loadUserByUsername("khan@example.com")

        then:
        1 * userRepository.findByEmail("khan@example.com") >> Optional.of(user)
        0 * _

        result instanceof CustomUserPrincipal
        result.id == 1L
        result.username == "khan@example.com"
        result.password == "encoded-password"
        result.enabled
        result.authorities*.authority == ["ROLE_USER", "ROLE_ADMIN"]
    }

    def "loadUserByUsername should fail when user does not exist"() {
        when:
        userDetailsService.loadUserByUsername("missing@example.com")

        then:
        1 * userRepository.findByEmail("missing@example.com") >> Optional.empty()

        def exception = thrown(UsernameNotFoundException)
        exception.message == "User not found with email: missing@example.com"

        0 * _
    }

    private static User enabledUser() {
        def user = new User()
        user.setId(1L)
        user.setEmail("khan@example.com")
        user.setPassword("encoded-password")
        user.setEnabled(true)
        user.setRoles([
                new Role(1L, "ROLE_USER"),
                new Role(2L, "ROLE_ADMIN")
        ] as LinkedHashSet)
        user
    }
}
