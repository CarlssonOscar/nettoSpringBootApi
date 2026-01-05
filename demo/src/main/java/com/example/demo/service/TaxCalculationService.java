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
 * Main service for Swedish salary tax calculations (2026).
 * 
 * Implementation follows SKV 433 - Teknisk beskrivning för skattetabeller 2026.
 * 
 * Kolumn 1 (arbetsinkomst för person under 66 år) beräknas enligt:
 * - Kommunal inkomstskatt (på beskattningsbar förvärvsinkomst)
 * - Statlig inkomstskatt (20% över 643 000 kr BFI)
 * - Allmän pensionsavgift (7% av bruttolön, max 47 100 kr)
 * - Begravningsavgift
 * - Kyrkoavgift (om medlem)
 * - Public service-avgift (1% av BFI, max 1 184 kr)
 * 
 * Minus skattereduktioner:
 * - Skattereduktion för allmän pensionsavgift (100%)
 * - Skattereduktion för arbetsinkomst (jobbskatteavdrag)
 * - Skattereduktion för förvärvsinkomst (0.75% av BFI-40000, max 1 500 kr)
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
     * Following SKV 433 exactly.
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

        // Get tax rates from database
        BigDecimal municipalTaxRate = taxRateService.getMunicipalTaxRate(request.municipalityId(), today);
        BigDecimal regionalTaxRate = taxRateService.getRegionalTaxRate(request.municipalityId(), today);
        BigDecimal burialFeeRate = taxRateService.getBurialFeeRate(request.municipalityId(), today);

        BigDecimal churchFeeRate = BigDecimal.ZERO;
        if (request.churchMember()) {
            churchFeeRate = taxRateService.getChurchFeeRate(request.municipalityId(), today);
        }

        // ========================================
        // 1. BERÄKNA ÅRSINKOMST (SKV 433 section 7.1)
        // ========================================
        BigDecimal grossYearly = request.grossMonthlySalary()
                .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));

        // ========================================
        // 2. GRUNDAVDRAG (SKV 433 section 6.1)
        // ========================================
        BigDecimal basicDeduction = basicDeductionCalculator.calculate(grossYearly, request.isPensioner());

        // ========================================
        // 3. BESKATTNINGSBAR FÖRVÄRVSINKOMST
        // ========================================
        BigDecimal taxableIncome = grossYearly.subtract(basicDeduction).max(BigDecimal.ZERO);

        // ========================================
        // 4. KOMMUNAL INKOMSTSKATT (SKV 433 section 7.3)
        // ========================================
        BigDecimal totalLocalTaxRate = municipalTaxRate.add(regionalTaxRate);
        BigDecimal municipalTax = taxableIncome.multiply(municipalTaxRate)
                .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal regionalTax = taxableIncome.multiply(regionalTaxRate)
                .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal totalCommunalTax = municipalTax.add(regionalTax);

        // ========================================
        // 5. STATLIG INKOMSTSKATT (SKV 433 section 7.2)
        // 20% på BFI över 643 000 kr
        // ========================================
        BigDecimal stateTax = calculateStateTax(taxableIncome);

        // ========================================
        // 6. ALLMÄN PENSIONSAVGIFT (SKV 433 section 7.4)
        // 7% av årsinkomst, max 47 100 kr
        // Ej för pensionärer eller inkomst under 25 042 kr
        // ========================================
        BigDecimal pensionContribution = BigDecimal.ZERO;
        if (!request.isPensioner() && grossYearly.compareTo(PENSION_CONTRIBUTION_MIN_INCOME) >= 0) {
            BigDecimal pensionBase = grossYearly.min(PENSION_CONTRIBUTION_MAX_INCOME);
            pensionContribution = pensionBase.multiply(PENSION_CONTRIBUTION_RATE);
            // Avrunda till närmaste hundratal (50 öre avrundas nedåt)
            pensionContribution = roundToHundredSpecial(pensionContribution);
            pensionContribution = pensionContribution.min(PENSION_CONTRIBUTION_MAX);
        }

        // ========================================
        // 7. SKATTEREDUKTION FÖR ALLMÄN PENSIONSAVGIFT (SKV 433 section 7.5.1)
        // 100% av avgiften, men får ej överstiga kommunal + statlig skatt
        // ========================================
        BigDecimal pensionReduction = pensionContribution.min(totalCommunalTax.add(stateTax));

        // ========================================
        // 8. JOBBSKATTEAVDRAG (SKV 433 section 7.5.2)
        // Endast mot kommunal inkomstskatt
        // ========================================
        BigDecimal jobTaxCredit = BigDecimal.ZERO;
        if (!request.isPensioner() || grossYearly.compareTo(BigDecimal.ZERO) > 0) {
            // For working income (kolumn 1 or 3)
            jobTaxCredit = jobTaxCreditCalculator.calculate(
                    grossYearly, totalLocalTaxRate, request.isPensioner());
        }

        // ========================================
        // 9. SKATTEREDUKTION FÖR FÖRVÄRVSINKOMST (SKV 433 section 7.5.4)
        // 0.75% av (BFI - 40 000), max 1 500 kr
        // ========================================
        BigDecimal incomeReduction = calculateIncomeReduction(taxableIncome);

        // ========================================
        // 10. PUBLIC SERVICE-AVGIFT (SKV 433 section 7.6)
        // 1% av BFI, max 1 184 kr
        // ========================================
        BigDecimal publicServiceFee = calculatePublicServiceFee(taxableIncome);

        // ========================================
        // 11. BEGRAVNINGSAVGIFT OCH KYRKOAVGIFT (SKV 433 section 7.3)
        // Beräknas på beskattningsbar förvärvsinkomst
        // ========================================
        BigDecimal burialFee = taxableIncome.multiply(burialFeeRate)
                .setScale(0, RoundingMode.DOWN);

        BigDecimal churchFee = BigDecimal.ZERO;
        if (request.churchMember()) {
            churchFee = taxableIncome.multiply(churchFeeRate)
                    .setScale(0, RoundingMode.DOWN);
        }

        // ========================================
        // 12. TOTAL SKATT (SKV 433 section 8)
        // Formel för kolumn 1:
        // statlig skatt + kommunal skatt 
        // - skattereduktion för pensionsavgift 
        // - jobbskatteavdrag 
        // - skattereduktion förvärvsinkomst
        // + begravnings- och kyrkoavgift 
        // + allmän pensionsavgift 
        // + public service-avgift
        // ========================================
        
        // Kommunal skatt efter reduktioner
        // Jobbskatteavdrag och inkomstreduktion räknas bara av mot kommunal skatt
        BigDecimal communalTaxAfterReductions = totalCommunalTax
                .subtract(jobTaxCredit)
                .subtract(incomeReduction)
                .max(BigDecimal.ZERO);
        
        // Statlig skatt efter pensionsavgiftsreduktion
        // Pensionsavgiftsreduktion dras först mot kommunal, sedan statlig
        BigDecimal pensionReductionForCommunal = pensionReduction.min(totalCommunalTax);
        BigDecimal pensionReductionForState = pensionReduction.subtract(pensionReductionForCommunal).max(BigDecimal.ZERO);
        
        BigDecimal communalTaxFinal = communalTaxAfterReductions.subtract(pensionReductionForCommunal).max(BigDecimal.ZERO);
        BigDecimal stateTaxFinal = stateTax.subtract(pensionReductionForState).max(BigDecimal.ZERO);

        // Total årlig skatt
        BigDecimal yearlyTotalTax = communalTaxFinal
                .add(stateTaxFinal)
                .add(burialFee)
                .add(churchFee)
                .add(pensionContribution)
                .add(publicServiceFee);

        // ========================================
        // 13. MÅNATLIG SKATT OCH NETTOLÖN
        // ========================================
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
     * Calculate state tax (statlig skatt) - SKV 433 section 7.2.
     * 20% on beskattningsbar förvärvsinkomst above 643 000 kr.
     */
    private BigDecimal calculateStateTax(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(STATE_TAX_THRESHOLD) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal incomeAboveThreshold = taxableIncome.subtract(STATE_TAX_THRESHOLD);
        return incomeAboveThreshold.multiply(STATE_TAX_RATE)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate income reduction (skattereduktion för förvärvsinkomst) - SKV 433 section 7.5.4.
     * 0.75% of (BFI - 40 000), max 1 500 kr.
     */
    private BigDecimal calculateIncomeReduction(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(INCOME_REDUCTION_THRESHOLD) <= 0) {
            return BigDecimal.ZERO;
        }

        if (taxableIncome.compareTo(INCOME_REDUCTION_MAX_THRESHOLD) >= 0) {
            return INCOME_REDUCTION_MAX;
        }

        BigDecimal base = taxableIncome.subtract(INCOME_REDUCTION_THRESHOLD);
        return base.multiply(INCOME_REDUCTION_RATE).setScale(0, RoundingMode.DOWN);
    }

    /**
     * Calculate public service fee - SKV 433 section 7.6.
     * 1% of BFI, max 1 184 kr.
     */
    private BigDecimal calculatePublicServiceFee(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (taxableIncome.compareTo(PUBLIC_SERVICE_THRESHOLD) >= 0) {
            return PUBLIC_SERVICE_MAX;
        }

        return taxableIncome.multiply(PUBLIC_SERVICE_RATE).setScale(0, RoundingMode.DOWN);
    }

    /**
     * Round to nearest hundred, with special rule: 50 kr rounds down.
     * From SKV 433 section 7.4: "Avgift som slutar på 50 kr avrundas till närmaste lägre hundratal kronor."
     */
    private BigDecimal roundToHundredSpecial(BigDecimal value) {
        BigDecimal remainder = value.remainder(new BigDecimal("100"));
        BigDecimal base = value.subtract(remainder);
        
        if (remainder.compareTo(new BigDecimal("50")) > 0) {
            return base.add(new BigDecimal("100"));
        }
        return base;
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

    /**
     * Calculate net salary using municipality code instead of UUID.
     */
    public TaxCalculationResponse calculateByMunicipalityCode(
            String municipalityCode, BigDecimal grossMonthlySalary, 
            boolean churchMember, boolean isPensioner) {
        
        Municipality municipality = taxRateService.getMunicipalityByCode(municipalityCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Municipality not found with code: " + municipalityCode));

        TaxCalculationRequest request = new TaxCalculationRequest(
                municipality.getId(),
                grossMonthlySalary,
                churchMember,
                isPensioner
        );

        return calculate(request);
    }
}
