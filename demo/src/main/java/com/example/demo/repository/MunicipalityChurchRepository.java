package com.example.demo.repository;

import com.example.demo.entity.MunicipalityChurch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MunicipalityChurchRepository extends JpaRepository<MunicipalityChurch, MunicipalityChurch.MunicipalityChurchId> {

    /**
     * Find all churches for a municipality with valid fee rates on a specific date.
     */
    @Query("""
        SELECT mc FROM MunicipalityChurch mc
        WHERE mc.municipality.id = :municipalityId
          AND mc.validFrom <= :date
          AND (mc.validTo IS NULL OR mc.validTo >= :date)
        """)
    List<MunicipalityChurch> findValidChurchesForMunicipality(
            @Param("municipalityId") UUID municipalityId,
            @Param("date") LocalDate date);

    /**
     * Find a specific church fee for a municipality and church on a specific date.
     */
    @Query("""
        SELECT mc FROM MunicipalityChurch mc
        WHERE mc.municipality.id = :municipalityId
          AND mc.church.id = :churchId
          AND mc.validFrom <= :date
          AND (mc.validTo IS NULL OR mc.validTo >= :date)
        """)
    Optional<MunicipalityChurch> findValidFee(
            @Param("municipalityId") UUID municipalityId,
            @Param("churchId") UUID churchId,
            @Param("date") LocalDate date);
}
