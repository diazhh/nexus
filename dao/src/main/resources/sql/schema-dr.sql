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
-- DRILLING MODULE (DR) SCHEMA
-- Tables for Drilling Operations Management
-- ============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- Table: dr_rigs
-- Description: Drilling Rigs
-- =====================================================
CREATE TABLE IF NOT EXISTS dr_rigs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    rig_code VARCHAR(50) NOT NULL UNIQUE,
    rig_name VARCHAR(255) NOT NULL,
    asset_id UUID NOT NULL UNIQUE,

    -- Child Asset References
    drawworks_asset_id UUID,
    top_drive_asset_id UUID,
    mud_pump_1_asset_id UUID,
    mud_pump_2_asset_id UUID,
    mud_pump_3_asset_id UUID,
    mud_system_asset_id UUID,
    bop_stack_asset_id UUID,
    gas_detector_asset_id UUID,

    -- Rig Type and Status
    rig_type VARCHAR(50),
    operational_status VARCHAR(50) NOT NULL DEFAULT 'STANDBY',

    -- Rig Specifications
    contractor VARCHAR(100),
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    year_built INTEGER,
    max_hookload_lbs INTEGER,
    max_rotary_torque_ft_lbs INTEGER,
    max_depth_capability_ft DECIMAL(10,2),

    -- Current Operation References
    current_well_id UUID,
    current_run_id UUID,

    -- Location
    current_location VARCHAR(255),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),

    -- Cumulative Statistics
    total_wells_drilled INTEGER DEFAULT 0,
    total_footage_drilled_ft DECIMAL(12,2) DEFAULT 0,
    total_npt_hours DECIMAL(10,2) DEFAULT 0,
    total_operational_hours DECIMAL(12,2) DEFAULT 0,

    -- Maintenance and Certification
    last_rig_inspection_date BIGINT,
    next_rig_inspection_due BIGINT,
    bop_test_date BIGINT,
    certification_expiry_date BIGINT,

    -- Metadata
    notes TEXT,
    metadata JSONB,
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,

    CONSTRAINT fk_dr_rigs_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dr_rigs_tenant ON dr_rigs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dr_rigs_code ON dr_rigs(rig_code);
CREATE INDEX IF NOT EXISTS idx_dr_rigs_status ON dr_rigs(operational_status);
CREATE INDEX IF NOT EXISTS idx_dr_rigs_asset ON dr_rigs(asset_id);

