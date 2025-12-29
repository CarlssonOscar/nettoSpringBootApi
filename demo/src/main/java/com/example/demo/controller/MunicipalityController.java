package com.example.demo.controller;

import com.example.demo.entity.Municipality;
import com.example.demo.entity.Region;
import com.example.demo.repository.MunicipalityRepository;
import com.example.demo.repository.RegionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for municipality and region lookups.
 * Used by frontend to populate dropdowns.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Municipalities & Regions", description = "Endpoints for retrieving Swedish municipalities and regions")
public class MunicipalityController {

    private final RegionRepository regionRepository;
    private final MunicipalityRepository municipalityRepository;

    public MunicipalityController(RegionRepository regionRepository,
                                   MunicipalityRepository municipalityRepository) {
        this.regionRepository = regionRepository;
        this.municipalityRepository = municipalityRepository;
    }

    /**
     * Get all regions.
     */
    @GetMapping("/regions")
    @Operation(summary = "Get all regions", description = "Returns all Swedish regions (län)")
    public ResponseEntity<List<Region>> getAllRegions() {
        List<Region> regions = regionRepository.findAll();
        return ResponseEntity.ok(regions);
    }

    /**
     * Get all municipalities.
     */
    @GetMapping("/municipalities")
    public ResponseEntity<List<Municipality>> getAllMunicipalities() {
        List<Municipality> municipalities = municipalityRepository.findAll();
        return ResponseEntity.ok(municipalities);
    }

    /**
     * Get municipalities by region.
     */
    @GetMapping("/municipalities/by-region/{regionId}")
    public ResponseEntity<List<Municipality>> getMunicipalitiesByRegion(
            @PathVariable UUID regionId) {
        List<Municipality> municipalities = municipalityRepository.findByRegionId(regionId);
        return ResponseEntity.ok(municipalities);
    }

    /**
     * Get municipality by ID.
     */
    @GetMapping("/municipalities/{id}")
    public ResponseEntity<Municipality> getMunicipalityById(@PathVariable UUID id) {
        return municipalityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
