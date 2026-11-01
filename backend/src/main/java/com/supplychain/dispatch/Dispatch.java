package com.supplychain.dispatch;

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
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "dispatches")
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "tracking_reference")
    private String trackingReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShipmentStatus status;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "dispatch_date")
    private Instant dispatchDate;

    @Column(name = "delivery_date")
    private Instant deliveryDate;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Dispatch() {}

    public Dispatch(
            Order order,
            Warehouse warehouse,
            String trackingReference,
            ShipmentStatus status,
            LocalDate estimatedDeliveryDate,
            Instant createdAt) {
        this.order = order;
        this.warehouse = warehouse;
        this.trackingReference = trackingReference;
        this.status = status;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public String getTrackingReference() {
        return trackingReference;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public Instant getDispatchDate() {
        return dispatchDate;
    }

    public void setDispatchDate(Instant dispatchDate) {
        this.dispatchDate = dispatchDate;
    }

    public Instant getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Instant deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
