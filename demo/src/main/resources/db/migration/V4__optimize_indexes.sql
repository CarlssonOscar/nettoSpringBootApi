-- ============================================
-- V3: Optimize indexes for tax calculation queries
-- ============================================

-- PROBLEM: The main query `findValidRatesForMunicipality` searches by:
--   municipality_id = :municipalityId
--   validFrom <= :date
--   (validTo IS NULL OR validTo >= :date)
--
-- The existing idx_tax_rate_lookup has tax_type_id FIRST, making it inefficient
-- for this query pattern.

-- Drop the suboptimal composite index
DROP INDEX IF EXISTS idx_tax_rate_lookup;

-- Create optimal composite index for the main query pattern:
-- 1. municipality_id (equality) FIRST for exact match
-- 2. valid_from (range) for date filtering
-- 3. INCLUDE tax_type_id for covering index (avoids table lookup)
CREATE INDEX IF NOT EXISTS idx_tax_rate_municipality_validity ON tax_rate(
    municipality_id, 
    valid_from DESC, 
    valid_to
) INCLUDE (tax_type_id, rate);

-- Index for finding rates by tax_type code (joined query)
-- Supports findByMunicipalityAndTaxType query
CREATE INDEX IF NOT EXISTS idx_tax_type_code ON tax_type(code);

-- Partial index for active rates (valid_to IS NULL) - most common case
CREATE INDEX IF NOT EXISTS idx_tax_rate_active ON tax_rate(municipality_id, valid_from DESC)
WHERE valid_to IS NULL;

-- Index for region-based tax rate lookups
CREATE INDEX IF NOT EXISTS idx_tax_rate_region_validity ON tax_rate(
    region_id,
    valid_from DESC,
    valid_to
) WHERE region_id IS NOT NULL;

-- Composite index for municipality_church lookups
CREATE INDEX IF NOT EXISTS idx_municipality_church_lookup ON municipality_church(
    municipality_id,
    valid_from DESC,
    valid_to
);

-- Ensure tax_calculation_log has index for municipality_id
-- (if you want to query logs by municipality)
CREATE INDEX IF NOT EXISTS idx_calculation_log_municipality 
ON tax_calculation_log(municipality_id);

-- VACUUM ANALYZE to update statistics after adding indexes
-- (This is a hint - needs to be run manually in production)
-- ANALYZE tax_rate;
-- ANALYZE municipality;
-- ANALYZE tax_type;
