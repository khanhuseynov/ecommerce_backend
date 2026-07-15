package com.kahnhuseynov.ecommercebackend.auth.service;

import com.kahnhuseynov.ecommercebackend.auth.dto.LoginRequest;
import com.kahnhuseynov.ecommercebackend.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);
}