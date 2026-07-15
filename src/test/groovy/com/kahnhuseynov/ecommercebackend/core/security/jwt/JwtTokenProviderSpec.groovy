package com.kahnhuseynov.ecommercebackend.core.security.jwt

import com.kahnhuseynov.ecommercebackend.core.security.CustomUserPrincipal
import com.kahnhuseynov.ecommercebackend.user.entity.Role
import com.kahnhuseynov.ecommercebackend.user.entity.User
import io.jsonwebtoken.ExpiredJwtException
import org.springframework.security.core.Authentication
import spock.lang.Specification

class JwtTokenProviderSpec extends Specification {

    private static final String SECRET = "EaH2ro8ToZ5oeqZKBEzj8+kar0DfwlNymc2wYxWHy2U="

    def "generateToken should create valid token with email subject"() {
        given:
        def provider = new JwtTokenProvider(SECRET, 86400000L)
        Authentication authentication = Mock()
        authentication.getPrincipal() >> new CustomUserPrincipal(user())

        when:
        def token = provider.generateToken(authentication)

        then:
        provider.validateToken(token)
        provider.getEmailFromToken(token) == "khan@example.com"
    }

    def "validateToken should fail for expired token"() {
        given:
        def provider = new JwtTokenProvider(SECRET, -1000L)
        Authentication authentication = Mock()
        authentication.getPrincipal() >> new CustomUserPrincipal(user())
        def token = provider.generateToken(authentication)

        when:
        provider.validateToken(token)

        then:
        thrown(ExpiredJwtException)
    }

    private static User user() {
        def user = new User()
        user.setId(1L)
        user.setEmail("khan@example.com")
        user.setPassword("encoded-password")
        user.setEnabled(true)
        user.setRoles(Set.of(new Role(1L, "ROLE_USER")))
        user
    }
}
