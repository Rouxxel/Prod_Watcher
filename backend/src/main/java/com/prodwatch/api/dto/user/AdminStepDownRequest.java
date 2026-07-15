package com.prodwatch.api.dto.user;

import com.prodwatch.api.entity.AppRole;

import jakarta.validation.constraints.NotNull;

public record AdminStepDownRequest(@NotNull AppRole role) {}
