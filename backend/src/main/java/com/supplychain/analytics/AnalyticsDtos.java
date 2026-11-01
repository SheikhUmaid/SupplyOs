package com.supplychain.analytics;

import com.supplychain.common.PageResponse;
import com.supplychain.dispatch.DispatchDtos.DispatchResponse;
import com.supplychain.inventory.InventoryDtos.InventoryResponse;
import com.supplychain.order.OrderDtos.OrderSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {}

    public record DailyCountResponse(LocalDate date, long count) {}

    public record LabelledCountResponse(String key, long count) {}

    public record KpiResponse(
            long totalOrders,
            long pendingOrders,
            long totalProducts,
            long totalWarehouses,
            long totalInventory,
            long activeDispatches,
            long deliveredOrders,
            long lowStockItems) {}

    /** `fulfilmentRate` is delivered / allocated, or null until an order has been allocated. */
    public record WarehouseComparisonResponse(
            long id,
            String name,
            boolean active,
            long availableUnits,
            long reservedUnits,
            long allocatedOrders,
            long completedDispatches,
            @Schema(types = {"number", "null"}) Double fulfilmentRate) {}

    public record DashboardResponse(
            KpiResponse kpis,
            List<DailyCountResponse> orderActivity,
            List<WarehouseComparisonResponse> warehousePerformance,
            PageResponse<OrderSummaryResponse> recentOrders,
            PageResponse<InventoryResponse> inventoryAlerts,
            PageResponse<DispatchResponse> recentDispatches) {}

    public record AlertsResponse(long pendingOrders, long lowStockItems) {}

    public record TopProductResponse(long productId, String name, String sku, long units, BigDecimal revenue) {}

    /**
     * Orders placed in the period. Cancelled orders count towards the total and the status breakdown,
     * but not towards value or top products, because no sale took place.
     */
    public record OrderAnalyticsResponse(
            int days,
            long totalOrders,
            BigDecimal averageOrderValue,
            BigDecimal totalValue,
            @Schema(types = {"number", "null"}, description = "Share of orders that found a warehouse.")
                    Double allocationRate,
            @Schema(types = {"number", "null"}, description = "Share of orders that were cancelled.")
                    Double cancellationRate,
            @Schema(types = {"number", "null"}, description = "Average hours from placed to delivered.")
                    Double averageDeliveryHours,
            List<LabelledCountResponse> ordersByStatus,
            List<DailyCountResponse> ordersOverTime,
            List<TopProductResponse> topProducts) {}

    public record WarehouseAnalyticsResponse(int days, List<WarehouseComparisonResponse> warehouses) {}

    public record ProductStockResponse(long warehouseId, long available) {}

    public record ProductAvailabilityResponse(
            long productId, String name, long totalAvailable, List<ProductStockResponse> byWarehouse) {}

    public record WarehouseNameResponse(long id, String name) {}

    /** Units in and out of sellable or physical stock per day, read from the stock ledger. */
    public record StockMovementDayResponse(LocalDate date, long received, long shipped) {}

    public record InventoryAnalyticsResponse(
            int days,
            long totalAvailable,
            long totalReserved,
            long lowStockItems,
            List<InventoryResponse> lowStockProducts,
            List<WarehouseNameResponse> warehouses,
            List<ProductAvailabilityResponse> productAvailability,
            List<StockMovementDayResponse> stockMovement) {}

    public record DispatchAnalyticsResponse(
            int days,
            long activeDispatches,
            long completedDeliveries,
            long pendingShipments,
            @Schema(types = {"number", "null"}, description = "Share of deliveries on or before the estimate.")
                    Double onTimeRate,
            List<LabelledCountResponse> statusDistribution) {}
}
