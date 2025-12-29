package com.example.demo.service;

import com.example.demo.dto.TaxCalculationRequest;
import com.example.demo.dto.TaxCalculationResponse;
import com.example.demo.entity.Municipality;
import com.example.demo.entity.TaxCalculationLog;
import com.example.demo.repository.TaxCalculationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static com.example.demo.service.TaxConstants.*;

/**
 * Main service for Swedish salary tax calculations.
 * Handles all aspects of nettolön calculation including:
 * - Municipal and regional tax
 * - State tax
 * - Basic deduction (grundavdrag)
 * - Job tax credit (jobbskatteavdrag)
 * - Burial fee
 * - Church fee (optional)
 */
@Service
public class TaxCalculationService {

    private static final Logger log = LoggerFactory.getLogger(TaxCalculationService.class);
    private static final int SCALE = 2;

    private final TaxRateService taxRateService;
    private final BasicDeductionCalculator basicDeductionCalculator;
    private final JobTaxCreditCalculator jobTaxCreditCalculator;
    private final TaxCalculationLogRepository taxCalculationLogRepository;

    public TaxCalculationService(TaxRateService taxRateService,
                                  BasicDeductionCalculator basicDeductionCalculator,
                                  JobTaxCreditCalculator jobTaxCreditCalculator,
                                  TaxCalculationLogRepository taxCalculationLogRepository) {
        this.taxRateService = taxRateService;
        this.basicDeductionCalculator = basicDeductionCalculator;
        this.jobTaxCreditCalculator = jobTaxCreditCalculator;
        this.taxCalculationLogRepository = taxCalculationLogRepository;
    }

    /**
     * Calculate net salary from gross salary with full tax breakdown.
     */
    @Transactional
    public TaxCalculationResponse calculate(TaxCalculationRequest request) {
        log.info("Calculating tax for municipality {} with gross salary {}",
                request.municipalityId(), request.grossMonthlySalary());

        LocalDate today = LocalDate.now();

        // Get municipality info
        Municipality municipality = taxRateService.getMunicipality(request.municipalityId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Municipality not found: " + request.municipalityId()));

        // Get tax rates
        BigDecimal municipalTaxRate = taxRateService.getMunicipalTaxRate(request.municipalityId(), today);
        BigDecimal regionalTaxRate = taxRateService.getRegionalTaxRate(request.municipalityId(), today);
        BigDecimal burialFeeRate = taxRateService.getBurialFeeRate(request.municipalityId(), today);

        BigDecimal churchFeeRate = BigDecimal.ZERO;
        if (request.churchMember()) {
            churchFeeRate = taxRateService.getChurchFeeRate(request.municipalityId(), today);
        }

        // Calculate yearly values
        BigDecimal grossYearly = request.grossMonthlySalary()
                .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));

        // Basic deduction
        BigDecimal basicDeduction = basicDeductionCalculator.calculate(grossYearly);

        // Taxable income (can't be negative)
        BigDecimal taxableIncome = grossYearly.subtract(basicDeduction).max(BigDecimal.ZERO);

        // Combined local tax rate for job tax credit calculation
        BigDecimal totalLocalTaxRate = municipalTaxRate.add(regionalTaxRate);

        // Job tax credit
        BigDecimal jobTaxCredit = jobTaxCreditCalculator.calculate(
                grossYearly, totalLocalTaxRate, request.isPensioner());

        // Calculate individual taxes
        BigDecimal municipalTax = taxableIncome.multiply(municipalTaxRate)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal regionalTax = taxableIncome.multiply(regionalTaxRate)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal stateTax = calculateStateTax(grossYearly);

        // Burial and church fees are on gross income
        BigDecimal burialFee = grossYearly.multiply(burialFeeRate)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal churchFee = BigDecimal.ZERO;
        if (request.churchMember()) {
            churchFee = grossYearly.multiply(churchFeeRate)
                    .setScale(SCALE, RoundingMode.HALF_UP);
        }

        // Total yearly tax (before job tax credit)
        BigDecimal totalTaxBeforeCredit = municipalTax
                .add(regionalTax)
                .add(stateTax)
                .add(burialFee)
                .add(churchFee);

        // Apply job tax credit (reduces tax, can't make it negative)
        BigDecimal yearlyTotalTax = totalTaxBeforeCredit.subtract(jobTaxCredit).max(BigDecimal.ZERO);

        // Monthly values
        BigDecimal monthlyTotalTax = yearlyTotalTax
                .divide(BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP);

        BigDecimal netMonthlySalary = request.grossMonthlySalary().subtract(monthlyTotalTax);

        // Effective tax rate
        BigDecimal effectiveTaxRate = BigDecimal.ZERO;
        if (grossYearly.compareTo(BigDecimal.ZERO) > 0) {
            effectiveTaxRate = yearlyTotalTax.divide(grossYearly, 4, RoundingMode.HALF_UP);
        }

        // Log the calculation
        logCalculation(municipality, request.grossMonthlySalary(), yearlyTotalTax, netMonthlySalary);

        // Build response
        return TaxCalculationResponse.builder()
                .municipalityId(municipality.getId())
                .municipalityName(municipality.getName())
                .regionName(municipality.getRegion().getName())
                .grossMonthlySalary(request.grossMonthlySalary())
                .grossYearlySalary(grossYearly)
                .municipalTaxRate(municipalTaxRate)
                .regionalTaxRate(regionalTaxRate)
                .stateTaxRate(STATE_TAX_RATE)
                .burialFeeRate(burialFeeRate)
                .churchFeeRate(churchFeeRate)
                .yearlyBasicDeduction(basicDeduction)
                .yearlyJobTaxCredit(jobTaxCredit)
                .yearlyTaxableIncome(taxableIncome)
                .yearlyMunicipalTax(municipalTax)
                .yearlyRegionalTax(regionalTax)
                .yearlyStateTax(stateTax)
                .yearlyBurialFee(burialFee)
                .yearlyChurchFee(churchFee)
                .yearlyTotalTax(yearlyTotalTax)
                .monthlyTotalTax(monthlyTotalTax)
                .netMonthlySalary(netMonthlySalary)
                .effectiveTaxRate(effectiveTaxRate)
                .build();
    }

    /**
     * Calculate state tax (statlig skatt).
     * 20% on yearly income above the threshold.
     */
    private BigDecimal calculateStateTax(BigDecimal yearlyGrossIncome) {
        if (yearlyGrossIncome.compareTo(STATE_TAX_THRESHOLD) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal incomeAboveThreshold = yearlyGrossIncome.subtract(STATE_TAX_THRESHOLD);
        return incomeAboveThreshold.multiply(STATE_TAX_RATE)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Log the tax calculation for analytics and auditing.
     */
    private void logCalculation(Municipality municipality, BigDecimal grossSalary,
                                 BigDecimal totalTax, BigDecimal netSalary) {
        try {
            TaxCalculationLog logEntry = new TaxCalculationLog(
                    municipality,
                    grossSalary,
                    totalTax.divide(BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP),
                    netSalary
            );
            taxCalculationLogRepository.save(logEntry);
            log.debug("Logged tax calculation for municipality {}", municipality.getName());
        } catch (Exception e) {
            // Don't fail the calculation if logging fails
            log.error("Failed to log tax calculation", e);
        }
    }
}
