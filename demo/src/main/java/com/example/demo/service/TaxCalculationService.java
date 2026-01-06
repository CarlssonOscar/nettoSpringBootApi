package com.example.demo.service;

import com.example.demo.dto.TaxCalculationRequest;
import com.example.demo.dto.TaxCalculationResponse;
import com.example.demo.entity.Municipality;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static com.example.demo.service.TaxConstants.*;

/**
 * Main service for Swedish salary tax calculations (2026).
 * 
 * Implementation follows SKV 433 - Teknisk beskrivning för skattetabeller 2026.
 */
@Service
@RequiredArgsConstructor
public class TaxCalculationService {

    private static final Logger log = LoggerFactory.getLogger(TaxCalculationService.class);
    private static final int SCALE = 2;

    private final TaxRateService taxRateService;
    private final BasicDeductionCalculator basicDeductionCalculator;
    private final JobTaxCreditCalculator jobTaxCreditCalculator;
    private final TaxCalculationLogService taxCalculationLogService;

    /**
     * Internal record to hold tax rates for a calculation.
     */
    private record TaxRates(
            BigDecimal municipal,
            BigDecimal regional,
            BigDecimal burial,
            BigDecimal church
    ) {
        BigDecimal totalLocal() {
            return municipal.add(regional);
        }
    }

    /**
     * Internal record to hold intermediate calculation results.
     */
    private record TaxComponents(
            BigDecimal municipalTax,
            BigDecimal regionalTax,
            BigDecimal stateTax,
            BigDecimal pensionContribution,
            BigDecimal jobTaxCredit,
            BigDecimal incomeReduction,
            BigDecimal publicServiceFee,
            BigDecimal burialFee,
            BigDecimal churchFee
    ) {
        BigDecimal totalCommunalTax() {
            return municipalTax.add(regionalTax);
        }
    }

    /**
     * Calculate net salary from gross salary with full tax breakdown.
     * Using readOnly=true for better performance since this only reads data.
     */
    @Transactional(readOnly = true)
    public TaxCalculationResponse calculate(TaxCalculationRequest request) {
        long startTime = System.nanoTime();
        log.info("Calculating tax for municipality {} with gross salary {}",
                request.municipalityId(), request.grossMonthlySalary());

        Municipality municipality = getMunicipalityOrThrow(request.municipalityId());
        TaxRates rates = fetchTaxRates(request.municipalityId(), request.churchMember());
        
        BigDecimal grossYearly = request.grossMonthlySalary().multiply(BigDecimal.valueOf(MONTHS_PER_YEAR));
        BigDecimal basicDeduction = basicDeductionCalculator.calculate(grossYearly, request.isPensioner());
        BigDecimal taxableIncome = grossYearly.subtract(basicDeduction).max(BigDecimal.ZERO);

        TaxComponents components = calculateTaxComponents(request, rates, grossYearly, taxableIncome);
        BigDecimal yearlyTotalTax = calculateYearlyTotalTax(components);
        BigDecimal monthlyTotalTax = yearlyTotalTax.divide(BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP);
        BigDecimal netMonthlySalary = request.grossMonthlySalary().subtract(monthlyTotalTax);

        taxCalculationLogService.logCalculationAsync(
                municipality.getId(), request.grossMonthlySalary(), yearlyTotalTax, netMonthlySalary);

        logPerformance(startTime, municipality.getName());

        return buildResponse(request, municipality, rates, grossYearly, basicDeduction, 
                taxableIncome, components, yearlyTotalTax, monthlyTotalTax, netMonthlySalary);
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
                municipality.getId(), grossMonthlySalary, churchMember, isPensioner);

