package com.prodwatch.api.dto.user;

import com.prodwatch.api.entity.AppRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin provisioning — role must not be {@code admin} (enforced in service). */
public record UserProvisionRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull AppRole role,
        boolean active) {}
