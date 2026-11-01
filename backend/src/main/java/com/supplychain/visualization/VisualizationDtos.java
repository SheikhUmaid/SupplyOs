package com.supplychain.visualization;

import com.supplychain.dispatch.ShipmentStatus;
import com.supplychain.warehouse.WarehouseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class VisualizationDtos {

    private VisualizationDtos() {}

    public record NetworkWarehouseResponse(
            long id,
            String name,
            String city,
            String state,
            BigDecimal latitude,
            BigDecimal longitude,
            WarehouseStatus status,
            long productCount,
            long inStockProducts,
            long totalAvailable,
            long totalReserved,
            long lowStockItems,
            long activeOrders,
            long activeDispatches) {}

    public record NetworkShipmentResponse(
            long dispatchId,
            String trackingReference,
            long orderId,
            String orderNumber,
            long warehouseId,
            String warehouseName,
            String destinationCity,
            BigDecimal latitude,
            BigDecimal longitude,
            ShipmentStatus status,
            LocalDate estimatedDeliveryDate) {}

    public record NetworkResponse(
            List<NetworkWarehouseResponse> warehouses, List<NetworkShipmentResponse> shipments) {}
}
