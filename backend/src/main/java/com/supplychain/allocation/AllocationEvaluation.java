package com.supplychain.allocation;

import com.supplychain.warehouse.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "allocation_evaluations")
public class AllocationEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private WarehouseAllocation allocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "eligible")
    private boolean eligible;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "distance_km")
    private BigDecimal distanceKm;

    @Column(name = "delivery_cost")
    private BigDecimal deliveryCost;

    @Column(name = "eta_hours")
    private BigDecimal etaHours;

    @Column(name = "norm_distance")
    private BigDecimal normDistance;

    @Column(name = "norm_cost")
    private BigDecimal normCost;

    @Column(name = "inventory_factor")
    private BigDecimal inventoryFactor;

    @Column(name = "priority_factor")
    private BigDecimal priorityFactor;

    @Column(name = "final_score")
    private BigDecimal finalScore;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private EvaluationDecision decision;

    protected AllocationEvaluation() {}

    public AllocationEvaluation(
            WarehouseAllocation allocation,
            Warehouse warehouse,
            boolean eligible,
            String rejectionReason,
            BigDecimal distanceKm,
            BigDecimal deliveryCost,
            BigDecimal etaHours,
            BigDecimal normDistance,
            BigDecimal normCost,
            BigDecimal inventoryFactor,
            BigDecimal priorityFactor,
            BigDecimal finalScore,
            Integer rankPosition,
            EvaluationDecision decision) {
        this.allocation = allocation;
        this.warehouse = warehouse;
        this.eligible = eligible;
        this.rejectionReason = rejectionReason;
        this.distanceKm = distanceKm;
        this.deliveryCost = deliveryCost;
        this.etaHours = etaHours;
        this.normDistance = normDistance;
        this.normCost = normCost;
        this.inventoryFactor = inventoryFactor;
        this.priorityFactor = priorityFactor;
        this.finalScore = finalScore;
        this.rankPosition = rankPosition;
        this.decision = decision;
    }

    public Long getId() {
        return id;
    }

    public WarehouseAllocation getAllocation() {
        return allocation;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public boolean isEligible() {
        return eligible;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public BigDecimal getDeliveryCost() {
        return deliveryCost;
    }

    public BigDecimal getEtaHours() {
        return etaHours;
    }

    public BigDecimal getNormDistance() {
        return normDistance;
    }

    public BigDecimal getNormCost() {
        return normCost;
    }

    public BigDecimal getInventoryFactor() {
        return inventoryFactor;
    }

    public BigDecimal getPriorityFactor() {
        return priorityFactor;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public EvaluationDecision getDecision() {
        return decision;
    }
}
