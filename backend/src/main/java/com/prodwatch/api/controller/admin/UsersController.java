package com.prodwatch.api.controller.admin;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.config.RateLimit;
import com.prodwatch.api.dto.user.UserProvisionRequest;
import com.prodwatch.api.dto.user.UserResetPasswordRequest;
import com.prodwatch.api.dto.user.UserResponse;
import com.prodwatch.api.dto.user.UserUpdateRequest;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.RoleChecker;
import com.prodwatch.api.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "${config.endpoints.users_endpoint.endpoint_prefix}${config.endpoints.users_endpoint.endpoint_route}")
@Tag(name = "users")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @RateLimit("users_endpoint")
    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireAdmin(user);
        return userService.list();
    }

    @RateLimit("users_endpoint")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CurrentUser user) {
        return userService.getMe(user);
    }

    @RateLimit("users_endpoint")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireAdmin(user);
        return userService.get(id);
    }

    @RateLimit("users_endpoint")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse provision(
            @Valid @RequestBody UserProvisionRequest body, @AuthenticationPrincipal CurrentUser user) {
        return userService.provision(body, user);
    }

    @RateLimit("users_endpoint")
    @PatchMapping("/{id}")
    public UserResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest body,
            @AuthenticationPrincipal CurrentUser user) {
        return userService.update(id, body, user);
    }

    @RateLimit("users_endpoint")
    @PostMapping("/{id}/promote-admin")
    public UserResponse promoteAdmin(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return userService.promoteToAdmin(id, user);
    }

    @RateLimit("users_endpoint")
    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody UserResetPasswordRequest body,
            @AuthenticationPrincipal CurrentUser user) {
        userService.resetPassword(id, body, user);
    }

    @RateLimit("users_endpoint")
    @PostMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return userService.deactivate(id, user);
    }

    @RateLimit("users_endpoint")
    @PostMapping("/{id}/reactivate")
    public UserResponse reactivate(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return userService.reactivate(id, user);
    }
}
