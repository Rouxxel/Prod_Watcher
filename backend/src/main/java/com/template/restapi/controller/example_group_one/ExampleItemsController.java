/**
 * #############################################################################
 * ### Example controller (group one)
 * ###
 * ### @file ExampleItemsController.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Reference controller for this template. Copy this file (and its matching
 * config_file.json entry) when adding new endpoint groups. Group controllers
 * live in their own package (folder) to keep the codebase organized rather than
 * one endless file.
 * "specific_router_group_*" folders.
 *
 * Demonstrates every pattern the template expects:
 *   - config-driven path/tag/rate-limit (ConfigLoader + ${config....} + @RateLimit)
 *   - request DTOs with bean validation (dto/ExampleItemCreate, ExampleItemUpdate)
 *   - response DTOs decoupled from the entity (dto/ExampleItemResponse)
 *   - delegation to a @Service (no business logic in the controller)
 *   - optional input checks via util/Validators
 *   - static reference data via DataLoader
 *   - structured logging via CustomLogger
 *
 * The path prefix and route come from config_file.json ->
 * endpoints.example_endpoint_1; the rate limit for every handler is enforced by
 * @RateLimit("example_endpoint_1").
 */
package com.template.restapi.controller.example_group_one;

import java.util.List;

import com.template.restapi.config.RateLimit;
import com.template.restapi.core_specs.data.DataLoader;
import com.template.restapi.dto.ExampleItemCreate;
import com.template.restapi.dto.ExampleItemResponse;
import com.template.restapi.dto.ExampleItemUpdate;
import com.template.restapi.service.ExampleItemService;
import com.template.restapi.util.CustomLogger;
import com.template.restapi.util.Validators;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${config.endpoints.example_endpoint_1.endpoint_prefix}")
@Tag(name = "example-items")
public class ExampleItemsController {

    private final ExampleItemService service;

    public ExampleItemsController(ExampleItemService service) {
        this.service = service;
    }

    /** List all example items. GET /subsection/items */
    @RateLimit("example_endpoint_1")
    @GetMapping("${config.endpoints.example_endpoint_1.endpoint_route}")
    public List<ExampleItemResponse> list() {
        CustomLogger.debug("Listing example items (supported languages: "
                + DataLoader.stringList("languages") + ")");
        return service.listAll();
    }

    /** Get a single item by id. GET /subsection/items/{id} */
    @RateLimit("example_endpoint_1")
    @GetMapping("${config.endpoints.example_endpoint_1.endpoint_route}/{id}")
    public ExampleItemResponse getById(@PathVariable String id) {
        return service.getById(id);
    }

    /**
     * Create an item. POST /subsection/items
     *
     * Shows two layers of validation working together:
     *   - bean validation on the DTO (@Valid -> 400 on constraint violations)
     *   - an optional Validators check on a query param (contactEmail)
     */
    @RateLimit("example_endpoint_1")
    @PostMapping("${config.endpoints.example_endpoint_1.endpoint_route}")
    @ResponseStatus(HttpStatus.CREATED)
    public ExampleItemResponse create(@Valid @RequestBody ExampleItemCreate body,
                                      @RequestParam(required = false) String contactEmail) {
        if (contactEmail != null && !contactEmail.isBlank()) {
            // Throws Validators.ValidationException -> mapped to 400 by the handler.
            Validators.validateEmailFormat(contactEmail);
        }
        return service.create(body);
    }

    /** Partial update. PATCH /subsection/items/{id} */
    @RateLimit("example_endpoint_1")
    @PatchMapping("${config.endpoints.example_endpoint_1.endpoint_route}/{id}")
    public ExampleItemResponse update(@PathVariable String id,
                                      @Valid @RequestBody ExampleItemUpdate body) {
        return service.update(id, body);
    }

    /** Delete. DELETE /subsection/items/{id} */
    @RateLimit("example_endpoint_1")
    @DeleteMapping("${config.endpoints.example_endpoint_1.endpoint_route}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
