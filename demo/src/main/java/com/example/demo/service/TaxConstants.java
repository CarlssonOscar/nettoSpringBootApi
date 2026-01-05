package com.example.demo.service;

import java.math.BigDecimal;

/**
 * Tax constants for Swedish tax calculation (2026 values).
 * Based on SKV 433 - Teknisk beskrivning för skattetabeller 2026, utgåva 36.
 * 
 * Source: Skatteverket SKV 433, december 2025
 */
public final class TaxConstants {

    private TaxConstants() {
        // Prevent instantiation
    }

    // ========================================
    // General constants
    // ========================================

    public static final int TAX_YEAR = 2026;
    public static final int MONTHS_PER_YEAR = 12;

    // ========================================
    // Base amounts (SKV 433 section 1)
    // ========================================

    /**
     * Price base amount (prisbasbelopp) for 2026: 59 200 kr
     * Used for grundavdrag and jobbskatteavdrag calculations.
     */
    public static final BigDecimal PRICE_BASE_AMOUNT = new BigDecimal("59200");

    /**
     * Income base amount (inkomstbasbelopp) for 2026: 83 400 kr
     * Used for allmän pensionsavgift ceiling and public service-avgift.
     */
    public static final BigDecimal INCOME_BASE_AMOUNT = new BigDecimal("83400");

    // ========================================
    // State tax (Statlig skatt) - SKV 433 section 7.2
    // ========================================

    /**
     * State tax threshold (skiktgräns) for 2026: 643 000 kr
     * State tax applies when beskattningsbar förvärvsinkomst exceeds this.
     */
    public static final BigDecimal STATE_TAX_THRESHOLD = new BigDecimal("643000");

    /**
     * State tax rate: 20% on income above threshold.
     */
    public static final BigDecimal STATE_TAX_RATE = new BigDecimal("0.20");

    // ========================================
    // Allmän pensionsavgift - SKV 433 section 7.4
    // ========================================

    /**
     * General pension contribution rate: 7%
     */
    public static final BigDecimal PENSION_CONTRIBUTION_RATE = new BigDecimal("0.07");

    /**
     * Minimum income for pension contribution: 0.423 PBB = 25 041.60 kr
     * Rounded up to 25 042 kr.
     */
    public static final BigDecimal PENSION_CONTRIBUTION_MIN_INCOME = new BigDecimal("25042");

    /**
     * Maximum income base for pension contribution: 8.07 × IBB = 673 038 kr
     */
    public static final BigDecimal PENSION_CONTRIBUTION_MAX_INCOME = new BigDecimal("673038");

    /**
     * Maximum pension contribution: 47 100 kr (7% of 673 038, rounded)
     */
    public static final BigDecimal PENSION_CONTRIBUTION_MAX = new BigDecimal("47100");

    // ========================================
    // Grundavdrag thresholds (PBB factors) - SKV 433 section 6.1
    // ========================================

    /** 0.99 × PBB = 58 608 kr */
    public static final BigDecimal GA_THRESHOLD_1 = new BigDecimal("58608");
    /** 2.72 × PBB = 161 024 kr */
    public static final BigDecimal GA_THRESHOLD_2 = new BigDecimal("161024");
    /** 3.11 × PBB = 184 112 kr */
    public static final BigDecimal GA_THRESHOLD_3 = new BigDecimal("184112");
    /** 7.88 × PBB = 466 496 kr */
    public static final BigDecimal GA_THRESHOLD_4 = new BigDecimal("466496");

    /** 0.423 × PBB = 25 041.60 kr */
    public static final BigDecimal GA_FACTOR_0423_PBB = new BigDecimal("25041.60");
    /** 0.77 × PBB = 45 584.00 kr */
    public static final BigDecimal GA_FACTOR_077_PBB = new BigDecimal("45584.00");
    /** 0.293 × PBB = 17 345.60 kr */
    public static final BigDecimal GA_FACTOR_0293_PBB = new BigDecimal("17345.60");

    // ========================================
    // Jobbskatteavdrag thresholds (PBB factors) - SKV 433 section 7.5.2
    // ========================================

