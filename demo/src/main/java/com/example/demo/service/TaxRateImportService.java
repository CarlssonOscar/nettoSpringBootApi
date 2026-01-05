package com.example.demo.service;

import com.example.demo.dto.TaxRateImportResult;
import com.example.demo.entity.Municipality;
import com.example.demo.entity.Region;
import com.example.demo.entity.TaxRate;
import com.example.demo.entity.TaxType;
import com.example.demo.repository.MunicipalityRepository;
import com.example.demo.repository.RegionRepository;
import com.example.demo.repository.TaxRateRepository;
import com.example.demo.repository.TaxTypeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for importing tax rates from Skatteverket Excel files.
 * 
 * Expected Excel format (skattesatser-kommuner-YYYY.xlsx):
 * - Column 0: År (Year)
 * - Column 1: Kommunkod (Municipality code, 4 digits)
 * - Column 2: Kommun (Municipality name)
 * - Column 3: Församling (Parish) - optional
 * - Column 4: Kommunal-skatt (Municipal tax rate %)
 * - Column 5: Landstings-skatt (Regional tax rate %)
 * - Column 6: Begravnings-avgift (Burial fee %)
 * - Column 7: Kyrkoavgift (Church fee %)
 * - Column 8: Summa (Total %)
 */
@Service
public class TaxRateImportService {

    private static final Logger log = LoggerFactory.getLogger(TaxRateImportService.class);

    private final RegionRepository regionRepository;
    private final MunicipalityRepository municipalityRepository;
    private final TaxRateRepository taxRateRepository;
    private final TaxTypeRepository taxTypeRepository;

    public TaxRateImportService(
            RegionRepository regionRepository,
            MunicipalityRepository municipalityRepository,
            TaxRateRepository taxRateRepository,
            TaxTypeRepository taxTypeRepository) {
        this.regionRepository = regionRepository;
        this.municipalityRepository = municipalityRepository;
        this.taxRateRepository = taxRateRepository;
        this.taxTypeRepository = taxTypeRepository;
    }

    /**
     * Import tax rates from an Excel file.
     * 
     * @param file The uploaded Excel file
     * @param taxYear The tax year (e.g., 2026)
     * @return Import result with counts and any errors
     */
    @Transactional
    public TaxRateImportResult importFromExcel(MultipartFile file, int taxYear) {
        TaxRateImportResult result = new TaxRateImportResult();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            log.info("Reading sheet: {} with {} rows", sheet.getSheetName(), sheet.getPhysicalNumberOfRows());

            // First, detect column layout by reading header row
            int headerRowIndex = findHeaderRow(sheet);
            if (headerRowIndex < 0) {
                result.addError("Could not find header row with expected columns");
                return result;
            }

            Map<String, Integer> columnMap = mapColumns(sheet.getRow(headerRowIndex));
            log.info("Column mapping: {}", columnMap);

            // Get tax types from database
            TaxType communalType = taxTypeRepository.findByCode("COMMUNAL")
                    .orElseThrow(() -> new RuntimeException("COMMUNAL tax type not found"));
            TaxType regionalType = taxTypeRepository.findByCode("REGIONAL")
                    .orElseThrow(() -> new RuntimeException("REGIONAL tax type not found"));
            TaxType burialType = taxTypeRepository.findByCode("BURIAL")
                    .orElseThrow(() -> new RuntimeException("BURIAL tax type not found"));
            TaxType churchType = taxTypeRepository.findByCode("CHURCH")
                    .orElseThrow(() -> new RuntimeException("CHURCH tax type not found"));

            // Track imported entities
            Map<String, Region> regionsByCode = new HashMap<>();
            Map<String, Municipality> municipalitiesByCode = new HashMap<>();
            Set<String> processedMunicipalities = new HashSet<>();

            LocalDate validFrom = LocalDate.of(taxYear, 1, 1);
            LocalDate validTo = LocalDate.of(taxYear, 12, 31);

            // Process data rows
            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                try {
                    processRow(row, columnMap, taxYear, validFrom, validTo,
                            communalType, regionalType, burialType, churchType,
                            regionsByCode, municipalitiesByCode, processedMunicipalities, result);
                } catch (Exception e) {
                    result.addWarning("Row " + (rowIndex + 1) + ": " + e.getMessage());
                }
            }

