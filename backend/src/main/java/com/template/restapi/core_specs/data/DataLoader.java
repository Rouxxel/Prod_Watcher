/**
 * #############################################################################
 * ### Data loader file
 * ###
 * ### @file DataLoader.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Loads general (non-environment-specific) reference data from
 * general_data.json on the classpath
 *
 * Use it for static lookup tables such as supported languages:
 *   Java:    DataLoader.get().get("languages")
 */
package com.template.restapi.core_specs.data;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DataLoader {

    private static final String DATA_PATH = "/core_specs/data/general_data.json";

    private static final JsonNode DATA = load();

    private DataLoader() { }

    private static JsonNode load() {
        try (InputStream in = DataLoader.class.getResourceAsStream(DATA_PATH)) {
            if (in == null) {
                System.err.println("ERROR: Data file not found on classpath: " + DATA_PATH);
                System.exit(1);
            }
            return new ObjectMapper().readTree(in);
        } catch (Exception ex) {
            System.err.println("ERROR: Failed to parse JSON data file '" + DATA_PATH + "': " + ex.getMessage());
            System.exit(1);
            return null; // unreachable
        }
    }

    /** The entire parsed data tree (equivalent to the data_loader dict). */
    public static JsonNode get() {
        return DATA;
    }

    /** Convenience helper: read a top-level string array (e.g. "languages"). */
    public static List<String> stringList(String key) {
        List<String> out = new ArrayList<>();
        JsonNode node = DATA.path(key);
        if (node.isArray()) {
            node.forEach(item -> out.add(item.asText()));
        }
        return out;
    }
}
