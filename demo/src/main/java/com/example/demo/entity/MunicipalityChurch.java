package com.example.demo.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "municipality_church")
@IdClass(MunicipalityChurch.MunicipalityChurchId.class)
public class MunicipalityChurch {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipality_id", nullable = false)
    private Municipality municipality;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @Id
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "fee_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal feeRate;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public MunicipalityChurch() {}

    public MunicipalityChurch(Municipality municipality, Church church, BigDecimal feeRate, LocalDate validFrom) {
        this.municipality = municipality;
        this.church = church;
        this.feeRate = feeRate;
        this.validFrom = validFrom;
    }

    // Getters and Setters
    public Municipality getMunicipality() {
        return municipality;
    }

    public void setMunicipality(Municipality municipality) {
        this.municipality = municipality;
    }

    public Church getChurch() {
        return church;
    }

    public void setChurch(Church church) {
        this.church = church;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public BigDecimal getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(BigDecimal feeRate) {
        this.feeRate = feeRate;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Composite primary key class
    public static class MunicipalityChurchId implements Serializable {
        private UUID municipality;
        private UUID church;
        private LocalDate validFrom;

        public MunicipalityChurchId() {}

        public MunicipalityChurchId(UUID municipality, UUID church, LocalDate validFrom) {
            this.municipality = municipality;
            this.church = church;
            this.validFrom = validFrom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MunicipalityChurchId that = (MunicipalityChurchId) o;
            return Objects.equals(municipality, that.municipality) &&
                   Objects.equals(church, that.church) &&
                   Objects.equals(validFrom, that.validFrom);
        }

        @Override
        public int hashCode() {
            return Objects.hash(municipality, church, validFrom);
        }
    }
}
