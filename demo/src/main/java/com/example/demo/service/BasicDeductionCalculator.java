package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.example.demo.service.TaxConstants.*;

/**
 * Calculates the Swedish basic deduction (grundavdrag) for 2026.
 * 
 * Implementation follows SKV 433 section 6.1 exactly.
 * The basic deduction reduces taxable income and varies based on income level.
 */
@Component
public class BasicDeductionCalculator {

    /**
     * Calculate yearly basic deduction based on yearly gross income.
     * 
     * From SKV 433 section 6.1:
     * Grundavdraget baseras på prisbasbeloppet (59 200 kr för 2026).
     * Grundavdraget avrundas uppåt till jämnt hundratal kronor.
     *
     * @param yearlyGrossIncome The yearly gross income (fastställd förvärvsinkomst)
     * @return The yearly basic deduction amount, rounded up to nearest 100 kr
     */
    public BigDecimal calculate(BigDecimal yearlyGrossIncome) {
        return calculate(yearlyGrossIncome, false);
    }

    /**
     * Calculate yearly basic deduction with age consideration.
     *
     * @param yearlyGrossIncome The yearly gross income (fastställd förvärvsinkomst)
     * @param isSenior Whether the person is 66+ years old at year start
     * @return The yearly basic deduction amount
     */
    public BigDecimal calculate(BigDecimal yearlyGrossIncome, boolean isSenior) {
        BigDecimal standardDeduction = calculateStandardDeduction(yearlyGrossIncome);
        
        if (isSenior) {
            BigDecimal enhancedPart = calculateEnhancedDeductionForSeniors(yearlyGrossIncome);
            BigDecimal total = standardDeduction.add(enhancedPart);
            return roundUpToHundred(total);
        }
        
        return roundUpToHundred(standardDeduction);
    }

