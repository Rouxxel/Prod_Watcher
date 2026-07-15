package com.prodwatch.api.controller.settings;

import com.prodwatch.api.config.RateLimit;
import com.prodwatch.api.dto.settings.SettingsResponse;
import com.prodwatch.api.dto.settings.SettingsUpdateRequest;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.RoleChecker;
import com.prodwatch.api.service.SettingsService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "${config.endpoints.settings_endpoint.endpoint_prefix}${config.endpoints.settings_endpoint.endpoint_route}")
@Tag(name = "settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @RateLimit("settings_endpoint")
    @GetMapping
    public SettingsResponse get() {
        return settingsService.get();
    }

    @RateLimit("settings_endpoint")
    @PatchMapping
    public SettingsResponse update(
            @Valid @RequestBody SettingsUpdateRequest body, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireAdmin(user);
        return settingsService.update(body, user);
    }
}
