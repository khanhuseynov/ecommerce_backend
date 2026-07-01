package com.kahnhuseynov.ecommercebackend.user.controller;

import com.kahnhuseynov.ecommercebackend.user.dto.UserRegisterRequest;
import com.kahnhuseynov.ecommercebackend.user.dto.UserResponse;
import com.kahnhuseynov.ecommercebackend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController


@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Endpoint for user registration
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {

        // Pass the validated request to the service layer
        UserResponse response = userService.registerUser(request);

        // Return 201 CREATED status code upon successful registration
        return new ResponseEntity<>(response, HttpStatus.CREATED);


    }
}