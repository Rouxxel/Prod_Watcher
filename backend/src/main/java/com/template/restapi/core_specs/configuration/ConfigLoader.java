/**
 * #############################################################################
 * ### Configuration loader file
 * ###
 * ### @file ConfigLoader.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Loads configuration data from config_file.json (on the classpath) once, at
 * class-initialization time, and exposes it as a parsed JsonNode tree for the
 * rest of the application.
 *
 *   Java:    ConfigLoader.endpoint("example_endpoint_1").get("endpoint_route").asText()
 *
 * Note: CoreSpecsInitializer additionally bridges this same JSON into the
 * Spring Environment so the values can be referenced via ${config....}
 * placeholders inside controller annotations.
 */
package com.template.restapi.core_specs.configuration;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ConfigLoader {

    /** Classpath location of the config file (under src/main/resources). */
    private static final String CONFIG_PATH = "/core_specs/configuration/config_file.json";

    private static final JsonNode CONFIG = load();

    private ConfigLoader() { }

    /**
     * Reads and parses config_file.json. Mirrors read_data_from_config_json():
     * a failure here is fatal because nothing can run without configuration.
     */
    private static JsonNode load() {
        try (InputStream in = ConfigLoader.class.getResourceAsStream(CONFIG_PATH)) {
            if (in == null) {
                System.err.println("ERROR: Config file not found on classpath: " + CONFIG_PATH);
                System.exit(1);
            }
            return new ObjectMapper().readTree(in);
        } catch (Exception ex) {
            System.err.println("ERROR: Failed to parse JSON config file '" + CONFIG_PATH + "': " + ex.getMessage());
            System.exit(1);
            return null; // unreachable; keeps the compiler happy
        }
    }

    /** The entire parsed configuration tree (equivalent to the config_loader dict). */
    public static JsonNode get() {
        return CONFIG;
    }

    /** Convenience accessor for the "network" section. */
    public static JsonNode network() {
        return CONFIG.path("network");
    }

    /** Convenience accessor for the "logging" section. */
    public static JsonNode logging() {
        return CONFIG.path("logging");
    }

    /** Convenience accessor for the "email_validation" section. */
    public static JsonNode emailValidation() {
        return CONFIG.path("email_validation");
    }

    /** Convenience accessor for a single endpoint entry under "endpoints". */
    public static JsonNode endpoint(String key) {
        return CONFIG.path("endpoints").path(key);
    }
}
