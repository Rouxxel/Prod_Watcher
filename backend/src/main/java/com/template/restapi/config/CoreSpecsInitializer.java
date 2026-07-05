/**
 * #############################################################################
 * ### Core specs -> Spring Environment bridge
 * ###
 * ### @file CoreSpecsInitializer.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Flattens config_file.json and general_data.json into the Spring Environment
 * BEFORE the application context is created, so their values can be referenced
 * with ${...} placeholders in annotations (e.g. a controller's request path).
 *
 * Why this exists:
 *   Spring annotations are evaluated by the framework, so to keep paths 
 *   "config-driven" the values must live in the Environment. This 
 *   initializer makes that happen with two prefixes:
 *
 *     config.*   <- everything in config_file.json
 *     data.*     <- everything in general_data.json
 *
 *   Example resolved key:  config.endpoints.example_endpoint_1.endpoint_route
 *
 * It also maps a couple of network settings onto Spring's own keys
 * (server.port / server.address) so config_file.json controls the bound port.
 *
 * Registered via application.properties:
 *   context.initializer.classes=com.template.restapi.config.CoreSpecsInitializer
 */
package com.template.restapi.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CoreSpecsInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment env = context.getEnvironment();
        Map<String, Object> props = new HashMap<>();

        flattenResource("/core_specs/configuration/config_file.json", "config", props);
        flattenResource("/core_specs/data/general_data.json", "data", props);

        // Let config_file.json drive the bound server port/host (Spring's own keys).
        Object port = props.get("config.network.server_port");
        if (port != null) {
            props.put("server.port", port);
        }
        Object host = props.get("config.network.host");
        if (host != null) {
            props.put("server.address", host);
        }
        Object contextPath = props.get("config.network.context_path");
        if (contextPath != null && !contextPath.toString().isEmpty()) {
            props.put("server.servlet.context-path", contextPath);
        }

        // Highest precedence among defaults but still overridable by real env vars.
        env.getPropertySources().addFirst(new MapPropertySource("coreSpecs", props));
    }

    private void flattenResource(String classpath, String prefix, Map<String, Object> out) {
        try (InputStream in = getClass().getResourceAsStream(classpath)) {
            if (in == null) {
                System.err.println("WARNING: core spec not found on classpath: " + classpath);
                return;
            }
            flatten(prefix, MAPPER.readTree(in), out);
        } catch (Exception ex) {
            System.err.println("WARNING: failed to flatten '" + classpath + "': " + ex.getMessage());
        }
    }

    /** Recursively turns a JSON tree into dotted property keys. */
    private void flatten(String prefix, JsonNode node, Map<String, Object> out) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                flatten(prefix + "." + field.getKey(), field.getValue(), out);
            }
        } else if (node.isArray()) {
            // Expose both an indexed form and a comma-joined form for convenience.
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < node.size(); i++) {
                flatten(prefix + "[" + i + "]", node.get(i), out);
                if (i > 0) joined.append(',');
                joined.append(node.get(i).asText());
            }
            out.put(prefix, joined.toString());
        } else {
            out.put(prefix, node.asText());
        }
    }
}
