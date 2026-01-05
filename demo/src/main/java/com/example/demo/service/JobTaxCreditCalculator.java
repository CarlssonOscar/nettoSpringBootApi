package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.example.demo.service.TaxConstants.*;

/**
 * Calculates the Swedish job tax credit (jobbskatteavdrag) for 2026.
 * 
 * Implementation follows SKV 433 section 7.5.2 exactly.
 * 
 * The job tax credit is a reduction of the final tax.
 * It depends on income level, grundavdrag, and the municipal tax rate.
 * 
 * From SKV 433:
 * "Skattereduktionen för arbetsinkomst ska endast räknas av mot kommunal inkomstskatt."
 * 
 * The formula calculates a "base amount" that is then multiplied by the 
 * municipal tax rate (excluding 1.16% burial/church fee).
 */
@Component
public class JobTaxCreditCalculator {

    private final BasicDeductionCalculator basicDeductionCalculator;

    public JobTaxCreditCalculator(BasicDeductionCalculator basicDeductionCalculator) {
        this.basicDeductionCalculator = basicDeductionCalculator;
    }

    /**
     * Calculate yearly job tax credit.
     * 
     * From SKV 433 section 7.5.2:
     * The jobbskatteavdrag is calculated as:
     *   (underlag - grundavdrag) × kommunalskattesats
     * 
     * Where the "underlag" varies by income level.
     *
     * @param yearlyGrossIncome The yearly gross income (arbetsinkomst)
     * @param totalLocalTaxRate Municipal + regional tax rate (as decimal, e.g., 0.3284)
     * @param isPensioner Whether the person is 66+ years old at year start
     * @return The yearly job tax credit amount
     */
    public BigDecimal calculate(BigDecimal yearlyGrossIncome, BigDecimal totalLocalTaxRate, boolean isPensioner) {
        if (yearlyGrossIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // KI = kommunal inkomstskatt exkl. begravnings- och trossamfundsavgift (1.16%)
        // From SKV 433: "multiplicerat med skattesatsen för kommunal inkomstskatt 
        // (exkl. 1,16 procentenheter för begravningsavgift och avgift till trossamfund)"
        BigDecimal ki = totalLocalTaxRate.subtract(BURIAL_AND_CHURCH_RATE_IN_TABLE);
        
        // Ensure KI is not negative
        if (ki.compareTo(BigDecimal.ZERO) <= 0) {
            ki = totalLocalTaxRate;
        }

        if (isPensioner) {
            return calculateSeniorCredit(yearlyGrossIncome, ki);
        } else {
            return calculateStandardCredit(yearlyGrossIncome, ki);
        }
    }

    /**
     * Standard job tax credit for persons under 66 years.
     * 
     * From SKV 433 section 7.5.2:
     * 
     * AI ≤ 0.91 PBB (53 872):
     *   (AI - GA) × KI
     * 
     * AI ≤ 3.24 PBB (191 808):
     *   (0.91 PBB + 38.74% × (AI - 0.91 PBB) - GA) × KI
     * 
     * AI ≤ 8.08 PBB (478 336):
     *   (1.813 PBB + 25.10% × (AI - 3.24 PBB) - GA) × KI
     * 
     * AI > 8.08 PBB:
     *   (3.027 PBB - GA) × KI
     */
    private BigDecimal calculateStandardCredit(BigDecimal ai, BigDecimal ki) {
        // AI = arbetsinkomst, avrundad nedåt till helt hundratal
        ai = roundDownToHundred(ai);
        
        // GA = grundavdrag (for non-senior)
        BigDecimal ga = basicDeductionCalculator.calculate(ai, false);
        
        BigDecimal underlag;
        
        if (ai.compareTo(JSA_THRESHOLD_1) <= 0) {
            // AI ≤ 53 872: underlag = AI
            underlag = ai;
        } else if (ai.compareTo(JSA_THRESHOLD_2) <= 0) {
            // AI ≤ 191 808: underlag = 0.91 PBB + 38.74% × (AI - 0.91 PBB)
            BigDecimal excess = ai.subtract(JSA_THRESHOLD_1);
            BigDecimal increase = excess.multiply(new BigDecimal("0.3874"));
            underlag = JSA_THRESHOLD_1.add(increase);
        } else if (ai.compareTo(JSA_THRESHOLD_3) <= 0) {
            // AI ≤ 478 336: underlag = 1.813 PBB + 25.10% × (AI - 3.24 PBB)
            BigDecimal excess = ai.subtract(JSA_THRESHOLD_2);
            BigDecimal increase = excess.multiply(new BigDecimal("0.251"));
            underlag = JSA_FACTOR_1813_PBB.add(increase);
        } else {
            // AI > 478 336: underlag = 3.027 PBB
            underlag = JSA_FACTOR_3027_PBB;
        }
        
        // Jobbskatteavdrag = (underlag - GA) × KI
        BigDecimal base = underlag.subtract(ga);
        
        // Can't be negative
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal credit = base.multiply(ki);
        
        // Avrundas nedåt till hel krona
        return credit.setScale(0, RoundingMode.DOWN);
    }

    /**
     * Job tax credit for seniors (66+ years).
     * 
     * From SKV 433 section 7.5.2:
     * 
     * AI ≤ 1.75 PBB (103 600):
     *   AI × 22%
     * 
     * AI ≤ 5.24 PBB (310 208):
     *   0.2635 PBB + 7% × AI
     * 
     * AI > 5.24 PBB:
     *   0.6293 PBB
     */
    private BigDecimal calculateSeniorCredit(BigDecimal ai, BigDecimal ki) {
        // For seniors, the calculation is different - not based on municipal tax rate
        // but a fixed percentage
        
        BigDecimal credit;
        
        if (ai.compareTo(JSA_SENIOR_THRESHOLD_1) <= 0) {
            // AI ≤ 103 600: credit = AI × 22%
            credit = ai.multiply(new BigDecimal("0.22"));
        } else if (ai.compareTo(JSA_SENIOR_THRESHOLD_2) <= 0) {
            // AI ≤ 310 208: credit = 0.2635 PBB + 7% × AI
            credit = JSA_SENIOR_FACTOR_02635_PBB.add(ai.multiply(new BigDecimal("0.07")));
        } else {
            // AI > 310 208: credit = 0.6293 PBB
            credit = JSA_SENIOR_FACTOR_06293_PBB;
        }
        
        return credit.setScale(0, RoundingMode.DOWN);
    }

    /**
     * Round down to nearest hundred kronor.
     * From SKV 433: "arbetsinkomsten avrundas nedåt till helt hundratal kronor"
     */
    private BigDecimal roundDownToHundred(BigDecimal value) {
        return value.divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
                .multiply(new BigDecimal("100"));
    }
}
