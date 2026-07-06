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
package com.prodwatch.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the application context boots without errors.
    }
}
