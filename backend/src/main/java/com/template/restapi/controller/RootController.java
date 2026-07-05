/**
 * #############################################################################
 * ### Root endpoint file
 * ###
 * ### @file RootController.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Health-check endpoint that confirms the backend is up. Path, tag, and rate limit all come from
 * config_file.json -> endpoints.root_directory_endpoint.
 *
 * The ${config....} placeholders are resolved from the Spring Environment,
 * which CoreSpecsInitializer populated from config_file.json at startup.
 */
package com.template.restapi.controller;

import java.util.Map;

import com.template.restapi.config.RateLimit;
import com.template.restapi.util.CustomLogger;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${config.endpoints.root_directory_endpoint.endpoint_prefix}")
@Tag(name = "root")
public class RootController {

    @RateLimit("root_directory_endpoint")
    @GetMapping("${config.endpoints.root_directory_endpoint.endpoint_route}")
    public Map<String, String> root() {
        CustomLogger.debug("Backend running successfully");
        return Map.of("message", "Backend running successfully, ready to use other endpoints");
    }
}
