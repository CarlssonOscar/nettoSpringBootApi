-- ============================================
-- V2: Prepare for tax rate import
-- Clean up any test data and ensure indexes
-- ============================================

-- Clear existing data (respecting foreign key constraints)
-- This allows clean re-import from Skatteverket Excel files
DELETE FROM tax_calculation_log;
DELETE FROM tax_rate;
DELETE FROM municipality_church;
DELETE FROM municipality;
DELETE FROM region;

-- Add index for faster lookups during import
CREATE INDEX IF NOT EXISTS idx_region_code ON region(code);
CREATE INDEX IF NOT EXISTS idx_municipality_code ON municipality(code);

-- Add composite index for tax rate lookups
CREATE INDEX IF NOT EXISTS idx_tax_rate_lookup ON tax_rate(
    tax_type_id, 
    municipality_id, 
    valid_from, 
    valid_to
);
