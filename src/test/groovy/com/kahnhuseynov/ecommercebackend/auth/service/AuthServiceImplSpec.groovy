package com.kahnhuseynov.ecommercebackend.auth.service

import com.kahnhuseynov.ecommercebackend.auth.dto.LoginRequest
import com.kahnhuseynov.ecommercebackend.core.security.jwt.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import spock.lang.Specification

class AuthServiceImplSpec extends Specification {

    AuthenticationManager authenticationManager = Mock()
    JwtTokenProvider jwtTokenProvider = Mock()

    AuthServiceImpl authService = new AuthServiceImpl(
            authenticationManager,
            jwtTokenProvider
    )

    def "login should authenticate credentials and return bearer token"() {
        given:
        def request = new LoginRequest("khan@example.com", "Password123")
        Authentication authentication = Mock()

        when:
        def result = authService.login(request)

        then:
        1 * authenticationManager.authenticate({
            it instanceof UsernamePasswordAuthenticationToken &&
                    it.principal == "khan@example.com" &&
                    it.credentials == "Password123"
        }) >> authentication
        1 * jwtTokenProvider.generateToken(authentication) >> "jwt-token"
        0 * _

        result.accessToken() == "jwt-token"
        result.tokenType() == "Bearer"
    }
}
