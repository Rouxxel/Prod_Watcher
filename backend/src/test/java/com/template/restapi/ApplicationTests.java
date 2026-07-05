/**
 * #############################################################################
 * ### Smoke test
 * ###
 * ### @file ApplicationTests.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Minimal context-load test: verifies the Spring application context starts
 * (all beans wire up, the core specs load, the interceptor registers). Run with
 * `./gradlew test`. Add real endpoint tests with MockMvc / WebTestClient here.
 */
package com.template.restapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the application context boots without errors.
    }
}
