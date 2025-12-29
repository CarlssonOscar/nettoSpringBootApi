package com.example.demo.repository;

import com.example.demo.entity.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MunicipalityRepository extends JpaRepository<Municipality, UUID> {

    Optional<Municipality> findByCode(String code);

    Optional<Municipality> findByName(String name);

    List<Municipality> findByRegionId(UUID regionId);

    List<Municipality> findByRegionCode(String regionCode);

    boolean existsByCode(String code);
}
