--
-- Copyright © 2016-2026 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- ============================================================================
-- Production Facilities (PF) Module - Database Schema
-- Version: 2.0 (ThingsBoard Core Architecture)
-- Date: 2026-02-03
-- Description: Schema for PF module custom tables
--
-- NOTE: In this architecture:
-- - Telemetry is stored in ThingsBoard ts_kv tables
-- - Alarms are managed by ThingsBoard Alarm System
-- - Operational limits are configured as Alarm Rules in Asset Profiles
-- - Only custom configuration tables are defined here
-- ============================================================================

-- Create schema
CREATE SCHEMA IF NOT EXISTS pf;

-- ============================================================================
-- DATA QUALITY RULES TABLE
-- Stores rules for validating telemetry data quality
-- This is a custom table because data quality rules need versioning
-- and are not directly supported by TB core.
-- ============================================================================

CREATE TABLE IF NOT EXISTS pf.data_quality_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variable_key VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    physical_min NUMERIC(18,4),
    physical_max NUMERIC(18,4),
    expected_min NUMERIC(18,4),
    expected_max NUMERIC(18,4),
    max_rate_of_change_percent_per_minute NUMERIC(10,4),
    max_absolute_change_per_minute NUMERIC(18,4),
    outlier_sigma_threshold NUMERIC(5,2) DEFAULT 3.0,
    min_samples_for_statistics INTEGER DEFAULT 30,
    unit VARCHAR(50),
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    updated_time BIGINT,
    UNIQUE(variable_key, entity_type, entity_id)
);

-- Indexes for efficient rule lookup
CREATE INDEX IF NOT EXISTS idx_dq_rule_variable
    ON pf.data_quality_rule(variable_key);
CREATE INDEX IF NOT EXISTS idx_dq_rule_entity_type
    ON pf.data_quality_rule(entity_type)
    WHERE entity_type IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_dq_rule_entity_id
    ON pf.data_quality_rule(entity_id)
    WHERE entity_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_dq_rule_enabled
    ON pf.data_quality_rule(enabled)
    WHERE enabled = TRUE;

-- Function to get the most specific rule for a variable
CREATE OR REPLACE FUNCTION pf.get_applicable_rule(
    p_variable_key VARCHAR(255),
    p_entity_type VARCHAR(50),
    p_entity_id UUID
) RETURNS pf.data_quality_rule AS $$
DECLARE
    v_rule pf.data_quality_rule%ROWTYPE;
BEGIN
    -- Try entity-specific rule first
    SELECT * INTO v_rule
    FROM pf.data_quality_rule
    WHERE variable_key = p_variable_key
      AND entity_id = p_entity_id
      AND enabled = TRUE;

    IF FOUND THEN
        RETURN v_rule;
    END IF;

    -- Try entity-type rule
    SELECT * INTO v_rule
    FROM pf.data_quality_rule
    WHERE variable_key = p_variable_key
      AND entity_type = p_entity_type
      AND entity_id IS NULL
      AND enabled = TRUE;

    IF FOUND THEN
        RETURN v_rule;
    END IF;

    -- Try global rule
    SELECT * INTO v_rule
    FROM pf.data_quality_rule
    WHERE variable_key = p_variable_key
      AND entity_type IS NULL
      AND entity_id IS NULL
      AND enabled = TRUE;

    RETURN v_rule;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- OPTIMIZATION RESULTS TABLE (Future - Production Optimization Module)
-- Stores results from optimization algorithms
-- ============================================================================

-- CREATE TABLE IF NOT EXISTS pf.optimization_result (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     tenant_id UUID NOT NULL,
--     well_id UUID NOT NULL,  -- References TB Asset
--     optimization_type VARCHAR(50) NOT NULL,
--     optimization_date BIGINT NOT NULL,
--     status VARCHAR(50) NOT NULL,
--     input_parameters JSONB,
--     results JSONB,
--     recommendations JSONB,
--     confidence_score NUMERIC(5,4),
--     created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
--     created_by UUID
-- );

-- ============================================================================
-- RECOMMENDATIONS TABLE (Future - Production Optimization Module)
-- Stores AI-generated recommendations
-- ============================================================================

-- CREATE TABLE IF NOT EXISTS pf.recommendation (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     tenant_id UUID NOT NULL,
--     entity_id UUID NOT NULL,  -- References TB Asset
--     entity_type VARCHAR(50) NOT NULL,
--     recommendation_type VARCHAR(100) NOT NULL,
--     priority VARCHAR(50) NOT NULL,
--     title VARCHAR(500) NOT NULL,
--     description TEXT,
--     expected_benefit JSONB,
--     status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
--     created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
--     reviewed_time BIGINT,
--     reviewed_by UUID,
--     implemented_time BIGINT,
--     implemented_by UUID,
--     feedback TEXT
-- );

-- ============================================================================
-- GRANTS (adjust roles as needed)
-- ============================================================================

-- GRANT USAGE ON SCHEMA pf TO thingsboard_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA pf TO thingsboard_app;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA pf TO thingsboard_app;

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================
