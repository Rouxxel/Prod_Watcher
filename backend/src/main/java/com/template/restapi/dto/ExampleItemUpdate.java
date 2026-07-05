/**
 * #############################################################################
 * ### Update DTO
 * ###
 * ### @file ExampleItemUpdate.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Request body accepted for partial updates (PATCH). Equivalent to the Python
 * template's `ExampleItemUpdate`: every field is nullable and only the provided
 * (non-null) ones are applied. Mirror the editable fields of the entity here;
 * omit anything that must stay immutable.
 */
package com.template.restapi.dto;

import jakarta.validation.constraints.Size;

public record ExampleItemUpdate(

        @Size(min = 1, message = "name must be at least 1 character when provided")
        String name,

        String description
) {
}
