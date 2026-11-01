package com.supplychain.order;

import com.supplychain.user.User;
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
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_warehouse_id")
    private Warehouse selectedWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private OrderPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "delivery_name")
    private String deliveryName;

    @Column(name = "delivery_phone")
    private String deliveryPhone;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_city")
    private String deliveryCity;

    @Column(name = "delivery_state")
    private String deliveryState;

    @Column(name = "delivery_postal_code")
    private String deliveryPostalCode;

    @Column(name = "delivery_latitude")
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude")
    private BigDecimal deliveryLongitude;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Order() {}

    public Order(
            String orderNumber,
            User customer,
            OrderStatus status,
            OrderPriority priority,
            PaymentStatus paymentStatus,
            BigDecimal totalAmount,
            String deliveryName,
            String deliveryPhone,
            String deliveryAddress,
            String deliveryCity,
            String deliveryState,
            String deliveryPostalCode,
            BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude,
            String idempotencyKey,
            Instant createdAt) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.status = status;
        this.priority = priority;
        this.paymentStatus = paymentStatus;
        this.totalAmount = totalAmount;
        this.deliveryName = deliveryName;
        this.deliveryPhone = deliveryPhone;
        this.deliveryAddress = deliveryAddress;
        this.deliveryCity = deliveryCity;
        this.deliveryState = deliveryState;
        this.deliveryPostalCode = deliveryPostalCode;
        this.deliveryLatitude = deliveryLatitude;
        this.deliveryLongitude = deliveryLongitude;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public User getCustomer() {
        return customer;
    }

    public Warehouse getSelectedWarehouse() {
        return selectedWarehouse;
    }

    public void setSelectedWarehouse(Warehouse selectedWarehouse) {
        this.selectedWarehouse = selectedWarehouse;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderPriority getPriority() {
        return priority;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getDeliveryName() {
        return deliveryName;
    }

    public String getDeliveryPhone() {
        return deliveryPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public String getDeliveryState() {
        return deliveryState;
    }

    public String getDeliveryPostalCode() {
        return deliveryPostalCode;
    }

    public BigDecimal getDeliveryLatitude() {
        return deliveryLatitude;
    }

    public BigDecimal getDeliveryLongitude() {
        return deliveryLongitude;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
