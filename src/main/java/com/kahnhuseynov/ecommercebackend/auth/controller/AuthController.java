package com.kahnhuseynov.ecommercebackend.auth.controller;

import com.kahnhuseynov.ecommercebackend.auth.dto.LoginRequest;
import com.kahnhuseynov.ecommercebackend.auth.dto.CurrentUserResponse;
import com.kahnhuseynov.ecommercebackend.auth.dto.TokenResponse;
import com.kahnhuseynov.ecommercebackend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(
                new CurrentUserResponse(authentication.getName(), authorities)
        );
    }
}
