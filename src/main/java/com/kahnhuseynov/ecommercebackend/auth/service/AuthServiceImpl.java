package com.kahnhuseynov.ecommercebackend.auth.service;

import com.kahnhuseynov.ecommercebackend.auth.dto.LoginRequest;
import com.kahnhuseynov.ecommercebackend.auth.dto.TokenResponse;
import com.kahnhuseynov.ecommercebackend.core.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);

        return new TokenResponse(token, "Bearer");
    }
}