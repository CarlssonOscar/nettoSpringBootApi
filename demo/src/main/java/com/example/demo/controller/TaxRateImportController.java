package com.example.demo.controller;

import com.example.demo.dto.TaxRateImportResult;
import com.example.demo.service.TaxRateImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for administrative import operations.
 * 
 * Endpoints for importing tax data from Skatteverket Excel files.
 */
@RestController
@RequestMapping("/api/v1/admin/import")
@Tag(name = "Admin Import", description = "Administrative endpoints for importing tax data")
public class TaxRateImportController {

    private final TaxRateImportService taxRateImportService;

    public TaxRateImportController(TaxRateImportService taxRateImportService) {
        this.taxRateImportService = taxRateImportService;
    }

    @Operation(
            summary = "Import tax rates from Excel",
            description = "Upload a Skatteverket Excel file (skattesatser-kommuner-YYYY.xlsx) to import " +
                    "municipality tax rates including kommunalskatt, regionalskatt, begravningsavgift, and kyrkoavgift."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Import completed (check result for details)",
                    content = @Content(schema = @Schema(implementation = TaxRateImportResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file or missing parameters"
            )
    })
    @PostMapping(value = "/tax-rates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaxRateImportResult> importTaxRates(
            @Parameter(description = "Skatteverket Excel file (.xlsx)", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Tax year (e.g., 2026)", required = true)
            @RequestParam("year") int year) {

        if (file.isEmpty()) {
            TaxRateImportResult result = new TaxRateImportResult();
            result.addError("No file uploaded");
            return ResponseEntity.badRequest().body(result);
        }

        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            TaxRateImportResult result = new TaxRateImportResult();
            result.addError("Only .xlsx files are supported");
            return ResponseEntity.badRequest().body(result);
        }

        TaxRateImportResult result = taxRateImportService.importFromExcel(file, year);

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}
