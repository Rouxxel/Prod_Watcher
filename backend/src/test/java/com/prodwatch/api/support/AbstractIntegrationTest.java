package com.prodwatch.api.support;

import com.prodwatch.api.util.CustomLogger;

import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractIntegrationTest {

    @BeforeAll
    static void initLogger() {
        if (!CustomLogger.logDirectory.equals("logs-test")) {
            CustomLogger.setup("logs-test", "prodwatch_test", CustomLogger.LogLevel.ERROR);
        }
    }
}
