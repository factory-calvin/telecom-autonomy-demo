package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "data_limit_gb")
    private Integer dataLimitGb;

    @Column(name = "minutes_limit")
    private Integer minutesLimit;

    @Column(name = "sms_limit")
    private Integer smsLimit;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Plan() {}

    public Plan(String name, BigDecimal monthlyPrice, Integer dataLimitGb, Integer minutesLimit, Integer smsLimit) {
        this.name = name;
        this.monthlyPrice = monthlyPrice;
        this.dataLimitGb = dataLimitGb;
        this.minutesLimit = minutesLimit;
        this.smsLimit = smsLimit;
        this.isActive = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }

    public Integer getDataLimitGb() { return dataLimitGb; }
    public void setDataLimitGb(Integer dataLimitGb) { this.dataLimitGb = dataLimitGb; }

    public Integer getMinutesLimit() { return minutesLimit; }
    public void setMinutesLimit(Integer minutesLimit) { this.minutesLimit = minutesLimit; }

    public Integer getSmsLimit() { return smsLimit; }
    public void setSmsLimit(Integer smsLimit) { this.smsLimit = smsLimit; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
