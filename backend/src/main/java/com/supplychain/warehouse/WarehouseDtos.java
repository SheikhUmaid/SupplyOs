package com.supplychain.warehouse;

import com.supplychain.inventory.InventoryRepository.WarehouseStock;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class WarehouseDtos {

    private WarehouseDtos() {}

    public record WarehouseRequest(
            @NotBlank(message = "Enter a warehouse name.") @Size(max = 120, message = "Use 120 characters or fewer.")
                    String name,
            @NotBlank(message = "Enter an address.") @Size(max = 255, message = "Use 255 characters or fewer.")
                    String address,
            @NotBlank(message = "Enter a city.") @Size(max = 100, message = "Use 100 characters or fewer.")
                    String city,
            @NotBlank(message = "Enter a state.") @Size(max = 100, message = "Use 100 characters or fewer.")
                    String state,
            @NotBlank(message = "Enter a PIN code.")
                    @Pattern(regexp = "^[1-9]\\d{5}$", message = "Enter a valid 6-digit PIN code.")
                    String postalCode,
            @NotNull(message = "Enter a latitude.")
                    @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90.")
                    @DecimalMax(value = "90", message = "Latitude must be between -90 and 90.")
                    @Digits(integer = 2, fraction = 6, message = "Use up to 6 decimals.")
                    BigDecimal latitude,
            @NotNull(message = "Enter a longitude.")
                    @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180.")
                    @DecimalMax(value = "180", message = "Longitude must be between -180 and 180.")
                    @Digits(integer = 3, fraction = 6, message = "Use up to 6 decimals.")
                    BigDecimal longitude,
            @NotBlank(message = "Enter a contact number.")
                    @Pattern(regexp = "^[+0-9 -]{7,20}$", message = "Enter a valid contact number.")
                    String contactNumber,
            @NotNull(message = "Enter a base delivery cost.")
                    @DecimalMin(value = "0.00", message = "The cost can't be negative.")
                    @Digits(integer = 8, fraction = 2, message = "Use up to 8 digits and 2 decimals.")
                    BigDecimal baseDeliveryCost,
            @NotNull(message = "Enter a cost per km.")
                    @DecimalMin(value = "0.00", message = "The cost can't be negative.")
                    @Digits(integer = 6, fraction = 2, message = "Use up to 6 digits and 2 decimals.")
                    BigDecimal costPerKm,
            @NotNull(message = "Enter the handling time.") @Min(value = 0, message = "The time can't be negative.")
                    Integer handlingHours,
            @Schema(description = "Defaults to ACTIVE.") WarehouseStatus status) {}

    public record StockSummaryResponse(
            long productCount, long totalAvailable, long totalReserved, long lowStockItems) {
        static final StockSummaryResponse EMPTY = new StockSummaryResponse(0, 0, 0, 0);

        static StockSummaryResponse from(WarehouseStock stock) {
            return stock == null
                    ? EMPTY
                    : new StockSummaryResponse(
                            stock.productCount(), stock.available(), stock.reserved(), stock.lowStock());
        }
    }

    public record WarehouseResponse(
            long id,
            String name,
            String address,
            String city,
            String state,
            String postalCode,
            BigDecimal latitude,
            BigDecimal longitude,
            String contactNumber,
            BigDecimal baseDeliveryCost,
            BigDecimal costPerKm,
            int handlingHours,
            WarehouseStatus status,
            Instant createdAt,
            StockSummaryResponse stock) {
        static WarehouseResponse from(Warehouse warehouse, StockSummaryResponse stock) {
            return new WarehouseResponse(
                    warehouse.getId(),
                    warehouse.getName(),
                    warehouse.getAddress(),
                    warehouse.getCity(),
                    warehouse.getState(),
                    warehouse.getPostalCode(),
                    warehouse.getLatitude(),
                    warehouse.getLongitude(),
                    warehouse.getContactNumber(),
                    warehouse.getBaseDeliveryCost(),
                    warehouse.getCostPerKm(),
                    warehouse.getHandlingHours(),
                    warehouse.getStatus(),
                    warehouse.getCreatedAt(),
                    stock);
        }
    }

    /** `fulfilmentRate` is delivered / allocated, or null until an order has been allocated. */
    public record WarehousePerformanceResponse(
            long allocatedOrders,
            long activeDispatches,
            long completedDispatches,
            @Schema(types = {"number", "null"}) Double fulfilmentRate) {}

    public record WarehouseDetailResponse(WarehouseResponse warehouse, WarehousePerformanceResponse performance) {}
}
