package com.example.demo.service;

import java.math.BigDecimal;

/**
 * Tax constants for Swedish tax calculation (2025 values).
 * These values are updated yearly by Skatteverket.
 */
public final class TaxConstants {

    private TaxConstants() {
        // Prevent instantiation
    }

    // ========================================
    // General constants
    // ========================================

    public static final int TAX_YEAR = 2025;
    public static final int MONTHS_PER_YEAR = 12;

    // ========================================
    // State tax (Statlig skatt)
    // ========================================

    /**
     * State tax threshold (brytpunkt) - yearly income above which 20% state tax applies.
     * 2025: 613 900 kr
     */
    public static final BigDecimal STATE_TAX_THRESHOLD = new BigDecimal("613900");

    /**
     * State tax rate above the threshold.
     */
    public static final BigDecimal STATE_TAX_RATE = new BigDecimal("0.20");

    // ========================================
    // Basic deduction (Grundavdrag)
    // ========================================

    /**
     * Price base amount (prisbasbelopp) for 2025.
     */
    public static final BigDecimal PRICE_BASE_AMOUNT = new BigDecimal("58800");

    /**
     * Minimum basic deduction: 0.293 × PBB
     */
    public static final BigDecimal BASIC_DEDUCTION_MIN_FACTOR = new BigDecimal("0.293");

    /**
     * Maximum basic deduction: 0.770 × PBB (for incomes in sweet spot)
     */
    public static final BigDecimal BASIC_DEDUCTION_MAX_FACTOR = new BigDecimal("0.770");

    /**
     * Income thresholds for basic deduction calculation (as factors of PBB).
     */
    public static final BigDecimal BASIC_DEDUCTION_THRESHOLD_1 = new BigDecimal("0.99");
    public static final BigDecimal BASIC_DEDUCTION_THRESHOLD_2 = new BigDecimal("2.72");
    public static final BigDecimal BASIC_DEDUCTION_THRESHOLD_3 = new BigDecimal("3.11");
    public static final BigDecimal BASIC_DEDUCTION_THRESHOLD_4 = new BigDecimal("7.88");

    // ========================================
    // Job tax credit (Jobbskatteavdrag)
    // ========================================

    /**
     * Base amount for job tax credit calculation (2025).
     * This is typically close to the price base amount.
     */
    public static final BigDecimal JOB_TAX_CREDIT_BASE = new BigDecimal("58800");

    /**
     * Threshold for full job tax credit (income factor).
     */
    public static final BigDecimal JOB_TAX_CREDIT_FULL_THRESHOLD = new BigDecimal("1.278");

    /**
     * Maximum job tax credit factor.
     */
    public static final BigDecimal JOB_TAX_CREDIT_MAX_FACTOR = new BigDecimal("0.1405");

    /**
     * Age threshold for enhanced job tax credit.
     */
    public static final int SENIOR_AGE_THRESHOLD = 65;

    /**
     * Enhanced job tax credit factor for seniors.
     */
    public static final BigDecimal SENIOR_JOB_TAX_CREDIT_FACTOR = new BigDecimal("0.20");

    // ========================================
    // Default rates (fallback if not in database)
    // ========================================

    public static final BigDecimal DEFAULT_MUNICIPAL_TAX_RATE = new BigDecimal("0.2000");
    public static final BigDecimal DEFAULT_REGIONAL_TAX_RATE = new BigDecimal("0.1100");
    public static final BigDecimal DEFAULT_BURIAL_FEE_RATE = new BigDecimal("0.0025");
    public static final BigDecimal DEFAULT_CHURCH_FEE_RATE = new BigDecimal("0.0100");
}
