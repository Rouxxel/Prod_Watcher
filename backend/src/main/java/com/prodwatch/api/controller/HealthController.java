package com.prodwatch.api.controller;

import java.util.Map;

import com.prodwatch.api.config.RateLimit;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "${config.endpoints.health_endpoint.endpoint_prefix}${config.endpoints.health_endpoint.endpoint_route}")
@Tag(name = "health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @RateLimit("health_endpoint")
    @GetMapping
    public Map<String, Object> health() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Map.of(
                "status", "ok",
                "database", result != null && result == 1 ? "connected" : "unknown");
    }
}
