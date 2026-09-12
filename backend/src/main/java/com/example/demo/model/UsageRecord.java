package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "usage_records", indexes = @Index(name = UsageRecord.CURRENT_CYCLE_INDEX, columnList = "type,customer_id,recorded_at"))
public class UsageRecord {

    public static final String CURRENT_CYCLE_INDEX = "idx_usage_records_type_customer_recorded_at";

    public enum Type {
        CALL, DATA, SMS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public UsageRecord() {
    }

    public UsageRecord(Customer customer, Type type, BigDecimal quantity, BigDecimal cost, Instant recordedAt) {
        this.customer = customer;
        this.type = type;
        this.quantity = quantity;
        this.cost = cost;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
