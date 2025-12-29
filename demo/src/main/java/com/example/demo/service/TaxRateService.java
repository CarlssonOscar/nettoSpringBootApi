package com.example.demo.service;

import com.example.demo.entity.Municipality;
import com.example.demo.entity.MunicipalityChurch;
import com.example.demo.entity.TaxRate;
import com.example.demo.repository.MunicipalityChurchRepository;
import com.example.demo.repository.MunicipalityRepository;
import com.example.demo.repository.TaxRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

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
     * Get municipal tax rate for a municipality.
     */
    public BigDecimal getMunicipalTaxRate(UUID municipalityId, LocalDate date) {
        return taxRateRepository.findByMunicipalityAndTaxType(municipalityId, "COMMUNAL", date)
                .map(TaxRate::getRate)
                .orElseGet(() -> {
                    log.warn("No municipal tax rate found for municipality {}, using default", municipalityId);
                    return DEFAULT_MUNICIPAL_TAX_RATE;
                });
    }

    /**
     * Get regional tax rate for a municipality's region.
     */
    public BigDecimal getRegionalTaxRate(UUID municipalityId, LocalDate date) {
        return municipalityRepository.findById(municipalityId)
                .flatMap(municipality -> taxRateRepository.findByRegionAndTaxType(
                        municipality.getRegion().getId(), "REGIONAL", date))
                .map(TaxRate::getRate)
                .orElseGet(() -> {
                    log.warn("No regional tax rate found for municipality {}, using default", municipalityId);
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
     * Uses average/default rate since specific congregation is not selected.
     */
    public BigDecimal getChurchFeeRate(UUID municipalityId, LocalDate date) {
        // Get first valid church fee for municipality, or use default
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
     */
    public Optional<Municipality> getMunicipality(UUID municipalityId) {
        return municipalityRepository.findById(municipalityId);
    }
}
