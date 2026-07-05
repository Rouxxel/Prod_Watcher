/**
 * #############################################################################
 * ### Main backend file
 * ###
 * ### @file Application.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Application entry point. This is the file you "just run" to start the backend.
 *
 * Responsibilities:
 *   1. Initialize the CustomLogger from config_file.json BEFORE the context boots
 *      so every component logs through the same handler.
 *   2. Hand control to Spring Boot, which auto-discovers controllers, services,
 *      the rate-limit interceptor and the global exception handler.
 *   3. Register a shutdown hook so the logger flushes/closes cleanly on exit.
 *
 * How to run:
 *   ./gradlew bootRun              (development; see start.sh / start.bat)
 *   java -jar build/libs/app.jar   (after ./gradlew bootJar)
 */
package com.template.restapi;

import com.template.restapi.core_specs.configuration.ConfigLoader;
import com.template.restapi.util.CustomLogger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.JsonNode;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // --- 1. Bring the custom logger up first (mirrors importing log_handler) ---
        JsonNode logging = ConfigLoader.logging();
        CustomLogger.setup(
                logging.path("dir_name").asText("logs"),
                logging.path("log_file_name").asText("x_bcknd"),
                parseLevel(logging.path("logging_level").asText("debug")));

        // Flush/close the logger when the JVM stops (Ctrl+C, container stop, etc.).
        Runtime.getRuntime().addShutdownHook(new Thread(CustomLogger::shutdown));

        int port = ConfigLoader.network().path("server_port").asInt(8080);
        CustomLogger.info("Java REST API Template server starting on port " + port);

        // --- 2. Start Spring Boot ---
        SpringApplication.run(Application.class, args);
    }

    /** Maps the lowercase level string from config_file.json to the enum. */
    private static CustomLogger.LogLevel parseLevel(String level) {
        try {
            return CustomLogger.LogLevel.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CustomLogger.LogLevel.DEBUG;
        }
    }
}
