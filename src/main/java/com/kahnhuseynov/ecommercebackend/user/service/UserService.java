package com.kahnhuseynov.ecommercebackend.user.service;

import com.kahnhuseynov.ecommercebackend.user.dto.UserRegisterRequest;
import com.kahnhuseynov.ecommercebackend.user.dto.UserResponse;

public interface UserService {
    // Registers a new user and returns the response DTO
    UserResponse registerUser(UserRegisterRequest request);
}