package com.kahnhuseynov.ecommercebackend.auth.controller;

import com.kahnhuseynov.ecommercebackend.auth.dto.LoginRequest;
import com.kahnhuseynov.ecommercebackend.auth.dto.TokenResponse;
import com.kahnhuseynov.ecommercebackend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}