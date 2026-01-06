package com.example.demo.service;

import com.example.demo.entity.Municipality;
import com.example.demo.entity.MunicipalityChurch;
import com.example.demo.entity.TaxRate;
import com.example.demo.repository.MunicipalityChurchRepository;
import com.example.demo.repository.MunicipalityRepository;
import com.example.demo.repository.TaxRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.demo.service.TaxConstants.*;

/**
 * Service for retrieving tax rates for a municipality.
 */
@Service
public class TaxRateService {

    private static final Logger log = LoggerFactory.getLogger(TaxRateService.class);

    private final MunicipalityRepository municipalityRepository;
    private final TaxRateRepository taxRateRepository;
    private final MunicipalityChurchRepository municipalityChurchRepository;

    public TaxRateService(MunicipalityRepository municipalityRepository,
                          TaxRateRepository taxRateRepository,
                          MunicipalityChurchRepository municipalityChurchRepository) {
        this.municipalityRepository = municipalityRepository;
        this.taxRateRepository = taxRateRepository;
        this.municipalityChurchRepository = municipalityChurchRepository;
    }

    /**
     * Get all tax rates for a municipality in a single query.
     * Returns a map of tax type code to rate.
     * This is the optimized method - ONE query instead of 4.
     * Results are cached for 1 hour since tax rates rarely change.
     */
    @Cacheable(value = "taxRates", key = "#municipalityId.toString() + '_' + #date.toString()")
    public Map<String, BigDecimal> getAllTaxRates(UUID municipalityId, LocalDate date) {
        log.debug("Cache miss - fetching tax rates from database for municipality {}", municipalityId);
        List<TaxRate> rates = taxRateRepository.findValidRatesForMunicipality(municipalityId, date);
        
        return rates.stream()
                .collect(Collectors.toMap(
                        tr -> tr.getTaxType().getCode(),
                        TaxRate::getRate,
                        (existing, replacement) -> existing // Keep first if duplicates
                ));
    }

    /**
     * Get municipal tax rate for a municipality.
     */
    public BigDecimal getMunicipalTaxRate(UUID municipalityId, LocalDate date) {
        return taxRateRepository.findByMunicipalityAndTaxType(municipalityId, "COMMUNAL", date)
                .map(TaxRate::getRate)
                .orElseGet(() -> {
                    log.info("No municipal tax rate found for municipality {}, using default", municipalityId);
                    return DEFAULT_MUNICIPAL_TAX_RATE;
                });
    }

    /**
     * Get regional tax rate for a municipality.
     * Note: Regional rates are stored per municipality in the tax_rate table.
     */
    public BigDecimal getRegionalTaxRate(UUID municipalityId, LocalDate date) {
        return taxRateRepository.findByMunicipalityAndTaxType(municipalityId, "REGIONAL", date)
                .map(TaxRate::getRate)
                .orElseGet(() -> {
                    log.info("No regional tax rate found for municipality {}, using default", municipalityId);
                    return DEFAULT_REGIONAL_TAX_RATE;
                });
    }

    /**
     * Get burial fee rate for a municipality.
     */
    public BigDecimal getBurialFeeRate(UUID municipalityId, LocalDate date) {
        return taxRateRepository.findByMunicipalityAndTaxType(municipalityId, "BURIAL", date)
                .map(TaxRate::getRate)
                .orElseGet(() -> {
                    log.debug("No burial fee rate found for municipality {}, using default", municipalityId);
                    return DEFAULT_BURIAL_FEE_RATE;
                });
    }

    /**
     * Get church fee rate for a municipality.
     * First tries tax_rate table, then municipality_church, then default.
     */
    public BigDecimal getChurchFeeRate(UUID municipalityId, LocalDate date) {
        // First try to get from tax_rate table (imported from Excel)
        Optional<TaxRate> churchRate = taxRateRepository.findByMunicipalityAndTaxType(municipalityId, "CHURCH", date);
        if (churchRate.isPresent()) {
            return churchRate.get().getRate();
        }

        // Fallback to municipality_church table
        return municipalityChurchRepository.findValidChurchesForMunicipality(municipalityId, date)
                .stream()
                .findFirst()
                .map(MunicipalityChurch::getFeeRate)
                .orElseGet(() -> {
                    log.debug("No church fee found for municipality {}, using default", municipalityId);
                    return DEFAULT_CHURCH_FEE_RATE;
                });
    }

    /**
     * Get municipality with region info.
     * Cached since municipality data rarely changes.
     */
    @Cacheable(value = "municipalities", key = "#municipalityId.toString()")
    public Optional<Municipality> getMunicipality(UUID municipalityId) {
        log.debug("Cache miss - fetching municipality from database: {}", municipalityId);
        return municipalityRepository.findById(municipalityId);
    }

    /**
     * Get municipality by code (e.g., "0180" for Stockholm).
     * Cached since municipality data rarely changes.
     */
    @Cacheable(value = "municipalities", key = "'code_' + #code")
    public Optional<Municipality> getMunicipalityByCode(String code) {
        log.debug("Cache miss - fetching municipality by code from database: {}", code);
        return municipalityRepository.findByCode(code);
    }
}
