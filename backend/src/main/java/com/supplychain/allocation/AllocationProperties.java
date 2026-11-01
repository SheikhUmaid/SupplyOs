package com.supplychain.allocation;

import com.supplychain.allocation.engine.WarehouseScoringEngine;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Scoring settings from application.yml. Invalid values stop the application at startup. */
@ConfigurationProperties("app.allocation")
public record AllocationProperties(WarehouseScoringEngine.Weights weights, double roadFactor, double speedKmh) {

    public AllocationProperties {
        BigDecimal sum = weights.distance().add(weights.cost()).add(weights.inventory()).add(weights.priority());
        if (sum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("app.allocation.weights must add up to 1, but they add up to " + sum);
        }
        if (weights.distance().signum() < 0 || weights.cost().signum() < 0
                || weights.inventory().signum() < 0 || weights.priority().signum() < 0) {
            throw new IllegalArgumentException("app.allocation.weights can't be negative");
        }
        if (roadFactor < 1 || speedKmh <= 0) {
            throw new IllegalArgumentException("app.allocation needs road-factor >= 1 and speed-kmh > 0");
        }
    }
}