            result.setRegionsImported(regionsByCode.size());
            result.setMunicipalitiesImported(municipalitiesByCode.size());

            log.info("Import complete: {} regions, {} municipalities, {} tax rates",
                    result.getRegionsImported(),
                    result.getMunicipalitiesImported(),
                    result.getTaxRatesImported());

        } catch (IOException e) {
            result.addError("Failed to read Excel file: " + e.getMessage());
            log.error("Failed to import tax rates", e);
        }

        return result;
    }

    /**
     * Find the header row by looking for known column names.
     */
    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (Cell cell : row) {
                String value = getCellAsString(cell).toLowerCase();
                if (value.contains("kommun") || value.contains("kommunalskatt")) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Map column names to indices.
     */
    private Map<String, Integer> mapColumns(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();

        for (Cell cell : headerRow) {
            String header = getCellAsString(cell).toLowerCase().trim();
            int colIndex = cell.getColumnIndex();

            if (header.contains("kommunkod") || header.equals("kod")) {
                columnMap.put("code", colIndex);
            } else if (header.equals("kommun") || header.contains("kommunnamn")) {
                columnMap.put("name", colIndex);
            } else if (header.contains("kommunal") && header.contains("skatt")) {
                columnMap.put("communal", colIndex);
            } else if (header.contains("landsting") || header.contains("region")) {
                columnMap.put("regional", colIndex);
            } else if (header.contains("begravning")) {
                columnMap.put("burial", colIndex);
            } else if (header.contains("kyrko")) {
                columnMap.put("church", colIndex);
            } else if (header.contains("summa") || header.contains("total")) {
                columnMap.put("total", colIndex);
            }
        }

        return columnMap;
    }

    /**
     * Process a single row of data.
     */
    private void processRow(
            Row row,
            Map<String, Integer> columnMap,
            int taxYear,
            LocalDate validFrom,
            LocalDate validTo,
            TaxType communalType,
            TaxType regionalType,
            TaxType burialType,
            TaxType churchType,
            Map<String, Region> regionsByCode,
            Map<String, Municipality> municipalitiesByCode,
            Set<String> processedMunicipalities,
            TaxRateImportResult result) {

        // Get municipality code
        String municipalityCode = getCellAsString(row.getCell(columnMap.getOrDefault("code", 1)));
        if (municipalityCode == null || municipalityCode.length() < 4) {
            return; // Skip non-data rows
        }

        // Ensure 4-digit code with leading zeros
        municipalityCode = String.format("%04d", Integer.parseInt(municipalityCode.replaceAll("[^0-9]", "")));

        // Skip if already processed (handles duplicate rows for different parishes)
        if (processedMunicipalities.contains(municipalityCode)) {
            return;
        }
        processedMunicipalities.add(municipalityCode);

        // Get municipality name
        String municipalityName = getCellAsString(row.getCell(columnMap.getOrDefault("name", 2)));
        if (municipalityName == null || municipalityName.isEmpty()) {
            return;
        }

        // Extract region code (first 2 digits of municipality code)
        String regionCode = municipalityCode.substring(0, 2);

        // Find or create region
        Region region = regionsByCode.computeIfAbsent(regionCode, code -> {
            Region existing = regionRepository.findByCode(code).orElse(null);
            if (existing != null) {
                return existing;
            }

            Region newRegion = new Region();
            newRegion.setCode(code);
            newRegion.setName(getRegionName(code));
            newRegion.setCreatedAt(LocalDateTime.now());
            newRegion.setUpdatedAt(LocalDateTime.now());
            return regionRepository.save(newRegion);
        });

        // Find or create municipality
        Municipality municipality = municipalitiesByCode.computeIfAbsent(municipalityCode, code -> {
            Municipality existing = municipalityRepository.findByCode(code).orElse(null);
            if (existing != null) {
                return existing;
            }

            Municipality newMunicipality = new Municipality();
            newMunicipality.setCode(code);
            newMunicipality.setName(municipalityName);
            newMunicipality.setRegion(region);
            newMunicipality.setCreatedAt(LocalDateTime.now());
            newMunicipality.setUpdatedAt(LocalDateTime.now());
            return municipalityRepository.save(newMunicipality);
        });

        // Get tax rates
        BigDecimal communalRate = getCellAsDecimal(row.getCell(columnMap.getOrDefault("communal", 4)));
        BigDecimal regionalRate = getCellAsDecimal(row.getCell(columnMap.getOrDefault("regional", 5)));
        BigDecimal burialRate = getCellAsDecimal(row.getCell(columnMap.getOrDefault("burial", 6)));
        BigDecimal churchRate = getCellAsDecimal(row.getCell(columnMap.getOrDefault("church", 7)));

        // Save tax rates
        if (communalRate != null) {
            saveTaxRate(communalType, municipality, null, communalRate, validFrom, validTo);
            result.setTaxRatesImported(result.getTaxRatesImported() + 1);
        }

        if (regionalRate != null) {
            saveTaxRate(regionalType, municipality, region, regionalRate, validFrom, validTo);
            result.setTaxRatesImported(result.getTaxRatesImported() + 1);
        }

        if (burialRate != null) {
            saveTaxRate(burialType, municipality, null, burialRate, validFrom, validTo);
            result.setTaxRatesImported(result.getTaxRatesImported() + 1);
        }

        if (churchRate != null) {
            saveTaxRate(churchType, municipality, null, churchRate, validFrom, validTo);
            result.setTaxRatesImported(result.getTaxRatesImported() + 1);
        }
    }

    /**
     * Save or update a tax rate.
     */
    private void saveTaxRate(
            TaxType taxType,
            Municipality municipality,
            Region region,
            BigDecimal rate,
            LocalDate validFrom,
            LocalDate validTo) {

        TaxRate taxRate = new TaxRate();
        taxRate.setTaxType(taxType);
        taxRate.setMunicipality(municipality);
        taxRate.setRegion(region);
        // Convert percentage to decimal (e.g., 20.35% -> 0.2035)
        taxRate.setRate(rate.divide(BigDecimal.valueOf(100)));
        taxRate.setValidFrom(validFrom);
        taxRate.setValidTo(validTo);
        taxRate.setCreatedAt(LocalDateTime.now());
        taxRate.setUpdatedAt(LocalDateTime.now());

        taxRateRepository.save(taxRate);
    }

    /**
     * Get cell value as string.
     */
    private String getCellAsString(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    /**
     * Get cell value as BigDecimal.
     */
    private BigDecimal getCellAsDecimal(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try {
                    String value = cell.getStringCellValue().replace(",", ".").trim();
                    yield new BigDecimal(value);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    /**
     * Get region name from region code.
     * Swedish regions (län).
     */
    private String getRegionName(String code) {
        return switch (code) {
            case "01" -> "Stockholms län";
            case "03" -> "Uppsala län";
            case "04" -> "Södermanlands län";
            case "05" -> "Östergötlands län";
            case "06" -> "Jönköpings län";
            case "07" -> "Kronobergs län";
            case "08" -> "Kalmar län";
            case "09" -> "Gotlands län";
            case "10" -> "Blekinge län";
            case "12" -> "Skåne län";
            case "13" -> "Hallands län";
            case "14" -> "Västra Götalands län";
            case "17" -> "Värmlands län";
            case "18" -> "Örebro län";
            case "19" -> "Västmanlands län";
            case "20" -> "Dalarnas län";
            case "21" -> "Gävleborgs län";
            case "22" -> "Västernorrlands län";
            case "23" -> "Jämtlands län";
            case "24" -> "Västerbottens län";
            case "25" -> "Norrbottens län";
            default -> "Okänt län (" + code + ")";
        };
    }
}