        return calculate(request);
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    private Municipality getMunicipalityOrThrow(UUID municipalityId) {
        return taxRateService.getMunicipality(municipalityId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Municipality not found: " + municipalityId));
    }

    private TaxRates fetchTaxRates(UUID municipalityId, boolean churchMember) {
        Map<String, BigDecimal> allRates = taxRateService.getAllTaxRates(municipalityId, LocalDate.now());
        
        return new TaxRates(
                allRates.getOrDefault("COMMUNAL", DEFAULT_MUNICIPAL_TAX_RATE),
                allRates.getOrDefault("REGIONAL", DEFAULT_REGIONAL_TAX_RATE),
                allRates.getOrDefault("BURIAL", DEFAULT_BURIAL_FEE_RATE),
                churchMember ? allRates.getOrDefault("CHURCH", DEFAULT_CHURCH_FEE_RATE) : BigDecimal.ZERO
        );
    }

    private TaxComponents calculateTaxComponents(TaxCalculationRequest request, TaxRates rates,
                                                   BigDecimal grossYearly, BigDecimal taxableIncome) {
        BigDecimal municipalTax = taxableIncome.multiply(rates.municipal()).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal regionalTax = taxableIncome.multiply(rates.regional()).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal stateTax = calculateStateTax(taxableIncome);
        BigDecimal pensionContribution = calculatePensionContribution(grossYearly, request.isPensioner());
        BigDecimal jobTaxCredit = jobTaxCreditCalculator.calculate(grossYearly, rates.totalLocal(), request.isPensioner());
        BigDecimal incomeReduction = calculateIncomeReduction(taxableIncome);
        BigDecimal publicServiceFee = calculatePublicServiceFee(taxableIncome);
        BigDecimal burialFee = taxableIncome.multiply(rates.burial()).setScale(0, RoundingMode.DOWN);
        BigDecimal churchFee = request.churchMember() 
                ? taxableIncome.multiply(rates.church()).setScale(0, RoundingMode.DOWN) 
                : BigDecimal.ZERO;

        return new TaxComponents(municipalTax, regionalTax, stateTax, pensionContribution,
                jobTaxCredit, incomeReduction, publicServiceFee, burialFee, churchFee);
    }

    private BigDecimal calculateYearlyTotalTax(TaxComponents c) {
        BigDecimal pensionReduction = c.pensionContribution().min(c.totalCommunalTax().add(c.stateTax()));
        BigDecimal pensionReductionForCommunal = pensionReduction.min(c.totalCommunalTax());
        BigDecimal pensionReductionForState = pensionReduction.subtract(pensionReductionForCommunal).max(BigDecimal.ZERO);
        
        BigDecimal communalTaxFinal = c.totalCommunalTax()
                .subtract(c.jobTaxCredit())
                .subtract(c.incomeReduction())
                .subtract(pensionReductionForCommunal)
                .max(BigDecimal.ZERO);
        
        BigDecimal stateTaxFinal = c.stateTax().subtract(pensionReductionForState).max(BigDecimal.ZERO);

        return communalTaxFinal
                .add(stateTaxFinal)
                .add(c.burialFee())
                .add(c.churchFee())
                .add(c.pensionContribution())
                .add(c.publicServiceFee());
    }

    private BigDecimal calculatePensionContribution(BigDecimal grossYearly, boolean isPensioner) {
        if (isPensioner || grossYearly.compareTo(PENSION_CONTRIBUTION_MIN_INCOME) < 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal pensionBase = grossYearly.min(PENSION_CONTRIBUTION_MAX_INCOME);
        BigDecimal contribution = pensionBase.multiply(PENSION_CONTRIBUTION_RATE);
        contribution = roundToHundredSpecial(contribution);
        return contribution.min(PENSION_CONTRIBUTION_MAX);
    }

    private BigDecimal calculateStateTax(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(STATE_TAX_THRESHOLD) <= 0) {
            return BigDecimal.ZERO;
        }
        return taxableIncome.subtract(STATE_TAX_THRESHOLD)
                .multiply(STATE_TAX_RATE)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIncomeReduction(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(INCOME_REDUCTION_THRESHOLD) <= 0) {
            return BigDecimal.ZERO;
        }
        if (taxableIncome.compareTo(INCOME_REDUCTION_MAX_THRESHOLD) >= 0) {
            return INCOME_REDUCTION_MAX;
        }
        return taxableIncome.subtract(INCOME_REDUCTION_THRESHOLD)
                .multiply(INCOME_REDUCTION_RATE)
                .setScale(0, RoundingMode.DOWN);
    }

    private BigDecimal calculatePublicServiceFee(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (taxableIncome.compareTo(PUBLIC_SERVICE_THRESHOLD) >= 0) {
            return PUBLIC_SERVICE_MAX;
        }
        return taxableIncome.multiply(PUBLIC_SERVICE_RATE).setScale(0, RoundingMode.DOWN);
    }

    private BigDecimal roundToHundredSpecial(BigDecimal value) {
        BigDecimal remainder = value.remainder(new BigDecimal("100"));
        BigDecimal base = value.subtract(remainder);
        return remainder.compareTo(new BigDecimal("50")) > 0 ? base.add(new BigDecimal("100")) : base;
    }

    private void logPerformance(long startTime, String municipalityName) {
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        if (log.isDebugEnabled()) {
            log.debug("Tax calculation completed in {} ms for municipality {}", durationMs, municipalityName);
        }
        if (durationMs > 500) {
            log.warn("Slow tax calculation: {} ms for municipality {}", durationMs, municipalityName);
        }
    }

    private TaxCalculationResponse buildResponse(TaxCalculationRequest request, Municipality municipality,
                                                   TaxRates rates, BigDecimal grossYearly, BigDecimal basicDeduction,
                                                   BigDecimal taxableIncome, TaxComponents c,
                                                   BigDecimal yearlyTotalTax, BigDecimal monthlyTotalTax, 
                                                   BigDecimal netMonthlySalary) {
        BigDecimal effectiveTaxRate = grossYearly.compareTo(BigDecimal.ZERO) > 0
                ? yearlyTotalTax.divide(grossYearly, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return TaxCalculationResponse.builder()
                .municipalityId(municipality.getId())
                .municipalityName(municipality.getName())
                .regionName(municipality.getRegion().getName())
                .grossMonthlySalary(request.grossMonthlySalary())
                .grossYearlySalary(grossYearly)
                .municipalTaxRate(rates.municipal())
                .regionalTaxRate(rates.regional())
                .stateTaxRate(STATE_TAX_RATE)
                .burialFeeRate(rates.burial())
                .churchFeeRate(rates.church())
                .yearlyBasicDeduction(basicDeduction)
                .yearlyJobTaxCredit(c.jobTaxCredit())
                .yearlyTaxableIncome(taxableIncome)
                .yearlyMunicipalTax(c.municipalTax())
                .yearlyRegionalTax(c.regionalTax())
                .yearlyStateTax(c.stateTax())
                .yearlyBurialFee(c.burialFee())
                .yearlyChurchFee(c.churchFee())
                .yearlyTotalTax(yearlyTotalTax)
                .monthlyTotalTax(monthlyTotalTax)
                .netMonthlySalary(netMonthlySalary)
                .effectiveTaxRate(effectiveTaxRate)
                .build();
    }
}
