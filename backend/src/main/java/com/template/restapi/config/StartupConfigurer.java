/**
 * #############################################################################
 * ### Startup configurer
 * ###
 * ### @file StartupConfigurer.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Runs once after the context is ready to push runtime configuration into the
 * stateless utility classes that need it:
 *   - Validators: allowed email providers / TLDs from config_file.json
 *
 * Add other one-time wiring here (e.g. SecureFileIo.setAllowedRoot to confine
 * file access to src/main/resources or a data directory).
 */
package com.template.restapi.config;

import java.util.ArrayList;
import java.util.List;

import com.template.restapi.core_specs.configuration.ConfigLoader;
import com.template.restapi.util.CustomLogger;
import com.template.restapi.util.Validators;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupConfigurer {

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        JsonNode emailCfg = ConfigLoader.emailValidation();
        Validators.setEmailValidationConfig(
                toStringList(emailCfg.path("allowed_providers")),
                toStringList(emailCfg.path("allowed_tlds")));

        CustomLogger.info("Startup configuration applied (email validation, etc.). Backend ready.");
    }

    private List<String> toStringList(JsonNode arrayNode) {
        List<String> out = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            arrayNode.forEach(n -> out.add(n.asText()));
        }
        return out;
    }
}
