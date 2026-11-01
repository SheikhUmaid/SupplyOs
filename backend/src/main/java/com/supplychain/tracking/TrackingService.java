package com.supplychain.tracking;

import com.supplychain.allocation.AllocationOutcome;
import com.supplychain.allocation.WarehouseAllocation;
import com.supplychain.allocation.WarehouseAllocationRepository;
import com.supplychain.dispatch.Dispatch;
import com.supplychain.dispatch.DispatchRepository;
import com.supplychain.dispatch.ShipmentStatus;
import com.supplychain.exception.ApiException;
import com.supplychain.order.Order;
import com.supplychain.order.OrderService;
import com.supplychain.order.OrderStatus;
import com.supplychain.security.AuthenticatedUser;
import com.supplychain.tracking.TrackingDtos.HistoryEntryResponse;
import com.supplychain.tracking.TrackingDtos.TimelineStepResponse;
import com.supplychain.tracking.TrackingDtos.TrackingResponse;
import com.supplychain.user.UserRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the timeline entirely from stored records. A step has a time only once it really happened. */
@Service
public class TrackingService {

    private final OrderService orderService;
    private final DispatchRepository dispatches;
    private final DeliveryTrackingRepository tracking;
    private final WarehouseAllocationRepository allocations;

    public TrackingService(
            OrderService orderService,
            DispatchRepository dispatches,
            DeliveryTrackingRepository tracking,
            WarehouseAllocationRepository allocations) {
        this.orderService = orderService;
        this.dispatches = dispatches;
        this.tracking = tracking;
        this.allocations = allocations;
    }

    @Transactional(readOnly = true)
    public TrackingResponse forOrder(long orderId, AuthenticatedUser viewer) {
        Order order = orderService.requireVisible(orderId, viewer);
        Optional<Dispatch> dispatch = dispatches.findByOrderId(orderId);
        List<DeliveryTracking> history =
                dispatch.map(found -> tracking.findHistory(found.getId())).orElse(List.of());
        boolean admin = viewer.role() == UserRole.ADMIN;

        return new TrackingResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                dispatch.map(Dispatch::getStatus).orElse(null),
                dispatch.map(Dispatch::getTrackingReference).orElse(null),
                dispatch.map(found -> found.getWarehouse().getName()).orElse(null),
                dispatch.map(Dispatch::getEstimatedDeliveryDate).orElse(null),
                dispatch.map(Dispatch::getDeliveryDate).orElse(null),
                timeline(order, history),
                history.stream().map(entry -> toHistoryEntry(entry, admin)).toList());
    }

    @Transactional(readOnly = true)
    public TrackingResponse forDispatch(long dispatchId, AuthenticatedUser viewer) {
        long orderId = dispatches.findOrderIdById(dispatchId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This dispatch doesn't exist."));
        return forOrder(orderId, viewer);
    }

    private List<TimelineStepResponse> timeline(Order order, List<DeliveryTracking> history) {
        Instant allocatedAt = allocations.findLatestByOrderId(order.getId())
                .filter(allocation -> allocation.getOutcome() == AllocationOutcome.ALLOCATED)
                .map(WarehouseAllocation::getAllocatedAt)
                .orElse(null);

        List<TimelineStepResponse> steps = new ArrayList<>();
        steps.add(new TimelineStepResponse("PLACED", "Order placed", order.getCreatedAt()));
        steps.add(new TimelineStepResponse("CONFIRMED", "Order confirmed", order.getConfirmedAt()));
        steps.add(new TimelineStepResponse("ALLOCATED", "Warehouse allocated", allocatedAt));
        steps.add(new TimelineStepResponse("PACKED", "Packed", reachedAt(history, ShipmentStatus.PACKED)));
        steps.add(new TimelineStepResponse("DISPATCHED", "Dispatched", reachedAt(history, ShipmentStatus.DISPATCHED)));
        steps.add(new TimelineStepResponse("IN_TRANSIT", "In transit", reachedAt(history, ShipmentStatus.IN_TRANSIT)));
        steps.add(new TimelineStepResponse("DELIVERED", "Delivered", reachedAt(history, ShipmentStatus.DELIVERED)));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            steps.add(new TimelineStepResponse("CANCELLED", "Cancelled", order.getCancelledAt()));
        }
        return steps;
    }

    private static Instant reachedAt(List<DeliveryTracking> history, ShipmentStatus status) {
        return history.stream()
                .filter(entry -> entry.getToStatus() == status)
                .map(DeliveryTracking::getCreatedAt)
                .findFirst()
                .orElse(null);
    }

    /** Who made a change, and any internal note, are operational details for administrators only. */
    private static HistoryEntryResponse toHistoryEntry(DeliveryTracking entry, boolean admin) {
        String updatedBy = entry.getUpdatedBy() == null ? "System" : entry.getUpdatedBy().getFullName();
        return new HistoryEntryResponse(
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getCreatedAt(),
                admin ? updatedBy : null,
                admin ? entry.getNote() : null);
    }
}
