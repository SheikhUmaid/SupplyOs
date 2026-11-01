package com.supplychain.dispatch;

import static org.assertj.core.api.Assertions.assertThat;

import com.supplychain.order.OrderPriority;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ShipmentStatusTest {

    @Test
    void theLifecycleMovesForwardOneStepAtATime() {
        assertThat(ShipmentStatus.PENDING.next()).contains(ShipmentStatus.PACKED);
        assertThat(ShipmentStatus.PACKED.next()).contains(ShipmentStatus.DISPATCHED);
        assertThat(ShipmentStatus.DISPATCHED.next()).contains(ShipmentStatus.IN_TRANSIT);
        assertThat(ShipmentStatus.IN_TRANSIT.next()).contains(ShipmentStatus.DELIVERED);
    }

    @Test
    void deliveredAndCancelledAreFinal() {
        assertThat(ShipmentStatus.DELIVERED.next()).isEmpty();
        assertThat(ShipmentStatus.CANCELLED.next()).isEmpty();
    }

    @Test
    void estimatedDeliveryRoundsUpToWholeDaysAndAddsAMarginForStandard() {
        Instant morning = Instant.parse("2026-03-01T04:00:00Z");
        LocalDate today = LocalDate.of(2026, 3, 1);

        assertThat(DispatchService.estimatedDeliveryDate(OrderPriority.STANDARD, new BigDecimal("15.66"), morning))
                .isEqualTo(today.plusDays(2));
        assertThat(DispatchService.estimatedDeliveryDate(OrderPriority.EXPRESS, new BigDecimal("15.66"), morning))
                .isEqualTo(today.plusDays(1));
        assertThat(DispatchService.estimatedDeliveryDate(OrderPriority.EXPRESS, new BigDecimal("25.00"), morning))
                .isEqualTo(today.plusDays(2));
    }

    @Test
    void theDateIsCountedInIndianTime() {
        // 20:00 UTC on 1 March is already 01:30 on 2 March in India.
        Instant lateUtc = Instant.parse("2026-03-01T20:00:00Z");

        assertThat(DispatchService.estimatedDeliveryDate(OrderPriority.EXPRESS, new BigDecimal("1.00"), lateUtc))
                .isEqualTo(LocalDate.of(2026, 3, 3));
    }
}
