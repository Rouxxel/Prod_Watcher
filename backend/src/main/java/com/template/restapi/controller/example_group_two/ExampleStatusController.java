/**
 * #############################################################################
 * ### Example controller (group two)
 * ###
 * ### @file ExampleStatusController.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * A second, minimal endpoint group. It exists to show that each group
 * lives in its own package, has its own config_file.json entry, and its own
 * rate limit, without crowding the first group's controller.
 *
 * Path/tag/rate-limit come from config_file.json -> endpoints.example_endpoint_2.
 */
package com.template.restapi.controller.example_group_two;

import java.util.Map;

import com.template.restapi.config.RateLimit;
import com.template.restapi.core_specs.data.DataLoader;
import com.template.restapi.util.CustomLogger;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${config.endpoints.example_endpoint_2.endpoint_prefix}")
@Tag(name = "example-status")
public class ExampleStatusController {

    /** Simple status payload. GET /subsection/status */
    @RateLimit("example_endpoint_2")
    @GetMapping("${config.endpoints.example_endpoint_2.endpoint_route}")
    public Map<String, Object> status() {
        CustomLogger.debug("Status endpoint called");
        return Map.of(
                "status", "ok",
                "maintainer", DataLoader.get().path("metadata").path("maintainer").asText("unknown"),
                "supported_languages", DataLoader.stringList("languages"));
    }
}
