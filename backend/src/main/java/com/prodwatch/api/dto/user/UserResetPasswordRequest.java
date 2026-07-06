package com.prodwatch.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserResetPasswordRequest(@NotBlank @Size(min = 8, max = 128) String newPassword) {}
