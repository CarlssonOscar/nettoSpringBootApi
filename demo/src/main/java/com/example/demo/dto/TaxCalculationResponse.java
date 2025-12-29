package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO with detailed tax calculation breakdown.
 */
public record TaxCalculationResponse(
        // Input
        UUID municipalityId,
        String municipalityName,
        String regionName,
        BigDecimal grossMonthlySalary,
        BigDecimal grossYearlySalary,

        // Tax rates (percentages)
        BigDecimal municipalTaxRate,
        BigDecimal regionalTaxRate,
        BigDecimal stateTaxRate,
        BigDecimal burialFeeRate,
        BigDecimal churchFeeRate,

        // Deductions
        BigDecimal yearlyBasicDeduction,
        BigDecimal yearlyJobTaxCredit,

        // Calculated taxes (yearly)
        BigDecimal yearlyTaxableIncome,
        BigDecimal yearlyMunicipalTax,
        BigDecimal yearlyRegionalTax,
        BigDecimal yearlyStateTax,
        BigDecimal yearlyBurialFee,
        BigDecimal yearlyChurchFee,
        BigDecimal yearlyTotalTax,

        // Monthly amounts
        BigDecimal monthlyTotalTax,
        BigDecimal netMonthlySalary,

        // Summary
        BigDecimal effectiveTaxRate
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID municipalityId;
        private String municipalityName;
        private String regionName;
        private BigDecimal grossMonthlySalary;
        private BigDecimal grossYearlySalary;
        private BigDecimal municipalTaxRate;
        private BigDecimal regionalTaxRate;
        private BigDecimal stateTaxRate;
        private BigDecimal burialFeeRate;
        private BigDecimal churchFeeRate;
        private BigDecimal yearlyBasicDeduction;
        private BigDecimal yearlyJobTaxCredit;
        private BigDecimal yearlyTaxableIncome;
        private BigDecimal yearlyMunicipalTax;
        private BigDecimal yearlyRegionalTax;
        private BigDecimal yearlyStateTax;
        private BigDecimal yearlyBurialFee;
        private BigDecimal yearlyChurchFee;
        private BigDecimal yearlyTotalTax;
        private BigDecimal monthlyTotalTax;
        private BigDecimal netMonthlySalary;
        private BigDecimal effectiveTaxRate;

        public Builder municipalityId(UUID municipalityId) {
            this.municipalityId = municipalityId;
            return this;
        }

        public Builder municipalityName(String municipalityName) {
            this.municipalityName = municipalityName;
            return this;
        }

        public Builder regionName(String regionName) {
            this.regionName = regionName;
            return this;
        }

        public Builder grossMonthlySalary(BigDecimal grossMonthlySalary) {
            this.grossMonthlySalary = grossMonthlySalary;
            return this;
        }

        public Builder grossYearlySalary(BigDecimal grossYearlySalary) {
            this.grossYearlySalary = grossYearlySalary;
            return this;
        }

        public Builder municipalTaxRate(BigDecimal municipalTaxRate) {
            this.municipalTaxRate = municipalTaxRate;
            return this;
        }

        public Builder regionalTaxRate(BigDecimal regionalTaxRate) {
            this.regionalTaxRate = regionalTaxRate;
            return this;
        }

        public Builder stateTaxRate(BigDecimal stateTaxRate) {
            this.stateTaxRate = stateTaxRate;
            return this;
        }

        public Builder burialFeeRate(BigDecimal burialFeeRate) {
            this.burialFeeRate = burialFeeRate;
            return this;
        }

        public Builder churchFeeRate(BigDecimal churchFeeRate) {
            this.churchFeeRate = churchFeeRate;
            return this;
        }

        public Builder yearlyBasicDeduction(BigDecimal yearlyBasicDeduction) {
            this.yearlyBasicDeduction = yearlyBasicDeduction;
            return this;
        }

        public Builder yearlyJobTaxCredit(BigDecimal yearlyJobTaxCredit) {
            this.yearlyJobTaxCredit = yearlyJobTaxCredit;
            return this;
        }

        public Builder yearlyTaxableIncome(BigDecimal yearlyTaxableIncome) {
            this.yearlyTaxableIncome = yearlyTaxableIncome;
            return this;
        }

        public Builder yearlyMunicipalTax(BigDecimal yearlyMunicipalTax) {
            this.yearlyMunicipalTax = yearlyMunicipalTax;
            return this;
        }

        public Builder yearlyRegionalTax(BigDecimal yearlyRegionalTax) {
            this.yearlyRegionalTax = yearlyRegionalTax;
            return this;
        }

        public Builder yearlyStateTax(BigDecimal yearlyStateTax) {
            this.yearlyStateTax = yearlyStateTax;
            return this;
        }

        public Builder yearlyBurialFee(BigDecimal yearlyBurialFee) {
            this.yearlyBurialFee = yearlyBurialFee;
            return this;
        }

        public Builder yearlyChurchFee(BigDecimal yearlyChurchFee) {
            this.yearlyChurchFee = yearlyChurchFee;
            return this;
        }

        public Builder yearlyTotalTax(BigDecimal yearlyTotalTax) {
            this.yearlyTotalTax = yearlyTotalTax;
            return this;
        }

        public Builder monthlyTotalTax(BigDecimal monthlyTotalTax) {
            this.monthlyTotalTax = monthlyTotalTax;
            return this;
        }

        public Builder netMonthlySalary(BigDecimal netMonthlySalary) {
            this.netMonthlySalary = netMonthlySalary;
            return this;
        }

        public Builder effectiveTaxRate(BigDecimal effectiveTaxRate) {
            this.effectiveTaxRate = effectiveTaxRate;
            return this;
        }

        public TaxCalculationResponse build() {
            return new TaxCalculationResponse(
                    municipalityId, municipalityName, regionName,
                    grossMonthlySalary, grossYearlySalary,
                    municipalTaxRate, regionalTaxRate, stateTaxRate, burialFeeRate, churchFeeRate,
                    yearlyBasicDeduction, yearlyJobTaxCredit, yearlyTaxableIncome,
                    yearlyMunicipalTax, yearlyRegionalTax, yearlyStateTax, yearlyBurialFee, yearlyChurchFee,
                    yearlyTotalTax, monthlyTotalTax, netMonthlySalary, effectiveTaxRate
            );
        }
    }
}
