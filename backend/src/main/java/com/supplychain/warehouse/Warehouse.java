package com.supplychain.warehouse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "base_delivery_cost")
    private BigDecimal baseDeliveryCost;

    @Column(name = "cost_per_km")
    private BigDecimal costPerKm;

    @Column(name = "handling_hours")
    private int handlingHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WarehouseStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Warehouse() {}

    public Warehouse(
            String name,
            String address,
            String city,
            String state,
            String postalCode,
            BigDecimal latitude,
            BigDecimal longitude,
            String contactNumber,
            BigDecimal baseDeliveryCost,
            BigDecimal costPerKm,
            int handlingHours,
            WarehouseStatus status,
            Instant createdAt) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.contactNumber = contactNumber;
        this.baseDeliveryCost = baseDeliveryCost;
        this.costPerKm = costPerKm;
        this.handlingHours = handlingHours;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public BigDecimal getBaseDeliveryCost() {
        return baseDeliveryCost;
    }

    public void setBaseDeliveryCost(BigDecimal baseDeliveryCost) {
        this.baseDeliveryCost = baseDeliveryCost;
    }

    public BigDecimal getCostPerKm() {
        return costPerKm;
    }

    public void setCostPerKm(BigDecimal costPerKm) {
        this.costPerKm = costPerKm;
    }

    public int getHandlingHours() {
        return handlingHours;
    }

    public void setHandlingHours(int handlingHours) {
        this.handlingHours = handlingHours;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
