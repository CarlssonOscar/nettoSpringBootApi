package com.example.demo.controller;

import com.example.demo.dto.TaxCalculationRequest;
import com.example.demo.dto.TaxCalculationResponse;
import com.example.demo.service.TaxCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for tax calculations.
 */
@RestController
@RequestMapping("/api/v1/tax")
@Tag(name = "Tax Calculation", description = "Endpoints for calculating Swedish net salary")
public class TaxCalculationController {

    private static final Logger log = LoggerFactory.getLogger(TaxCalculationController.class);

    private final TaxCalculationService taxCalculationService;

    public TaxCalculationController(TaxCalculationService taxCalculationService) {
        this.taxCalculationService = taxCalculationService;
    }

    /**
     * Calculate net salary from gross salary.
     *
     * @param municipalityId Municipality UUID
     * @param grossSalary Monthly gross salary
     * @param churchMember Whether the person is a Swedish church member
     * @param isPensioner Whether the person is 65+ years old
     * @return Detailed tax calculation breakdown
     */
    @GetMapping("/calculate")
    @Operation(summary = "Calculate net salary by municipality UUID")
    public ResponseEntity<TaxCalculationResponse> calculate(
            @RequestParam UUID municipalityId,
            @RequestParam BigDecimal grossSalary,
            @RequestParam(defaultValue = "false") boolean churchMember,
            @RequestParam(defaultValue = "false") boolean isPensioner) {

        log.info("Tax calculation request: municipality={}, grossSalary={}, churchMember={}, isPensioner={}",
                municipalityId, grossSalary, churchMember, isPensioner);

        TaxCalculationRequest request = new TaxCalculationRequest(
                municipalityId,
                grossSalary,
                churchMember,
                isPensioner
        );

        TaxCalculationResponse response = taxCalculationService.calculate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Calculate net salary using municipality code (e.g., "0180" for Stockholm).
     */
    @GetMapping("/calculate-by-code")
    @Operation(summary = "Calculate net salary by municipality code", 
               description = "Use 4-digit municipality code (e.g., 0180 for Stockholm)")
    public ResponseEntity<TaxCalculationResponse> calculateByCode(
            @Parameter(description = "4-digit municipality code, e.g., 0180 for Stockholm", example = "0180")
            @RequestParam String municipalityCode,
            @Parameter(description = "Monthly gross salary in SEK", example = "45000")
            @RequestParam BigDecimal grossSalary,
            @Parameter(description = "Is Swedish Church member?")
            @RequestParam(defaultValue = "false") boolean churchMember,
            @Parameter(description = "Is 65+ years old?")
            @RequestParam(defaultValue = "false") boolean isPensioner) {

        log.info("Tax calculation by code: municipality={}, grossSalary={}, churchMember={}, isPensioner={}",
                municipalityCode, grossSalary, churchMember, isPensioner);

        TaxCalculationResponse response = taxCalculationService.calculateByMunicipalityCode(
                municipalityCode, grossSalary, churchMember, isPensioner);
        return ResponseEntity.ok(response);
    }

    /**
     * POST endpoint for tax calculation with request body.
     */
    @PostMapping("/calculate")
    public ResponseEntity<TaxCalculationResponse> calculatePost(
            @Valid @RequestBody TaxCalculationRequest request) {

        log.info("Tax calculation POST request: municipality={}, grossSalary={}",
                request.municipalityId(), request.grossMonthlySalary());

        TaxCalculationResponse response = taxCalculationService.calculate(request);
        return ResponseEntity.ok(response);
    }
}
