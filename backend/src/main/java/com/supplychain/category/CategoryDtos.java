package com.supplychain.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {}

    public record CategoryRequest(
            @NotBlank(message = "Enter a category name.") @Size(max = 100, message = "Use 100 characters or fewer.")
                    String name,
            @Size(max = 255, message = "Use 255 characters or fewer.") String description) {}

    public record CategoryResponse(
            long id, String name, @Schema(types = {"string", "null"}) String description) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
        }
    }
}