-- =====================================================
-- Table: dr_bhas
-- Description: Bottom Hole Assemblies
-- =====================================================
CREATE TABLE IF NOT EXISTS dr_bhas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    bha_number VARCHAR(50) NOT NULL,
    asset_id UUID NOT NULL UNIQUE,

    -- BHA Type and Configuration
    bha_type VARCHAR(50),
    is_directional BOOLEAN DEFAULT FALSE,

    -- Bit Information
    bit_serial VARCHAR(100),
    bit_type VARCHAR(50),
    bit_size_in DECIMAL(5,3),
    bit_iadc_code VARCHAR(20),
    bit_manufacturer VARCHAR(100),
    bit_model VARCHAR(100),
    bit_tfa_sq_in DECIMAL(6,3),
    bit_nozzles VARCHAR(100),

    -- BHA Dimensions
    total_length_ft DECIMAL(8,2),
    total_weight_lbs DECIMAL(10,2),

    -- Motor Information
    motor_manufacturer VARCHAR(100),
    motor_model VARCHAR(100),
    motor_od_in DECIMAL(5,3),
    motor_bend_angle_deg DECIMAL(4,2),
    motor_lobe_configuration VARCHAR(20),

    -- RSS Information
    rss_manufacturer VARCHAR(100),
    rss_model VARCHAR(100),
    rss_type VARCHAR(50),

    -- Status and Tracking
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    total_footage_drilled DECIMAL(10,2) DEFAULT 0,
    total_hours_on_bottom DECIMAL(10,2) DEFAULT 0,
    total_runs INTEGER DEFAULT 0,

    -- Components (JSON detailed list)
    components_json JSONB,

    -- Dull Grading
    bit_dull_inner VARCHAR(10),
    bit_dull_outer VARCHAR(10),
    bit_dull_char VARCHAR(50),
    bit_dull_location VARCHAR(10),
    bit_bearing_condition VARCHAR(10),
    bit_gauge_condition VARCHAR(10),
    bit_reason_pulled VARCHAR(100),

    -- Metadata
    notes TEXT,
    metadata JSONB,
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,

    CONSTRAINT fk_dr_bhas_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dr_bhas_tenant ON dr_bhas(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dr_bhas_number ON dr_bhas(bha_number);
CREATE INDEX IF NOT EXISTS idx_dr_bhas_status ON dr_bhas(status);
CREATE INDEX IF NOT EXISTS idx_dr_bhas_type ON dr_bhas(bha_type);
CREATE INDEX IF NOT EXISTS idx_dr_bhas_bit_size ON dr_bhas(bit_size_in);

-- =====================================================
-- Table: dr_runs
-- Description: Drilling Runs (bit runs)
-- =====================================================
CREATE TABLE IF NOT EXISTS dr_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    run_number VARCHAR(50) NOT NULL,

    -- References
    rig_id UUID NOT NULL,
    well_id UUID NOT NULL,
    bha_id UUID,
    mwd_tool_asset_id UUID,
    lwd_tool_asset_id UUID,

    -- Hole Section Configuration
    hole_section VARCHAR(50),
    hole_size_in DECIMAL(5,3),
    casing_size_in DECIMAL(5,3),
    previous_casing_shoe_md_ft DECIMAL(10,2),

    -- Planned Depths
    planned_start_depth_md_ft DECIMAL(10,2),
    planned_end_depth_md_ft DECIMAL(10,2),
    planned_start_depth_tvd_ft DECIMAL(10,2),
    planned_end_depth_tvd_ft DECIMAL(10,2),

    -- Actual Depths
    start_depth_md_ft DECIMAL(10,2),
    end_depth_md_ft DECIMAL(10,2),
    current_depth_md_ft DECIMAL(10,2),
    start_depth_tvd_ft DECIMAL(10,2),
    end_depth_tvd_ft DECIMAL(10,2),
    current_depth_tvd_ft DECIMAL(10,2),

    -- Mud Properties
    mud_type VARCHAR(50),
    mud_weight_ppg DECIMAL(5,2),
    pore_pressure_ppg DECIMAL(5,2),
    frac_gradient_ppg DECIMAL(5,2),

    -- Dates
    spud_date BIGINT,
    start_date BIGINT,
    end_date BIGINT,

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',

    -- KPIs
    total_footage_ft DECIMAL(10,2) DEFAULT 0,
    avg_rop_ft_hr DECIMAL(8,2),
    max_rop_ft_hr DECIMAL(8,2),
    total_rotating_hours DECIMAL(8,2) DEFAULT 0,
    total_sliding_hours DECIMAL(8,2) DEFAULT 0,
    total_circulating_hours DECIMAL(8,2) DEFAULT 0,
    total_connection_time_hours DECIMAL(8,2) DEFAULT 0,
    total_trip_time_hours DECIMAL(8,2) DEFAULT 0,
    total_npt_hours DECIMAL(8,2) DEFAULT 0,
    drilling_efficiency_percent DECIMAL(5,2),

    -- Connection Statistics
    total_connections INTEGER DEFAULT 0,
    avg_connection_time_min DECIMAL(6,2),

    -- Survey Statistics
    survey_count INTEGER DEFAULT 0,
    max_inclination_deg DECIMAL(6,3),
    max_dls_deg_per_100ft DECIMAL(6,3),

    -- Vibration Statistics
    avg_axial_vibration_g DECIMAL(6,3),
    avg_lateral_vibration_g DECIMAL(6,3),
    max_shock_g DECIMAL(6,3),

    -- End of Run Summary
    reason_ended VARCHAR(255),
    bit_condition_out VARCHAR(100),

    -- Metadata
    notes TEXT,
    metadata JSONB,
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,

    CONSTRAINT fk_dr_runs_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_dr_runs_rig FOREIGN KEY (rig_id)
        REFERENCES dr_rigs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dr_runs_bha FOREIGN KEY (bha_id)
        REFERENCES dr_bhas(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_dr_runs_tenant ON dr_runs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dr_runs_rig ON dr_runs(rig_id);
CREATE INDEX IF NOT EXISTS idx_dr_runs_well ON dr_runs(well_id);
CREATE INDEX IF NOT EXISTS idx_dr_runs_bha ON dr_runs(bha_id);
CREATE INDEX IF NOT EXISTS idx_dr_runs_status ON dr_runs(status);
CREATE INDEX IF NOT EXISTS idx_dr_runs_dates ON dr_runs(start_date, end_date);

-- =====================================================
-- Table: dr_directional_surveys
-- Description: Directional Survey Data
-- =====================================================
CREATE TABLE IF NOT EXISTS dr_directional_surveys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    run_id UUID NOT NULL,

    -- Survey Data
    survey_time BIGINT NOT NULL,
    measured_depth_ft DECIMAL(10,2) NOT NULL,
    inclination_deg DECIMAL(6,3) NOT NULL,
    azimuth_deg DECIMAL(6,3) NOT NULL,

    -- Calculated Values
    tvd_ft DECIMAL(10,2),
    northing_ft DECIMAL(12,2),
    easting_ft DECIMAL(12,2),
    vertical_section_ft DECIMAL(10,2),
    dls_deg_per_100ft DECIMAL(6,3),

    -- Tool Information
    tool_type VARCHAR(50),
    tool_serial VARCHAR(100),

    -- Quality
    magnetic_field_strength DECIMAL(8,2),
    magnetic_dip_angle DECIMAL(6,3),
    gravity_field_strength DECIMAL(8,4),
    total_correction_deg DECIMAL(6,3),

    -- Metadata
    notes TEXT,
    metadata JSONB,
    created_time BIGINT NOT NULL,

    CONSTRAINT fk_dr_surveys_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_dr_surveys_run FOREIGN KEY (run_id)
        REFERENCES dr_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dr_surveys_tenant ON dr_directional_surveys(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dr_surveys_run ON dr_directional_surveys(run_id);
CREATE INDEX IF NOT EXISTS idx_dr_surveys_depth ON dr_directional_surveys(measured_depth_ft);
CREATE INDEX IF NOT EXISTS idx_dr_surveys_time ON dr_directional_surveys(survey_time);

-- =====================================================
-- Table: dr_mud_logs
-- Description: Mud Log Data
-- =====================================================
CREATE TABLE IF NOT EXISTS dr_mud_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    run_id UUID NOT NULL,

    -- Depth and Time
    log_time BIGINT NOT NULL,
    measured_depth_ft DECIMAL(10,2) NOT NULL,

    -- Gas Readings
    total_gas_units DECIMAL(8,2),
    c1_methane_ppm DECIMAL(10,2),
    c2_ethane_ppm DECIMAL(10,2),
    c3_propane_ppm DECIMAL(10,2),
    c4_butane_ppm DECIMAL(10,2),
    c5_pentane_ppm DECIMAL(10,2),
    h2s_ppm DECIMAL(8,2),
    co2_percent DECIMAL(5,2),

    -- Lithology
    lithology VARCHAR(100),
    lithology_description TEXT,
    formation_name VARCHAR(100),

    -- Drilling Parameters at Sample
    rop_ft_hr DECIMAL(8,2),
    wob_klbs DECIMAL(6,2),
    rpm DECIMAL(6,2),
    torque_kft_lbs DECIMAL(8,2),
    flow_rate_gpm DECIMAL(8,2),
    spp_psi DECIMAL(8,2),

    -- Shows
    oil_show VARCHAR(50),
    fluorescence VARCHAR(50),
    cut VARCHAR(50),
    stain VARCHAR(50),

    -- Metadata
    notes TEXT,
    metadata JSONB,
    created_time BIGINT NOT NULL,

    CONSTRAINT fk_dr_mudlogs_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_dr_mudlogs_run FOREIGN KEY (run_id)
        REFERENCES dr_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dr_mudlogs_tenant ON dr_mud_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dr_mudlogs_run ON dr_mud_logs(run_id);
CREATE INDEX IF NOT EXISTS idx_dr_mudlogs_depth ON dr_mud_logs(measured_depth_ft);
CREATE INDEX IF NOT EXISTS idx_dr_mudlogs_time ON dr_mud_logs(log_time);
