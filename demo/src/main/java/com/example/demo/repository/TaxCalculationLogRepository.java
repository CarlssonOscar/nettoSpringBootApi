package com.example.demo.repository;

import com.example.demo.entity.TaxCalculationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaxCalculationLogRepository extends JpaRepository<TaxCalculationLog, UUID> {

    List<TaxCalculationLog> findByMunicipalityId(UUID municipalityId);

    List<TaxCalculationLog> findByCalculatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<TaxCalculationLog> findTop10ByOrderByCalculatedAtDesc();
}
