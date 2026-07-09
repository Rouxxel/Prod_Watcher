package com.prodwatch.api.dto.user;

import com.prodwatch.api.entity.AppRole;

/** PATCH body — {@code null} fields are left unchanged. Role must not be {@code admin}. */
public record UserUpdateRequest(Boolean active, AppRole role) {}
