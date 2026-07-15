package com.kahnhuseynov.ecommercebackend.core.security.jwt

import com.kahnhuseynov.ecommercebackend.core.security.CustomUserDetailsService
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import spock.lang.Specification

class JwtAuthenticationFilterSpec extends Specification {

    JwtTokenProvider jwtTokenProvider = Mock()
    CustomUserDetailsService userDetailsService = Mock()

    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtTokenProvider,
            userDetailsService
    )

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "doFilterInternal should authenticate request when bearer token is valid"() {
        given:
        def request = new MockHttpServletRequest("GET", "/api/v1/orders")
        request.addHeader("Authorization", "Bearer valid-token")
        def response = new MockHttpServletResponse()
        FilterChain filterChain = Mock()
        def userDetails = new User(
                "khan@example.com",
                "encoded-password",
                [new SimpleGrantedAuthority("ROLE_USER")]
        )

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * jwtTokenProvider.validateToken("valid-token") >> true
        1 * jwtTokenProvider.getEmailFromToken("valid-token") >> "khan@example.com"
        1 * userDetailsService.loadUserByUsername("khan@example.com") >> userDetails
        1 * filterChain.doFilter(request, response)
        0 * _

        SecurityContextHolder.context.authentication.authenticated
        SecurityContextHolder.context.authentication.name == "khan@example.com"
        SecurityContextHolder.context.authentication.authorities*.authority == ["ROLE_USER"]
    }

    def "doFilterInternal should continue without authentication when bearer token is missing"() {
        given:
        def request = new MockHttpServletRequest("GET", "/api/v1/orders")
        def response = new MockHttpServletResponse()
        FilterChain filterChain = Mock()

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        0 * _

        SecurityContextHolder.context.authentication == null
    }

    def "shouldNotFilter should skip public endpoints"() {
        expect:
        filter.shouldNotFilter(request("POST", "/api/v1/auth/login"))
        filter.shouldNotFilter(request("POST", "/api/v1/users/register"))
        filter.shouldNotFilter(request("GET", "/swagger-ui/index.html"))
        filter.shouldNotFilter(request("GET", "/v3/api-docs"))
    }

    def "shouldNotFilter should not skip protected endpoints"() {
        expect:
        !filter.shouldNotFilter(request("GET", "/api/v1/orders"))
    }

    private static MockHttpServletRequest request(String method, String path) {
        def request = new MockHttpServletRequest(method, path)
        request.setServletPath(path)
        request
    }
}
