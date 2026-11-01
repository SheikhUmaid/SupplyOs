package com.supplychain.tracking;

import com.supplychain.dispatch.Dispatch;
import com.supplychain.dispatch.ShipmentStatus;
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
@Table(name = "delivery_tracking")
public class DeliveryTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id")
    private Dispatch dispatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ShipmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private ShipmentStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private Instant createdAt;

    protected DeliveryTracking() {}

    public DeliveryTracking(
            Dispatch dispatch,
            ShipmentStatus fromStatus,
            ShipmentStatus toStatus,
            User updatedBy,
            String note,
            Instant createdAt) {
        this.dispatch = dispatch;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.updatedBy = updatedBy;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Dispatch getDispatch() {
        return dispatch;
    }

    public ShipmentStatus getFromStatus() {
        return fromStatus;
    }

    public ShipmentStatus getToStatus() {
        return toStatus;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
