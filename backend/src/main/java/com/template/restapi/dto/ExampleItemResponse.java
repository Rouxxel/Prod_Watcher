/**
 * #############################################################################
 * ### Response DTO
 * ###
 * ### @file ExampleItemResponse.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * The shape the API returns. Kept separate from the entity so internal/storage
 * fields can be hidden or response-only fields added later without changing how
 * records are persisted.
 */
package com.template.restapi.dto;

import com.template.restapi.entity.ExampleItem;

public record ExampleItemResponse(
        String id,
        String name,
        String description
) {
    /** Maps a stored entity to its API response shape. */
    public static ExampleItemResponse from(ExampleItem item) {
        return new ExampleItemResponse(item.getId(), item.getName(), item.getDescription());
    }
}
