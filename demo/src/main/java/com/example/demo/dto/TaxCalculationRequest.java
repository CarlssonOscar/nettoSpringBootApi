package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for tax calculation.
 */
public record TaxCalculationRequest(
        UUID municipalityId,
        BigDecimal grossMonthlySalary,
        boolean churchMember,
        UUID churchId,
        int birthYear
) {
    public TaxCalculationRequest {
        if (grossMonthlySalary == null || grossMonthlySalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Gross salary must be a positive value");
        }
        if (municipalityId == null) {
            throw new IllegalArgumentException("Municipality ID is required");
        }
    }

    /**
     * Constructor without church membership (defaults to false).
     */
    public TaxCalculationRequest(UUID municipalityId, BigDecimal grossMonthlySalary, int birthYear) {
        this(municipalityId, grossMonthlySalary, false, null, birthYear);
    }
}
