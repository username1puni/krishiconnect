package com.krishiconnect.dto;

public record AuthResponse(String token, long expiresInSeconds, UserResponse user) {
}
