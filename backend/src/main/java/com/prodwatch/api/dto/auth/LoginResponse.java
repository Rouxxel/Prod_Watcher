package com.prodwatch.api.dto.auth;

import com.prodwatch.api.dto.user.UserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        UserResponse user) {}
