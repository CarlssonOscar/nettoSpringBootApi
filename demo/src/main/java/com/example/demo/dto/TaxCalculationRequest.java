package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for tax calculation.
 * 
 * Frontend input:
 * - grossMonthlySalary: Monthly gross salary
 * - municipalityId: Selected municipality
 * - churchMember: Whether person is a Swedish church member
 * - isPensioner: Whether person is 65+ (affects job tax credit)
 */
public record TaxCalculationRequest(
        UUID municipalityId,
        BigDecimal grossMonthlySalary,
        boolean churchMember,
        boolean isPensioner
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
     * Simple constructor with defaults (not church member, not pensioner).
     */
    public TaxCalculationRequest(UUID municipalityId, BigDecimal grossMonthlySalary) {
        this(municipalityId, grossMonthlySalary, false, false);
    }
}
