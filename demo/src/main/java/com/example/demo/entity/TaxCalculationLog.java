package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tax_calculation_log")
public class TaxCalculationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipality_id", insertable = false, updatable = false)
    private Municipality municipality;

    @Column(name = "municipality_id")
    private UUID municipalityId;

    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "total_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTax;

    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    // Constructors
    public TaxCalculationLog() {}

    public TaxCalculationLog(Municipality municipality, BigDecimal grossSalary, BigDecimal totalTax, BigDecimal netSalary) {
        this.municipality = municipality;
        this.grossSalary = grossSalary;
        this.totalTax = totalTax;
        this.netSalary = netSalary;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Municipality getMunicipality() {
        return municipality;
    }

    public void setMunicipality(Municipality municipality) {
        this.municipality = municipality;
        if (municipality != null) {
            this.municipalityId = municipality.getId();
        }
    }

    public UUID getMunicipalityId() {
        return municipalityId;
    }

    public void setMunicipalityId(UUID municipalityId) {
        this.municipalityId = municipalityId;
    }

    public BigDecimal getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(BigDecimal grossSalary) {
        this.grossSalary = grossSalary;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }
}
