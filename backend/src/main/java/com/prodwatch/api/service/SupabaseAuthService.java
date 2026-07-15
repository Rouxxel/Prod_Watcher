package com.prodwatch.api.service;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.util.CustomLogger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class SupabaseAuthService {

    private final RestClient anonClient;
    private final RestClient adminClient;
    private final String emailRedirectTo;

    public SupabaseAuthService(
            RestClient.Builder builder,
            @Value("${SUPABASE_URL}") String supabaseUrl,
            @Value("${SUPABASE_ANON_KEY}") String anonKey,
            @Value("${SUPABASE_SERVICE_ROLE_KEY}") String serviceRoleKey,
            @Value("${FRONTEND_URL:}") String frontendUrl) {
        String baseUrl = supabaseUrl.replaceAll("/$", "");
        this.emailRedirectTo =
                frontendUrl.isBlank() ? null : frontendUrl.replaceAll("/$", "") + "/confirm-email";
        this.anonClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("apikey", anonKey)
                .defaultHeader("Authorization", "Bearer " + anonKey)
                .build();
        this.adminClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
    }

    public AuthSession signUp(String email, String password, String name) {
        Map<String, Object> payload;
        if (emailRedirectTo != null && !emailRedirectTo.isBlank()) {
            payload = Map.of(
                    "email", email,
                    "password", password,
                    "data", Map.of("name", name),
                    "options", Map.of("emailRedirectTo", emailRedirectTo.trim()));
        } else {
            payload = Map.of("email", email, "password", password, "data", Map.of("name", name));
        }
        JsonNode body = postJson(anonClient, "/auth/v1/signup", payload);
        return parseSession(body);
    }

    public AuthSession signIn(String email, String password) {
        JsonNode body = postJson(
                anonClient,
                "/auth/v1/token?grant_type=password",
                Map.of("email", email, "password", password));
        return parseSession(body);
    }

    public void signOut(String accessToken) {
        try {
            anonClient
                    .post()
                    .uri("/auth/v1/logout")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            CustomLogger.debug("Supabase logout returned " + ex.getStatusCode());
        }
    }

    public UUID createConfirmedUser(String email, String password, String name) {
        JsonNode body = postJson(
                adminClient,
                "/auth/v1/admin/users",
                Map.of(
                        "email", email,
                        "password", password,
                        "email_confirm", true,
                        "user_metadata", Map.of("name", name)));
        if (body == null || !body.hasNonNull("id")) {
            throw new BusinessRuleException("Supabase did not return a user id");
        }
        return UUID.fromString(body.get("id").asText());
    }

    public void updatePassword(UUID userId, String newPassword) {
        putJson(adminClient, "/auth/v1/admin/users/{id}", userId.toString(), Map.of("password", newPassword));
    }

    public void disableUser(UUID userId) {
        putJson(
                adminClient,
                "/auth/v1/admin/users/{id}",
                userId.toString(),
                Map.of("ban_duration", "876000h"));
    }

    public void enableUser(UUID userId) {
        putJson(adminClient, "/auth/v1/admin/users/{id}", userId.toString(), Map.of("ban_duration", "none"));
    }

    public void deleteUser(UUID userId) {
        try {
            adminClient
                    .delete()
                    .uri("/auth/v1/admin/users/{id}", userId.toString())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapAuthError(ex);
        }
    }

    public AuthSession verifySignupToken(String token) {
        JsonNode body = postJson(anonClient, "/auth/v1/verify", Map.of("type", "signup", "token", token));
        return parseSession(body);
    }

    /** Checks Supabase Auth (auth.users) via the admin API. */
    public boolean emailExistsInAuth(String email) {
        try {
            JsonNode body = adminClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/auth/v1/admin/users")
                            .queryParam("page", 1)
                            .queryParam("per_page", 1)
                            .queryParam("filter", "email=eq." + email)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.has("users")) {
                return false;
            }
            JsonNode users = body.get("users");
            return users != null && users.isArray() && !users.isEmpty();
        } catch (RestClientResponseException ex) {
            CustomLogger.debug("Supabase email lookup failed " + ex.getStatusCode());
            return false;
        }
    }

    private JsonNode postJson(RestClient client, String uri, Object payload) {
        try {
            return client.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw mapAuthError(ex);
        }
    }

    private void putJson(RestClient client, String uri, String id, Object payload) {
        try {
            client.put()
                    .uri(uri, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapAuthError(ex);
        }
    }

    private AuthSession parseSession(JsonNode body) {
        if (body == null) {
            throw new BusinessRuleException("Empty response from Supabase Auth");
        }
        String accessToken = textOrNull(body, "access_token");
        String refreshToken = textOrNull(body, "refresh_token");
        Long expiresIn = body.has("expires_in") ? body.get("expires_in").asLong() : null;
        UUID userId = null;
        Boolean emailConfirmed = null;

        JsonNode user = body.get("user");
        if (user != null && user.hasNonNull("id")) {
            userId = UUID.fromString(user.get("id").asText());
            emailConfirmed =
                    user.hasNonNull("email_confirmed_at") && !user.get("email_confirmed_at").isNull();
        }

        return new AuthSession(accessToken, refreshToken, expiresIn, userId, emailConfirmed);
    }

    private static String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private BusinessRuleException mapAuthError(RestClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        CustomLogger.debug("Supabase auth error " + ex.getStatusCode() + ": " + responseBody);
        if (isDuplicateEmailError(ex.getStatusCode().value(), responseBody)) {
            return new BusinessRuleException(SignupEmailService.EMAIL_TAKEN_MESSAGE);
        }
        return new BusinessRuleException("Authentication request failed: " + ex.getStatusCode().value());
    }

    private static boolean isDuplicateEmailError(int status, String responseBody) {
        if (status != 400 && status != 422) {
            return false;
        }
        if (responseBody == null) {
            return false;
        }
        String lower = responseBody.toLowerCase();
        return lower.contains("already registered")
                || lower.contains("already exists")
                || lower.contains("user already")
                || lower.contains("duplicate");
    }

    public record AuthSession(
            String accessToken,
            String refreshToken,
            Long expiresIn,
            UUID userId,
            Boolean emailConfirmed) {}
}
