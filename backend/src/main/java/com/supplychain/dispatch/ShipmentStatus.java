package com.supplychain.dispatch;

import java.util.Optional;

/** A shipment moves forward one step at a time. CANCELLED is reached only by cancelling the order. */
public enum ShipmentStatus {
    PENDING,
    PACKED,
    DISPATCHED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED;

    public Optional<ShipmentStatus> next() {
        return switch (this) {
            case PENDING -> Optional.of(PACKED);
            case PACKED -> Optional.of(DISPATCHED);
            case DISPATCHED -> Optional.of(IN_TRANSIT);
            case IN_TRANSIT -> Optional.of(DELIVERED);
            case DELIVERED, CANCELLED -> Optional.empty();
        };
    }
}
