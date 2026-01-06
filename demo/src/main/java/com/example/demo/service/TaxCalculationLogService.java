package com.example.demo.service;

import com.example.demo.entity.TaxCalculationLog;
import com.example.demo.repository.TaxCalculationLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service for asynchronous logging of tax calculations.
 * Separated from TaxCalculationService to enable Spring proxy-based @Async.
 */
@Service
@RequiredArgsConstructor
public class TaxCalculationLogService {

    private static final Logger log = LoggerFactory.getLogger(TaxCalculationLogService.class);
    private static final int MONTHS_PER_YEAR = 12;
    private static final int SCALE = 2;

    private final TaxCalculationLogRepository taxCalculationLogRepository;

    /**
     * Log the tax calculation for analytics and auditing.
     * Made async to not block the main calculation thread.
     * 
     * @param municipalityId Municipality UUID (we only store ID to avoid detached entity issues)
     * @param grossSalary Monthly gross salary
     * @param yearlyTotalTax Yearly total tax
     * @param netSalary Monthly net salary
     */
    @Async("taskExecutor")
    @Transactional
    public void logCalculationAsync(UUID municipalityId, BigDecimal grossSalary,
                                     BigDecimal yearlyTotalTax, BigDecimal netSalary) {
        try {
            BigDecimal monthlyTax = yearlyTotalTax.divide(
                    BigDecimal.valueOf(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP);
            
            TaxCalculationLog logEntry = new TaxCalculationLog();
            logEntry.setMunicipalityId(municipalityId);
            logEntry.setGrossSalary(grossSalary);
            logEntry.setTotalTax(monthlyTax);
            logEntry.setNetSalary(netSalary);
            
            taxCalculationLogRepository.save(logEntry);
        } catch (Exception e) {
            // Don't fail - this is fire-and-forget
            log.warn("Failed to log tax calculation: {}", e.getMessage());
        }
    }
}
