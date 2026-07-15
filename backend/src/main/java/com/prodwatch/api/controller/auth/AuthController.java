package com.prodwatch.api.controller.auth;

import com.prodwatch.api.config.RateLimit;
import com.prodwatch.api.dto.auth.BootstrapStatusResponse;
import com.prodwatch.api.dto.auth.ConfirmEmailRequest;
import com.prodwatch.api.dto.auth.LoginRequest;
import com.prodwatch.api.dto.auth.LoginResponse;
import com.prodwatch.api.dto.auth.SignupEmailAvailabilityResponse;
import com.prodwatch.api.dto.auth.SignupRequest;
import com.prodwatch.api.dto.auth.SignupResponse;
import com.prodwatch.api.dto.user.UserResponse;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.error.UnauthorizedException;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.service.AuthBootstrapService;
import com.prodwatch.api.service.SignupEmailService;
import com.prodwatch.api.service.SupabaseAuthService;
import com.prodwatch.api.service.SupabaseAuthService.AuthSession;
import com.prodwatch.api.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${config.endpoints.auth_endpoint.endpoint_prefix}${config.endpoints.auth_endpoint.endpoint_route}")
@Tag(name = "auth")
public class AuthController {

    private final AuthBootstrapService authBootstrapService;
    private final SupabaseAuthService supabaseAuthService;
    private final UserService userService;
    private final SignupEmailService signupEmailService;

    public AuthController(
            AuthBootstrapService authBootstrapService,
            SupabaseAuthService supabaseAuthService,
            UserService userService,
            SignupEmailService signupEmailService) {
        this.authBootstrapService = authBootstrapService;
        this.supabaseAuthService = supabaseAuthService;
        this.userService = userService;
        this.signupEmailService = signupEmailService;
    }

    @RateLimit("auth_endpoint")
    @GetMapping("/bootstrap-status")
    public BootstrapStatusResponse bootstrapStatus() {
        return new BootstrapStatusResponse(authBootstrapService.isSignupAllowed());
    }

    @RateLimit("auth_endpoint")
    @GetMapping("/signup-email-available")
    public SignupEmailAvailabilityResponse signupEmailAvailable(@RequestParam @Email String email) {
        return new SignupEmailAvailabilityResponse(signupEmailService.isEmailAvailable(email));
    }

    @RateLimit("auth_endpoint")
    @PostMapping("/signup")
    public SignupResponse signup(@Valid @RequestBody SignupRequest body) {
        String email = SignupEmailService.normalizeEmail(body.email());
        signupEmailService.assertEmailAvailable(email);
        AuthSession session = supabaseAuthService.signUp(email, body.password(), body.name().trim());
        if (session.userId() != null) {
            authBootstrapService.ensureAdminRoleFromSignup(session.userId());
        }
        return new SignupResponse("Check your email to confirm your account");
    }

    @RateLimit("auth_endpoint")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        AuthSession session = supabaseAuthService.signIn(body.email(), body.password());
        if (session.userId() == null) {
            throw new UnauthorizedException("Invalid credentials");
        }
        authBootstrapService.ensureAdminRoleFromSignup(session.userId());
        UserResponse user = userService.getForAuthSession(session.userId());
        if (!user.active()) {
            throw new UnauthorizedException("Account is inactive");
        }
        return new LoginResponse(
                session.accessToken(),
                session.refreshToken(),
                session.expiresIn(),
                withEmailConfirmed(user, session.emailConfirmed()));
    }

    @RateLimit("auth_endpoint")
    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            supabaseAuthService.signOut(authorization.substring(7).trim());
        }
    }

    @RateLimit("auth_endpoint")
    @PostMapping("/confirm-email")
    public LoginResponse confirmEmail(@Valid @RequestBody ConfirmEmailRequest body) {
        AuthSession session = supabaseAuthService.verifySignupToken(body.token());
        if (session.userId() == null) {
            throw new BusinessRuleException("Invalid confirmation token");
        }
        authBootstrapService.ensureAdminRoleFromSignup(session.userId());
        UserResponse user = userService.getForAuthSession(session.userId());
        return new LoginResponse(
                session.accessToken(),
                session.refreshToken(),
                session.expiresIn(),
                withEmailConfirmed(user, true));
    }

    @RateLimit("auth_endpoint")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userService.getMe(currentUser);
    }

    private static UserResponse withEmailConfirmed(UserResponse user, Boolean emailConfirmed) {
        return new UserResponse(
                user.id(),
                user.name(),
                user.email(),
                user.role(),
                user.active(),
                emailConfirmed,
                user.ecosystemId(),
                user.ecosystemName());
    }
}
