package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.example.demo.service.TaxConstants.*;

/**
 * Calculates the Swedish basic deduction (grundavdrag).
 * The basic deduction reduces taxable income and varies based on income level.
 */
@Component
public class BasicDeductionCalculator {

    /**
     * Calculate yearly basic deduction based on yearly gross income.
     * 
     * The basic deduction follows a complex curve:
     * - Very low income: Lower deduction
     * - Low-medium income: Maximum deduction (sweet spot)
     * - High income: Gradually decreasing deduction
     * - Very high income: Minimum deduction
     *
     * @param yearlyGrossIncome The yearly gross income
     * @return The yearly basic deduction amount
     */
    public BigDecimal calculate(BigDecimal yearlyGrossIncome) {
        BigDecimal pbb = PRICE_BASE_AMOUNT;
        BigDecimal incomeFactor = yearlyGrossIncome.divide(pbb, 10, RoundingMode.HALF_UP);

        BigDecimal deductionFactor;

        // Threshold values as factors of PBB
        BigDecimal t1 = BASIC_DEDUCTION_THRESHOLD_1;  // 0.99
        BigDecimal t2 = BASIC_DEDUCTION_THRESHOLD_2;  // 2.72
        BigDecimal t3 = BASIC_DEDUCTION_THRESHOLD_3;  // 3.11
        BigDecimal t4 = BASIC_DEDUCTION_THRESHOLD_4;  // 7.88

        if (incomeFactor.compareTo(t1) <= 0) {
            // Very low income: linear increase from 0.293 to 0.423
            // deduction = 0.293 + 0.131 × (income / 0.99 PBB)
            deductionFactor = new BigDecimal("0.293")
                    .add(new BigDecimal("0.131")
                            .multiply(incomeFactor.divide(t1, 10, RoundingMode.HALF_UP)));
        } else if (incomeFactor.compareTo(t2) <= 0) {
            // Low income: increase towards maximum
            // deduction = 0.423 + 0.347 × ((income - 0.99 PBB) / (2.72 - 0.99) PBB)
            BigDecimal range = t2.subtract(t1);
            BigDecimal position = incomeFactor.subtract(t1).divide(range, 10, RoundingMode.HALF_UP);
            deductionFactor = new BigDecimal("0.423")
                    .add(new BigDecimal("0.347").multiply(position));
        } else if (incomeFactor.compareTo(t3) <= 0) {
            // Sweet spot: maximum deduction
            deductionFactor = BASIC_DEDUCTION_MAX_FACTOR;
        } else if (incomeFactor.compareTo(t4) <= 0) {
            // Higher income: gradual decrease from max to min
            // Linear decrease from 0.770 to 0.293
            BigDecimal range = t4.subtract(t3);
            BigDecimal position = incomeFactor.subtract(t3).divide(range, 10, RoundingMode.HALF_UP);
            BigDecimal decrease = BASIC_DEDUCTION_MAX_FACTOR.subtract(BASIC_DEDUCTION_MIN_FACTOR);
            deductionFactor = BASIC_DEDUCTION_MAX_FACTOR.subtract(decrease.multiply(position));
        } else {
            // Very high income: minimum deduction
            deductionFactor = BASIC_DEDUCTION_MIN_FACTOR;
        }

        return pbb.multiply(deductionFactor).setScale(0, RoundingMode.HALF_UP);
    }
}
