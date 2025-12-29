package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
        @NotNull(message = "Municipality ID is required")
        UUID municipalityId,
        
        @NotNull(message = "Gross monthly salary is required")
        @Positive(message = "Gross salary must be a positive value")
        BigDecimal grossMonthlySalary,
        
        boolean churchMember,
        boolean isPensioner
) {
    /**
     * Compact constructor - validation is now handled by Bean Validation annotations.
     */
    public TaxCalculationRequest {
        // Bean Validation handles null and positive checks
    }

    /**
     * Simple constructor with defaults (not church member, not pensioner).
     */
    public TaxCalculationRequest(UUID municipalityId, BigDecimal grossMonthlySalary) {
        this(municipalityId, grossMonthlySalary, false, false);
    }
}
