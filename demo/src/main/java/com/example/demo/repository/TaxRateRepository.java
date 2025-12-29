package com.example.demo.repository;

import com.example.demo.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, UUID> {

    /**
     * Find all tax rates for a municipality valid on a specific date.
     */
    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.municipality.id = :municipalityId
          AND tr.validFrom <= :date
          AND (tr.validTo IS NULL OR tr.validTo >= :date)
        """)
    List<TaxRate> findValidRatesForMunicipality(
            @Param("municipalityId") UUID municipalityId,
            @Param("date") LocalDate date);

    /**
     * Find all tax rates for a region valid on a specific date.
     */
    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.region.id = :regionId
          AND tr.validFrom <= :date
          AND (tr.validTo IS NULL OR tr.validTo >= :date)
        """)
    List<TaxRate> findValidRatesForRegion(
            @Param("regionId") UUID regionId,
            @Param("date") LocalDate date);

    /**
     * Find a specific tax rate by municipality, tax type and date.
     */
    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.municipality.id = :municipalityId
          AND tr.taxType.code = :taxTypeCode
          AND tr.validFrom <= :date
          AND (tr.validTo IS NULL OR tr.validTo >= :date)
        """)
    Optional<TaxRate> findByMunicipalityAndTaxType(
            @Param("municipalityId") UUID municipalityId,
            @Param("taxTypeCode") String taxTypeCode,
            @Param("date") LocalDate date);

    /**
     * Find a specific tax rate by region, tax type and date.
     */
    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.region.id = :regionId
          AND tr.taxType.code = :taxTypeCode
          AND tr.validFrom <= :date
          AND (tr.validTo IS NULL OR tr.validTo >= :date)
        """)
    Optional<TaxRate> findByRegionAndTaxType(
            @Param("regionId") UUID regionId,
            @Param("taxTypeCode") String taxTypeCode,
            @Param("date") LocalDate date);
}
