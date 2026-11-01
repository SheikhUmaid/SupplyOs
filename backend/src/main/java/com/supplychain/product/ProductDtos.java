package com.supplychain.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class ProductDtos {

    private ProductDtos() {}

    public record ProductRequest(
            @NotBlank(message = "Enter a product name.") @Size(max = 160, message = "Use 160 characters or fewer.")
                    String name,
            @NotBlank(message = "Enter a SKU.")
                    @Size(max = 64, message = "Use 64 characters or fewer.")
                    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Use letters, numbers, dots, dashes or underscores.")
                    String sku,
            String description,
            @NotNull(message = "Choose a category.") Long categoryId,
            @NotNull(message = "Enter a price.")
                    @DecimalMin(value = "0.00", message = "The price can't be negative.")
                    @Digits(integer = 10, fraction = 2, message = "Use up to 10 digits and 2 decimals.")
                    BigDecimal price,
            // Excludes schemes like javascript:, since this value is rendered as a link target.
            @Size(max = 500, message = "Use 500 characters or fewer.")
                    @Pattern(regexp = "^(https?://|/).*$", message = "Use a web address or a path starting with /.")
                    String imageUrl,
            @Schema(description = "Defaults to ACTIVE.") ProductStatus status) {}

    public record ProductResponse(
            long id,
            String name,
            String sku,
            @Schema(types = {"string", "null"}) String description,
            long categoryId,
            String categoryName,
            BigDecimal price,
            @Schema(types = {"string", "null"}) String imageUrl,
            ProductStatus status,
            Availability availability,
            Instant createdAt,
            Instant updatedAt) {}
}
