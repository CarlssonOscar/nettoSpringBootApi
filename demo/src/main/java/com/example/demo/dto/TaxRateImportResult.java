package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a tax rate import operation.
 */
public class TaxRateImportResult {

    private int regionsImported;
    private int municipalitiesImported;
    private int taxRatesImported;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public TaxRateImportResult() {}

    public void addError(String error) {
        errors.add(error);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // Getters and Setters
    public int getRegionsImported() {
        return regionsImported;
    }

    public void setRegionsImported(int regionsImported) {
        this.regionsImported = regionsImported;
    }

    public int getMunicipalitiesImported() {
        return municipalitiesImported;
    }

    public void setMunicipalitiesImported(int municipalitiesImported) {
        this.municipalitiesImported = municipalitiesImported;
    }

    public int getTaxRatesImported() {
        return taxRatesImported;
    }

    public void setTaxRatesImported(int taxRatesImported) {
        this.taxRatesImported = taxRatesImported;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
