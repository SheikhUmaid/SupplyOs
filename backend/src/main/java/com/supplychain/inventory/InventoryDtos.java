package com.supplychain.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class InventoryDtos {

    private InventoryDtos() {}

    public record InventoryCreateRequest(
            @NotNull(message = "Choose a product.") Long productId,
            @NotNull(message = "Choose a warehouse.") Long warehouseId,
            @NotNull(message = "Enter a quantity.") @Min(value = 0, message = "The quantity can't be negative.")
                    Integer availableQuantity,
            @NotNull(message = "Enter a reorder level.") @Min(value = 0, message = "The level can't be negative.")
                    Integer reorderLevel) {}

    /**
     * `type` is RESTOCK (quantity above zero) or ADJUSTMENT (positive or negative correction). Quantity
     * may be zero only to change the reorder level alone, which doesn't touch stock.
     */
    public record InventoryUpdateRequest(
            @NotNull(message = "Choose a change type.") StockChangeType type,
            @NotNull(message = "Enter a quantity.") Integer quantity,
            @Min(value = 0, message = "The level can't be negative.") Integer reorderLevel,
            @NotBlank(message = "Enter a reason.") @Size(max = 255, message = "Use 255 characters or fewer.")
                    String reason) {}

    public enum StockChangeType {
        RESTOCK,
        ADJUSTMENT
    }

    public record InventoryResponse(
            long id,
            long productId,
            String productName,
            String sku,
            long warehouseId,
            String warehouseName,
            int availableQuantity,
            int reservedQuantity,
            int reorderLevel,
            boolean lowStock,
            Instant lastUpdated) {
        static InventoryResponse from(Inventory inventory) {
            return new InventoryResponse(
                    inventory.getId(),
                    inventory.getProduct().getId(),
                    inventory.getProduct().getName(),
                    inventory.getProduct().getSku(),
                    inventory.getWarehouse().getId(),
                    inventory.getWarehouse().getName(),
                    inventory.getAvailableQuantity(),
                    inventory.getReservedQuantity(),
                    inventory.getReorderLevel(),
                    inventory.getAvailableQuantity() < inventory.getReorderLevel(),
                    inventory.getLastUpdated());
        }
    }

    public record InventoryTransactionResponse(
            long id,
            long inventoryId,
            String productName,
            String sku,
            String warehouseName,
            InventoryTransactionType type,
            int availableDelta,
            int reservedDelta,
            int availableAfter,
            int reservedAfter,
            @Schema(types = {"string", "null"}) String note,
            @Schema(types = {"string", "null"}) String orderNumber,
            @Schema(types = {"string", "null"}, description = "Null means the system made this change.")
                    String performedBy,
            Instant createdAt) {
        static InventoryTransactionResponse from(InventoryTransaction tx) {
            Inventory inventory = tx.getInventory();
            return new InventoryTransactionResponse(
                    tx.getId(),
                    inventory.getId(),
                    inventory.getProduct().getName(),
                    inventory.getProduct().getSku(),
                    inventory.getWarehouse().getName(),
                    tx.getType(),
                    tx.getAvailableDelta(),
                    tx.getReservedDelta(),
                    tx.getAvailableAfter(),
                    tx.getReservedAfter(),
                    tx.getNote(),
                    tx.getOrder() == null ? null : tx.getOrder().getOrderNumber(),
                    tx.getPerformedBy() == null ? null : tx.getPerformedBy().getFullName(),
                    tx.getCreatedAt());
        }
    }
}
