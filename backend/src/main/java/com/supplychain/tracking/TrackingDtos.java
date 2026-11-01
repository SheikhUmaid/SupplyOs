package com.supplychain.tracking;

import com.supplychain.dispatch.ShipmentStatus;
import com.supplychain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class TrackingDtos {

    private TrackingDtos() {}

    /** `completedAt` is null until the step has really happened. */
    public record TimelineStepResponse(
            String key, String label, @Schema(types = {"string", "null"}) Instant completedAt) {}

    /** `updatedBy` and `note` are only filled in for administrators. */
    public record HistoryEntryResponse(
            @Schema(types = {"string", "null"}) ShipmentStatus fromStatus,
            ShipmentStatus toStatus,
            Instant at,
            @Schema(types = {"string", "null"}) String updatedBy,
            @Schema(types = {"string", "null"}) String note) {}

    /** The dispatch fields are null for an order that has no warehouse yet. */
    public record TrackingResponse(
            long orderId,
            String orderNumber,
            OrderStatus orderStatus,
            @Schema(types = {"string", "null"}) ShipmentStatus shipmentStatus,
            @Schema(types = {"string", "null"}) String trackingReference,
            @Schema(types = {"string", "null"}) String warehouseName,
            @Schema(types = {"string", "null"}) LocalDate estimatedDeliveryDate,
            @Schema(types = {"string", "null"}) Instant deliveryDate,
            List<TimelineStepResponse> steps,
            List<HistoryEntryResponse> history) {}
}