    /**
     * Standard basic deduction calculation.
     * 
     * From SKV 433 section 6.1:
     * 
     * FFI ≤ 0.99 PBB (58 608):     0.423 PBB = 25 041.60
     * FFI ≤ 2.72 PBB (161 024):    0.423 PBB + 20% × (FFI - 0.99 PBB)
     * FFI ≤ 3.11 PBB (184 112):    0.77 PBB = 45 584
     * FFI ≤ 7.88 PBB (466 496):    0.77 PBB - 10% × (FFI - 3.11 PBB)
     * FFI > 7.88 PBB:              0.293 PBB = 17 345.60
     * 
     * Note: Returns unrounded value for combining with senior enhanced deduction.
     */
    private BigDecimal calculateStandardDeduction(BigDecimal ffi) {
        if (ffi.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal deduction;

        if (ffi.compareTo(GA_THRESHOLD_1) <= 0) {
            // FFI ≤ 58 608: deduction = 0.423 PBB = 25 041.60
            deduction = GA_FACTOR_0423_PBB;
        } else if (ffi.compareTo(GA_THRESHOLD_2) <= 0) {
            // FFI ≤ 161 024: deduction = 0.423 PBB + 20% × (FFI - 0.99 PBB)
            BigDecimal excess = ffi.subtract(GA_THRESHOLD_1);
            BigDecimal increase = excess.multiply(new BigDecimal("0.20"));
            deduction = GA_FACTOR_0423_PBB.add(increase);
        } else if (ffi.compareTo(GA_THRESHOLD_3) <= 0) {
            // FFI ≤ 184 112: deduction = 0.77 PBB = 45 584
            deduction = GA_FACTOR_077_PBB;
        } else if (ffi.compareTo(GA_THRESHOLD_4) <= 0) {
            // FFI ≤ 466 496: deduction = 0.77 PBB - 10% × (FFI - 3.11 PBB)
            BigDecimal excess = ffi.subtract(GA_THRESHOLD_3);
            BigDecimal decrease = excess.multiply(new BigDecimal("0.10"));
            deduction = GA_FACTOR_077_PBB.subtract(decrease);
        } else {
            // FFI > 466 496: deduction = 0.293 PBB = 17 345.60
            deduction = GA_FACTOR_0293_PBB;
        }

        // Grundavdraget får inte överstiga den fastställda förvärvsinkomsten
        return deduction.min(ffi);
    }

    /**
     * Enhanced (förhöjt) grundavdrag for seniors 66+ years.
     * From SKV 433 section 6.2.
     * 
     * This is added to the standard deduction for seniors.
     */
    private BigDecimal calculateEnhancedDeductionForSeniors(BigDecimal ffi) {
        if (ffi.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal pbb = PRICE_BASE_AMOUNT;
        
        // Thresholds for senior enhanced deduction
        BigDecimal t1 = pbb.multiply(new BigDecimal("0.91"));   // 53 872
        BigDecimal t2 = pbb.multiply(new BigDecimal("1.11"));   // 65 712
        BigDecimal t3 = pbb.multiply(new BigDecimal("1.965"));  // 116 328
        BigDecimal t4 = pbb.multiply(new BigDecimal("2.72"));   // 161 024
        BigDecimal t5 = pbb.multiply(new BigDecimal("3.11"));   // 184 112
        BigDecimal t6 = pbb.multiply(new BigDecimal("3.24"));   // 191 808
        BigDecimal t7 = pbb.multiply(new BigDecimal("5.00"));   // 296 000
        BigDecimal t8 = pbb.multiply(new BigDecimal("7.88"));   // 466 496
        BigDecimal t9 = pbb.multiply(new BigDecimal("8.08"));   // 478 336
        BigDecimal t10 = pbb.multiply(new BigDecimal("11.16")); // 660 672
        BigDecimal t11 = pbb.multiply(new BigDecimal("12.84")); // 760 128

        if (ffi.compareTo(t1) <= 0) {
            // 0.687 PBB
            return pbb.multiply(new BigDecimal("0.687"));
        } else if (ffi.compareTo(t2) <= 0) {
            // 0.885 PBB - 20% × FFI
            return pbb.multiply(new BigDecimal("0.885")).subtract(ffi.multiply(new BigDecimal("0.20")));
        } else if (ffi.compareTo(t3) <= 0) {
            // 0.600 PBB + 5.7% × FFI
            return pbb.multiply(new BigDecimal("0.600")).add(ffi.multiply(new BigDecimal("0.057")));
        } else if (ffi.compareTo(t4) <= 0) {
            // 0.333 PBB + 19.49% × FFI
            return pbb.multiply(new BigDecimal("0.333")).add(ffi.multiply(new BigDecimal("0.1949")));
        } else if (ffi.compareTo(t5) <= 0) {
            // 39.49% × FFI - 0.212 PBB
            return ffi.multiply(new BigDecimal("0.3949")).subtract(pbb.multiply(new BigDecimal("0.212")));
        } else if (ffi.compareTo(t6) <= 0) {
            // 49.49% × FFI - 0.523 PBB
            return ffi.multiply(new BigDecimal("0.4949")).subtract(pbb.multiply(new BigDecimal("0.523")));
        } else if (ffi.compareTo(t7) <= 0) {
            // 35.6% × FFI - 0.073 PBB
            return ffi.multiply(new BigDecimal("0.356")).subtract(pbb.multiply(new BigDecimal("0.073")));
        } else if (ffi.compareTo(t8) <= 0) {
            // 0.017 PBB + 33.80% × FFI
            return pbb.multiply(new BigDecimal("0.017")).add(ffi.multiply(new BigDecimal("0.338")));
        } else if (ffi.compareTo(t9) <= 0) {
            // 0.703 PBB + 25.10% × FFI
            return pbb.multiply(new BigDecimal("0.703")).add(ffi.multiply(new BigDecimal("0.251")));
        } else if (ffi.compareTo(t10) <= 0) {
            // 2.732 PBB
            return pbb.multiply(new BigDecimal("2.732"));
        } else if (ffi.compareTo(t11) <= 0) {
            // 9.651 PBB - 62% × FFI
            return pbb.multiply(new BigDecimal("9.651")).subtract(ffi.multiply(new BigDecimal("0.62")));
        } else {
            // 1.691 PBB
            return pbb.multiply(new BigDecimal("1.691"));
        }
    }

    /**
     * Round up to nearest hundred kronor.
     * From SKV 433: "Grundavdraget avrundas uppåt till jämnt hundratal kronor."
     */
    private BigDecimal roundUpToHundred(BigDecimal value) {
        return value.divide(new BigDecimal("100"), 0, RoundingMode.UP)
                .multiply(new BigDecimal("100"));
    }
}
