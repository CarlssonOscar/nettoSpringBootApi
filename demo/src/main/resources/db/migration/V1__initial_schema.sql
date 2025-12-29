-- ============================================
-- NettoApi Initial Database Schema
-- Swedish Salary/Tax Calculator
-- ============================================

-- 1. Region table (21 regions in Sweden)
CREATE TABLE region (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(2) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Municipality table (290 municipalities)
CREATE TABLE municipality (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(4) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    region_id UUID NOT NULL REFERENCES region(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_municipality_region ON municipality(region_id);

-- 3. Tax type table (types of taxes and deductions)
CREATE TABLE tax_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tax rate table (core table - all tax rates and deductions)
CREATE TABLE tax_rate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tax_type_id UUID NOT NULL REFERENCES tax_type(id),
    municipality_id UUID REFERENCES municipality(id),
    region_id UUID REFERENCES region(id),
    rate DECIMAL(10, 6) NOT NULL,
    income_from DECIMAL(12, 2),
    income_to DECIMAL(12, 2),
    valid_from DATE NOT NULL,
    valid_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tax_rate_type ON tax_rate(tax_type_id);
CREATE INDEX idx_tax_rate_municipality ON tax_rate(municipality_id);
CREATE INDEX idx_tax_rate_region ON tax_rate(region_id);
CREATE INDEX idx_tax_rate_validity ON tax_rate(valid_from, valid_to);

-- 5. Church table (religious organizations)
CREATE TABLE church (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Municipality-Church mapping (N:M relationship)
CREATE TABLE municipality_church (
    municipality_id UUID NOT NULL REFERENCES municipality(id),
    church_id UUID NOT NULL REFERENCES church(id),
    fee_rate DECIMAL(10, 6) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (municipality_id, church_id, valid_from)
);

-- 7. Tax calculation log (for statistics and demo)
CREATE TABLE tax_calculation_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    municipality_id UUID REFERENCES municipality(id),
    gross_salary DECIMAL(12, 2) NOT NULL,
    total_tax DECIMAL(12, 2) NOT NULL,
    net_salary DECIMAL(12, 2) NOT NULL,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_calculation_log_date ON tax_calculation_log(calculated_at);

-- ============================================
-- Seed data: Tax types
-- ============================================
INSERT INTO tax_type (code, description) VALUES
    ('COMMUNAL', 'Kommunalskatt'),
    ('REGIONAL', 'Regionalskatt'),
    ('STATE', 'Statlig inkomstskatt'),
    ('CHURCH', 'Kyrkoavgift'),
    ('BURIAL', 'Begravningsavgift'),
    ('BASIC_DEDUCTION', 'Grundavdrag'),
    ('JOB_TAX_CREDIT', 'Jobbskatteavdrag');
