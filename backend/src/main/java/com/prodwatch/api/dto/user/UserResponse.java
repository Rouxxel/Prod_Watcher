package com.prodwatch.api.dto.user;

import java.util.UUID;

import com.prodwatch.api.entity.AppRole;

public record UserResponse(
        UUID id,
        String name,
        String email,
        AppRole role,
        boolean active,
        Boolean emailConfirmed) {}
