package com.supplychain.dispatch;

import com.supplychain.order.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DispatchDtos {

    private DispatchDtos() {}

    public record DispatchStatusRequest(
            @NotNull(message = "Choose a status.") ShipmentStatus status,
            @Size(max = 255, message = "Use 255 characters or fewer.") String note) {}

    public record DispatchResponse(
            long id,
            String trackingReference,
            long orderId,
            String orderNumber,
            String customerName,
            String deliveryCity,
            long warehouseId,
            String warehouseName,
            ShipmentStatus status,
            List<ShipmentStatus> allowedNextStatuses,
            @Schema(types = {"string", "null"}) Instant dispatchDate,
            LocalDate estimatedDeliveryDate,
            @Schema(types = {"string", "null"}) Instant deliveryDate,
            Instant createdAt) {
        static DispatchResponse from(Dispatch dispatch) {
            Order order = dispatch.getOrder();
            return new DispatchResponse(
                    dispatch.getId(),
                    dispatch.getTrackingReference(),
                    order.getId(),
                    order.getOrderNumber(),
                    order.getCustomer().getFullName(),
                    order.getDeliveryCity(),
                    dispatch.getWarehouse().getId(),
                    dispatch.getWarehouse().getName(),
                    dispatch.getStatus(),
                    dispatch.getStatus().next().stream().toList(),
                    dispatch.getDispatchDate(),
                    dispatch.getEstimatedDeliveryDate(),
                    dispatch.getDeliveryDate(),
                    dispatch.getCreatedAt());
        }
    }
}