    /** 0.91 × PBB = 53 872 kr */
    public static final BigDecimal JSA_THRESHOLD_1 = new BigDecimal("53872");
    /** 3.24 × PBB = 191 808 kr */
    public static final BigDecimal JSA_THRESHOLD_2 = new BigDecimal("191808");
    /** 8.08 × PBB = 478 336 kr */
    public static final BigDecimal JSA_THRESHOLD_3 = new BigDecimal("478336");

    /** 1.813 × PBB = 107 329.60 kr */
    public static final BigDecimal JSA_FACTOR_1813_PBB = new BigDecimal("107329.60");
    /** 3.027 × PBB = 179 198.40 kr */
    public static final BigDecimal JSA_FACTOR_3027_PBB = new BigDecimal("179198.40");

    // ========================================
    // Jobbskatteavdrag for seniors (66+) - SKV 433 section 7.5.2
    // ========================================

    /** 1.75 × PBB = 103 600 kr */
    public static final BigDecimal JSA_SENIOR_THRESHOLD_1 = new BigDecimal("103600");
    /** 5.24 × PBB = 310 208 kr */
    public static final BigDecimal JSA_SENIOR_THRESHOLD_2 = new BigDecimal("310208");

    /** 0.2635 × PBB = 15 599.20 kr */
    public static final BigDecimal JSA_SENIOR_FACTOR_02635_PBB = new BigDecimal("15599.20");
    /** 0.6293 × PBB = 37 254.56 kr */
    public static final BigDecimal JSA_SENIOR_FACTOR_06293_PBB = new BigDecimal("37254.56");

    // ========================================
    // Skattereduktion för förvärvsinkomst - SKV 433 section 7.5.4
    // ========================================

    /** Threshold where reduction starts: 40 000 kr BFI */
    public static final BigDecimal INCOME_REDUCTION_THRESHOLD = new BigDecimal("40000");
    /** Threshold where max reduction is reached: 240 000 kr BFI */
    public static final BigDecimal INCOME_REDUCTION_MAX_THRESHOLD = new BigDecimal("240000");
    /** Rate for income reduction: 0.75% */
    public static final BigDecimal INCOME_REDUCTION_RATE = new BigDecimal("0.0075");
    /** Maximum income reduction: 1 500 kr */
    public static final BigDecimal INCOME_REDUCTION_MAX = new BigDecimal("1500");

    // ========================================
    // Public service-avgift - SKV 433 section 7.6
    // ========================================

    /** Public service rate: 1% of BFI */
    public static final BigDecimal PUBLIC_SERVICE_RATE = new BigDecimal("0.01");
    /** 1.42 × IBB = 118 428 kr - threshold for max public service fee */
    public static final BigDecimal PUBLIC_SERVICE_THRESHOLD = new BigDecimal("118428");
    /** Maximum public service fee: 1 184 kr (1% of 118 428) */
    public static final BigDecimal PUBLIC_SERVICE_MAX = new BigDecimal("1184");

    // ========================================
    // Begravningsavgift och trossamfundsavgift - SKV 433 section 7.3
    // ========================================

    /**
     * Combined burial and religious community fee included in tax table rates: 1.16%
     * This is subtracted from total rate when calculating jobbskatteavdrag.
     */
    public static final BigDecimal BURIAL_AND_CHURCH_RATE_IN_TABLE = new BigDecimal("0.0116");

    // ========================================
    // Age threshold
    // ========================================

    public static final int SENIOR_AGE_THRESHOLD = 66;

    // ========================================
    // Default rates (fallback if not in database)
    // ========================================

    public static final BigDecimal DEFAULT_MUNICIPAL_TAX_RATE = new BigDecimal("0.2000");
    public static final BigDecimal DEFAULT_REGIONAL_TAX_RATE = new BigDecimal("0.1100");
    public static final BigDecimal DEFAULT_BURIAL_FEE_RATE = new BigDecimal("0.0025");
    public static final BigDecimal DEFAULT_CHURCH_FEE_RATE = new BigDecimal("0.0100");
}
