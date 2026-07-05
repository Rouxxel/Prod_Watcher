/**
 * #############################################################################
 * ### Create DTO
 * ###
 * ### @file ExampleItemCreate.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Request body accepted when creating a record (POST). The server assigns the
 * `id`, so it is intentionally NOT part of this payload.
 *
 * Jakarta Bean Validation annotations play the role of Pydantic's Field(...)
 * constraints; violations are turned into a 400 by GlobalExceptionHandler.
 */
package com.template.restapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExampleItemCreate(

        @NotBlank(message = "name must not be blank")
        @Size(min = 1, message = "name must be at least 1 character")
        String name,

        // Optional free-text description (may be null).
        String description
) {
}
