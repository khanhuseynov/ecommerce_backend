package com.kahnhuseynov.ecommercebackend.auth.dto;

public record TokenResponse(
        String accessToken,
        String tokenType
) {
}