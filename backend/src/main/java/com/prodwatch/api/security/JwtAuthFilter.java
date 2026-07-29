package com.prodwatch.api.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.prodwatch.api.service.AuthContextService;
import com.prodwatch.api.util.CustomLogger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthContextService authContextService;

    /** Legacy shared secret (HS256) — used only when Supabase issues HS256 tokens. */
    private final Algorithm hmacAlgorithm;

    /** JWKS provider for asymmetric (ES256/RS256) Supabase signing keys. */
    private final JwkProvider jwkProvider;

    public JwtAuthFilter(
            AuthContextService authContextService,
            @Value("${SUPABASE_URL}") String supabaseUrl,
            @Value("${SUPABASE_JWT_SECRET:}") String jwtSecret) {
        this.authContextService = authContextService;
        this.hmacAlgorithm =
                (jwtSecret == null || jwtSecret.isBlank()) ? null : Algorithm.HMAC256(jwtSecret);

        String baseUrl = supabaseUrl.replaceAll("/$", "");
        URI jwksUri = URI.create(baseUrl + "/auth/v1/.well-known/jwks.json");
        try {
            this.jwkProvider = new GuavaCachedJwkProvider(new UrlJwkProvider(jwksUri.toURL()));
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Invalid SUPABASE_URL for JWKS: " + supabaseUrl, ex);
        }
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            DecodedJWT decoded = JWT.decode(token);
            Algorithm algorithm = resolveAlgorithm(decoded);
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            UUID userId = UUID.fromString(jwt.getSubject());

            authContextService
                    .resolve(userId)
                    .ifPresent(principal -> {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal, null, principal.getAuthorities());
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            CustomLogger.debug("JWT validation failed: " + ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Choose the verification algorithm based on the token header. Supabase projects using
     * asymmetric signing keys issue ES256/RS256 (verified via JWKS); legacy projects issue
     * HS256 (verified with the shared secret).
     */
    private Algorithm resolveAlgorithm(DecodedJWT decoded) {
        String alg = decoded.getAlgorithm();
        try {
            switch (alg) {
                case "ES256" -> {
                    Jwk jwk = jwkProvider.get(decoded.getKeyId());
                    return Algorithm.ECDSA256((ECPublicKey) jwk.getPublicKey(), null);
                }
                case "RS256" -> {
                    Jwk jwk = jwkProvider.get(decoded.getKeyId());
                    return Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
                }
                case "HS256" -> {
                    if (hmacAlgorithm == null) {
                        throw new JWTVerificationException("HS256 token received but no JWT secret configured");
                    }
                    return hmacAlgorithm;
                }
                default -> throw new JWTVerificationException("Unsupported JWT algorithm: " + alg);
            }
        } catch (JWTVerificationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JWTVerificationException("Unable to resolve signing key: " + ex.getMessage(), ex);
        }
    }
}
