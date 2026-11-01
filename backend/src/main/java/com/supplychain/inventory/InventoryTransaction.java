package com.supplychain.inventory;

import com.supplychain.order.Order;
import com.supplychain.user.User;
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
import java.time.Instant;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private InventoryTransactionType type;

    @Column(name = "available_delta")
    private int availableDelta;

    @Column(name = "reserved_delta")
    private int reservedDelta;

    @Column(name = "available_after")
    private int availableAfter;

    @Column(name = "reserved_after")
    private int reservedAfter;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private Instant createdAt;

    protected InventoryTransaction() {}

    public InventoryTransaction(
            Inventory inventory,
            Order order,
            User performedBy,
            InventoryTransactionType type,
            int availableDelta,
            int reservedDelta,
            int availableAfter,
            int reservedAfter,
            String note,
            Instant createdAt) {
        this.inventory = inventory;
        this.order = order;
        this.performedBy = performedBy;
        this.type = type;
        this.availableDelta = availableDelta;
        this.reservedDelta = reservedDelta;
        this.availableAfter = availableAfter;
        this.reservedAfter = reservedAfter;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Order getOrder() {
        return order;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public InventoryTransactionType getType() {
        return type;
    }

    public int getAvailableDelta() {
        return availableDelta;
    }

    public int getReservedDelta() {
        return reservedDelta;
    }

    public int getAvailableAfter() {
        return availableAfter;
    }

    public int getReservedAfter() {
        return reservedAfter;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
