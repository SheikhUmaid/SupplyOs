package com.supplychain.allocation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AllocationDtos {

    private AllocationDtos() {}

    public record EvaluationResponse(
            long warehouseId,
            String warehouseName,
            boolean eligible,
            @Schema(types = {"string", "null"}) String rejectionReason,
            BigDecimal distanceKm,
            BigDecimal deliveryCost,
            BigDecimal etaHours,
            @Schema(types = {"number", "null"}) BigDecimal normDistance,
            @Schema(types = {"number", "null"}) BigDecimal normCost,
            @Schema(types = {"number", "null"}) BigDecimal inventoryFactor,
            @Schema(types = {"number", "null"}) BigDecimal priorityFactor,
            @Schema(types = {"number", "null"}) BigDecimal finalScore,
            @Schema(types = {"integer", "null"}) Integer rank,
            EvaluationDecision decision) {
        static EvaluationResponse from(AllocationEvaluation evaluation) {
            return new EvaluationResponse(
                    evaluation.getWarehouse().getId(),
                    evaluation.getWarehouse().getName(),
                    evaluation.isEligible(),
                    evaluation.getRejectionReason(),
                    evaluation.getDistanceKm(),
                    evaluation.getDeliveryCost(),
                    evaluation.getEtaHours(),
                    evaluation.getNormDistance(),
                    evaluation.getNormCost(),
                    evaluation.getInventoryFactor(),
                    evaluation.getPriorityFactor(),
                    evaluation.getFinalScore(),
                    evaluation.getRankPosition(),
                    evaluation.getDecision());
        }
    }

    public record WeightsResponse(BigDecimal distance, BigDecimal cost, BigDecimal inventory, BigDecimal priority) {}

    public record AllocationResponse(
            long id,
            long orderId,
            String orderNumber,
            int attemptNo,
            AllocationOutcome outcome,
            @Schema(types = {"string", "null"}) String selectedWarehouse,
            WeightsResponse weights,
            String explanation,
            Instant allocatedAt,
            String deliveryCity,
            BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude,
            List<EvaluationResponse> evaluations) {}

    public record AllocationSummaryResponse(
            long id,
            long orderId,
            String orderNumber,
            int attemptNo,
            AllocationOutcome outcome,
            @Schema(types = {"string", "null"}) String selectedWarehouse,
            Instant allocatedAt) {}

    public record AllocationSettingsResponse(WeightsResponse weights, double roadFactor, double speedKmh) {}
}
