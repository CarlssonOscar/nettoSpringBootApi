package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static com.example.demo.service.TaxConstants.*;

/**
 * Calculates the Swedish job tax credit (jobbskatteavdrag).
 * The job tax credit reduces the actual tax (not taxable income).
 */
@Component
public class JobTaxCreditCalculator {

    /**
     * Calculate yearly job tax credit.
     * 
     * The job tax credit depends on:
     * - Income level
     * - Municipal + regional tax rate
     * - Age (seniors get enhanced credit)
     *
     * @param yearlyGrossIncome The yearly gross income
     * @param totalLocalTaxRate Municipal + regional tax rate (as decimal, e.g., 0.32)
     * @param birthYear The person's birth year
     * @return The yearly job tax credit amount
     */
    public BigDecimal calculate(BigDecimal yearlyGrossIncome, BigDecimal totalLocalTaxRate, int birthYear) {
        int age = LocalDate.now().getYear() - birthYear;
        boolean isSenior = age >= SENIOR_AGE_THRESHOLD;

        if (isSenior) {
            return calculateSeniorCredit(yearlyGrossIncome, totalLocalTaxRate);
        } else {
            return calculateStandardCredit(yearlyGrossIncome, totalLocalTaxRate);
        }
    }

    /**
     * Standard job tax credit for persons under 65.
     */
    private BigDecimal calculateStandardCredit(BigDecimal yearlyIncome, BigDecimal taxRate) {
        BigDecimal pbb = JOB_TAX_CREDIT_BASE;

        // Income thresholds as factors of PBB
        BigDecimal threshold1 = pbb.multiply(new BigDecimal("0.91"));
        BigDecimal threshold2 = pbb.multiply(new BigDecimal("3.24"));
        BigDecimal threshold3 = pbb.multiply(new BigDecimal("8.08"));
        BigDecimal threshold4 = pbb.multiply(new BigDecimal("13.54"));

        BigDecimal credit;

        if (yearlyIncome.compareTo(threshold1) <= 0) {
            // Low income: credit = (income - grundavdrag) × taxRate
            // Simplified: credit = income × taxRate × factor
            credit = yearlyIncome.multiply(taxRate).multiply(new BigDecimal("0.91"));
        } else if (yearlyIncome.compareTo(threshold2) <= 0) {
            // Medium-low income
            BigDecimal base = threshold1.multiply(taxRate).multiply(new BigDecimal("0.91"));
            BigDecimal additional = yearlyIncome.subtract(threshold1)
                    .multiply(taxRate)
                    .multiply(new BigDecimal("0.332"));
            credit = base.add(additional);
        } else if (yearlyIncome.compareTo(threshold3) <= 0) {
            // Medium income: approaching maximum credit
            BigDecimal maxCredit = pbb.multiply(JOB_TAX_CREDIT_MAX_FACTOR)
                    .multiply(taxRate)
                    .multiply(new BigDecimal("12"));
            credit = maxCredit;
        } else if (yearlyIncome.compareTo(threshold4) <= 0) {
            // Higher income: credit starts to decrease
            BigDecimal maxCredit = pbb.multiply(JOB_TAX_CREDIT_MAX_FACTOR)
                    .multiply(taxRate)
                    .multiply(new BigDecimal("12"));
            BigDecimal reduction = yearlyIncome.subtract(threshold3)
                    .multiply(new BigDecimal("0.03"));
            credit = maxCredit.subtract(reduction).max(BigDecimal.ZERO);
        } else {
            // Very high income: credit phases out
            BigDecimal maxCredit = pbb.multiply(JOB_TAX_CREDIT_MAX_FACTOR)
                    .multiply(taxRate)
                    .multiply(new BigDecimal("12"));
            BigDecimal fullReduction = threshold4.subtract(threshold3)
                    .multiply(new BigDecimal("0.03"));
            BigDecimal additionalReduction = yearlyIncome.subtract(threshold4)
                    .multiply(new BigDecimal("0.03"));
            credit = maxCredit.subtract(fullReduction).subtract(additionalReduction).max(BigDecimal.ZERO);
        }

        return credit.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Enhanced job tax credit for seniors (65+).
     * Seniors get a more generous credit.
     */
    private BigDecimal calculateSeniorCredit(BigDecimal yearlyIncome, BigDecimal taxRate) {
        BigDecimal pbb = JOB_TAX_CREDIT_BASE;

        // Seniors get enhanced credit up to a higher income level
        BigDecimal threshold1 = pbb.multiply(new BigDecimal("1.00"));
        BigDecimal threshold2 = pbb.multiply(new BigDecimal("3.00"));
        BigDecimal threshold3 = pbb.multiply(new BigDecimal("9.00"));

        BigDecimal credit;

        if (yearlyIncome.compareTo(threshold1) <= 0) {
            // credit = income × taxRate
            credit = yearlyIncome.multiply(taxRate);
        } else if (yearlyIncome.compareTo(threshold2) <= 0) {
            // credit increases at reduced rate
            BigDecimal base = threshold1.multiply(taxRate);
            BigDecimal additional = yearlyIncome.subtract(threshold1)
                    .multiply(taxRate)
                    .multiply(SENIOR_JOB_TAX_CREDIT_FACTOR);
            credit = base.add(additional);
        } else if (yearlyIncome.compareTo(threshold3) <= 0) {
            // Maximum senior credit
            BigDecimal base = threshold1.multiply(taxRate);
            BigDecimal mid = threshold2.subtract(threshold1)
                    .multiply(taxRate)
                    .multiply(SENIOR_JOB_TAX_CREDIT_FACTOR);
            credit = base.add(mid);
        } else {
            // Credit starts to decrease for very high incomes
            BigDecimal base = threshold1.multiply(taxRate);
            BigDecimal mid = threshold2.subtract(threshold1)
                    .multiply(taxRate)
                    .multiply(SENIOR_JOB_TAX_CREDIT_FACTOR);
            BigDecimal maxCredit = base.add(mid);
            BigDecimal reduction = yearlyIncome.subtract(threshold3)
                    .multiply(new BigDecimal("0.03"));
            credit = maxCredit.subtract(reduction).max(BigDecimal.ZERO);
        }

        return credit.setScale(0, RoundingMode.HALF_UP);
    }
}
