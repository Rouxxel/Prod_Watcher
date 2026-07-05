/**
 * #############################################################################
 * ### Example service
 * ###
 * ### @file ExampleItemService.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Business-logic layer between controllers and the repository. Controllers stay
 * thin (HTTP concerns only) while the service owns rules, validation calls,
 * id generation, and mapping to response DTOs.
 *
 */
package com.template.restapi.service;

import java.util.List;
import java.util.UUID;

import com.template.restapi.dto.ExampleItemCreate;
import com.template.restapi.dto.ExampleItemResponse;
import com.template.restapi.dto.ExampleItemUpdate;
import com.template.restapi.entity.ExampleItem;
import com.template.restapi.error.ResourceNotFoundException;
import com.template.restapi.repository.ExampleItemRepository;
import com.template.restapi.util.CustomLogger;

import org.springframework.stereotype.Service;

@Service
public class ExampleItemService {

    private final ExampleItemRepository repository;

    public ExampleItemService(ExampleItemRepository repository) {
        this.repository = repository;
    }

    public List<ExampleItemResponse> listAll() {
        CustomLogger.debug("Listing all example items");
        return repository.findAll().stream()
                .map(ExampleItemResponse::from)
                .toList();
    }

    public ExampleItemResponse getById(String id) {
        ExampleItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Example item with id '" + id + "' not found."));
        return ExampleItemResponse.from(item);
    }

    public ExampleItemResponse create(ExampleItemCreate body) {
        String id = UUID.randomUUID().toString();
        ExampleItem item = new ExampleItem(id, body.name(), body.description());
        repository.save(item);
        CustomLogger.info("Created example item with id '" + id + "'");
        return ExampleItemResponse.from(item);
    }

    public ExampleItemResponse update(String id, ExampleItemUpdate body) {
        ExampleItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Example item with id '" + id + "' not found."));

        // Partial update: only apply the fields that were actually provided.
        if (body.name() != null) {
            item.setName(body.name());
        }
        if (body.description() != null) {
            item.setDescription(body.description());
        }
        repository.save(item);
        CustomLogger.info("Updated example item with id '" + id + "'");
        return ExampleItemResponse.from(item);
    }

    public void delete(String id) {
        if (!repository.deleteById(id)) {
            throw new ResourceNotFoundException("Example item with id '" + id + "' not found.");
        }
        CustomLogger.info("Deleted example item with id '" + id + "'");
    }
}
