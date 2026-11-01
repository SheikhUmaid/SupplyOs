package com.supplychain.allocation;

import com.supplychain.order.Order;
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
import java.time.Instant;

@Entity
@Table(name = "warehouse_allocations")
public class WarehouseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "attempt_no")
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private AllocationOutcome outcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_warehouse_id")
    private Warehouse selectedWarehouse;

    @Column(name = "weight_distance")
    private BigDecimal weightDistance;

    @Column(name = "weight_cost")
    private BigDecimal weightCost;

    @Column(name = "weight_inventory")
    private BigDecimal weightInventory;

    @Column(name = "weight_priority")
    private BigDecimal weightPriority;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "allocated_at")
    private Instant allocatedAt;

    protected WarehouseAllocation() {}

    public WarehouseAllocation(
            Order order,
            int attemptNo,
            AllocationOutcome outcome,
            Warehouse selectedWarehouse,
            BigDecimal weightDistance,
            BigDecimal weightCost,
            BigDecimal weightInventory,
            BigDecimal weightPriority,
            String explanation,
            Instant allocatedAt) {
        this.order = order;
        this.attemptNo = attemptNo;
        this.outcome = outcome;
        this.selectedWarehouse = selectedWarehouse;
        this.weightDistance = weightDistance;
        this.weightCost = weightCost;
        this.weightInventory = weightInventory;
        this.weightPriority = weightPriority;
        this.explanation = explanation;
        this.allocatedAt = allocatedAt;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public AllocationOutcome getOutcome() {
        return outcome;
    }

    public Warehouse getSelectedWarehouse() {
        return selectedWarehouse;
    }

    public BigDecimal getWeightDistance() {
        return weightDistance;
    }

    public BigDecimal getWeightCost() {
        return weightCost;
    }

    public BigDecimal getWeightInventory() {
        return weightInventory;
    }

    public BigDecimal getWeightPriority() {
        return weightPriority;
    }

    public String getExplanation() {
        return explanation;
    }

    public Instant getAllocatedAt() {
        return allocatedAt;
    }
}
