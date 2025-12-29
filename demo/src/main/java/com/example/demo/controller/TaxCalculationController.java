package com.example.demo.controller;

import com.example.demo.dto.TaxCalculationRequest;
import com.example.demo.dto.TaxCalculationResponse;
import com.example.demo.service.TaxCalculationService;
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
@RequestMapping("/api/tax")
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
     * @param churchMember Whether the person is a church member
     * @param churchId Church UUID (optional)
     * @param birthYear Birth year for age-based calculations
     * @return Detailed tax calculation breakdown
     */
    @GetMapping("/calculate")
    public ResponseEntity<TaxCalculationResponse> calculate(
            @RequestParam UUID municipalityId,
            @RequestParam BigDecimal grossSalary,
            @RequestParam(defaultValue = "false") boolean churchMember,
            @RequestParam(required = false) UUID churchId,
            @RequestParam(defaultValue = "1985") int birthYear) {

        log.info("Tax calculation request: municipality={}, grossSalary={}, churchMember={}, birthYear={}",
                municipalityId, grossSalary, churchMember, birthYear);

        TaxCalculationRequest request = new TaxCalculationRequest(
                municipalityId,
                grossSalary,
                churchMember,
                churchId,
                birthYear
        );

        TaxCalculationResponse response = taxCalculationService.calculate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST endpoint for tax calculation with request body.
     */
    @PostMapping("/calculate")
    public ResponseEntity<TaxCalculationResponse> calculatePost(
            @RequestBody TaxCalculationRequest request) {

        log.info("Tax calculation POST request: municipality={}, grossSalary={}",
                request.municipalityId(), request.grossMonthlySalary());

        TaxCalculationResponse response = taxCalculationService.calculate(request);
        return ResponseEntity.ok(response);
    }
}
