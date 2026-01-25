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

-- Coiled Tubing Module - Initial Schema
-- Version: 1.0.0

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- Table: ct_units
-- Description: Coiled Tubing units (physical equipment)
-- =====================================================
CREATE TABLE ct_units (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Basic information
    unit_code VARCHAR(50) NOT NULL UNIQUE,
    unit_name VARCHAR(255) NOT NULL,
    asset_id UUID NOT NULL,
    
    -- Technical specifications
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    year_manufactured INTEGER,
    
    -- Capabilities
    max_pressure_psi INTEGER,
    max_tension_lbf INTEGER,
    max_speed_ft_min INTEGER,
    max_tubing_od_inch DECIMAL(4,3),
    
    -- Integrated systems (references to child assets)
    hydraulic_system_asset_id UUID,
    injection_system_asset_id UUID,
    control_system_asset_id UUID,
    power_pack_asset_id UUID,
    gooseneck_asset_id UUID,
    
    -- Operational status
    operational_status VARCHAR(50) NOT NULL DEFAULT 'STANDBY',
    current_location VARCHAR(255),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    
    -- Operational tracking
    total_operational_hours DECIMAL(10,2) DEFAULT 0,
    total_jobs_completed INTEGER DEFAULT 0,
    total_meters_deployed DECIMAL(12,2) DEFAULT 0,
    
    -- Currently coupled reel
    current_reel_id UUID,
    reel_coupled_date BIGINT,
    
    -- Maintenance
    last_maintenance_date BIGINT,
    last_maintenance_hours DECIMAL(10,2),
    next_maintenance_due_hours DECIMAL(10,2),
    maintenance_interval_hours INTEGER DEFAULT 500,
    
    -- Certifications
    last_pressure_test_date BIGINT,
    last_pressure_test_psi INTEGER,
    certification_expiry_date BIGINT,
    
    -- Metadata
    notes TEXT,
    metadata JSONB,
    
    -- Audit
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,
    
    CONSTRAINT fk_ct_units_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_units_asset FOREIGN KEY (asset_id) 
        REFERENCES asset(id) ON DELETE RESTRICT
);

CREATE INDEX idx_ct_units_tenant ON ct_units(tenant_id);
CREATE INDEX idx_ct_units_code ON ct_units(unit_code);
CREATE INDEX idx_ct_units_status ON ct_units(operational_status);
CREATE INDEX idx_ct_units_location ON ct_units(current_location);
CREATE INDEX idx_ct_units_asset ON ct_units(asset_id);
CREATE INDEX idx_ct_units_current_reel ON ct_units(current_reel_id);

-- =====================================================
-- Table: ct_reels
-- Description: Coiled tubing reels
-- =====================================================
CREATE TABLE ct_reels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Basic information
    reel_code VARCHAR(50) NOT NULL UNIQUE,
    reel_name VARCHAR(255) NOT NULL,
    asset_id UUID NOT NULL,
    
    -- Tubing specifications
    tubing_od_inch DECIMAL(4,3) NOT NULL,
    tubing_id_inch DECIMAL(4,3) NOT NULL,
    wall_thickness_inch DECIMAL(4,3) NOT NULL,
    total_length_ft DECIMAL(10,2) NOT NULL,
    
    -- Material
    material_grade VARCHAR(50) NOT NULL,
    material_yield_strength_psi INTEGER,
    material_tensile_strength_psi INTEGER,
    youngs_modulus_psi BIGINT,
    
    -- Fatigue configuration
    has_welds BOOLEAN DEFAULT FALSE,
    weld_stress_concentration_factor DECIMAL(3,2) DEFAULT 1.0,
    corrosion_environment VARCHAR(50) DEFAULT 'NONE',
    corrosion_factor DECIMAL(3,2) DEFAULT 1.0,
    
    -- Operation geometry
    reel_core_diameter_inch DECIMAL(6,2),
    typical_gooseneck_radius_inch DECIMAL(6,2),
    
    -- Reel status
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    current_unit_id UUID,
    current_location VARCHAR(255),
    
    -- Accumulated fatigue
    accumulated_fatigue_percent DECIMAL(5,2) DEFAULT 0.00,
    total_cycles INTEGER DEFAULT 0,
    estimated_remaining_cycles INTEGER,
    fatigue_calculation_method VARCHAR(50) DEFAULT 'PALMGREN_MINER',
    
    -- Operational tracking
    total_jobs_used INTEGER DEFAULT 0,
    total_meters_deployed DECIMAL(12,2) DEFAULT 0,
    total_hours_in_use DECIMAL(10,2) DEFAULT 0,
    
    -- Inspection
    last_inspection_date BIGINT,
    last_inspection_type VARCHAR(50),
    last_inspection_result VARCHAR(50),
    next_inspection_due_date BIGINT,
    
    -- Physical condition
    has_corrosion BOOLEAN DEFAULT FALSE,
    has_mechanical_damage BOOLEAN DEFAULT FALSE,
    ovality_percent DECIMAL(4,2),
    wall_thickness_loss_percent DECIMAL(4,2),
    
    -- Important dates
    manufacturing_date BIGINT,
    first_use_date BIGINT,
    retirement_date BIGINT,
    
    -- Metadata
    notes TEXT,
    metadata JSONB,
    
    -- Audit
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,
    
    CONSTRAINT fk_ct_reels_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_reels_asset FOREIGN KEY (asset_id) 
        REFERENCES asset(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ct_reels_current_unit FOREIGN KEY (current_unit_id) 
        REFERENCES ct_units(id) ON DELETE SET NULL
);

CREATE INDEX idx_ct_reels_tenant ON ct_reels(tenant_id);
CREATE INDEX idx_ct_reels_code ON ct_reels(reel_code);
CREATE INDEX idx_ct_reels_status ON ct_reels(status);
CREATE INDEX idx_ct_reels_asset ON ct_reels(asset_id);
CREATE INDEX idx_ct_reels_current_unit ON ct_reels(current_unit_id);
CREATE INDEX idx_ct_reels_fatigue ON ct_reels(accumulated_fatigue_percent);
CREATE INDEX idx_ct_reels_size ON ct_reels(tubing_od_inch);

-- =====================================================
-- Table: ct_jobs
-- Description: Coiled tubing jobs/operations
-- =====================================================
CREATE TABLE ct_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Job identification
    job_number VARCHAR(50) NOT NULL UNIQUE,
    job_name VARCHAR(255) NOT NULL,
    job_type VARCHAR(100) NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    
    -- Well information
    well_id UUID,
    well_name VARCHAR(255) NOT NULL,
    field_name VARCHAR(255),
    client_name VARCHAR(255),
    
    -- Well data
    well_depth_md_ft DECIMAL(10,2),
    well_depth_tvd_ft DECIMAL(10,2),
    target_depth_from_ft DECIMAL(10,2),
    target_depth_to_ft DECIMAL(10,2),
    wellhead_pressure_psi INTEGER,
    
    -- Assigned resources
    unit_id UUID NOT NULL,
    reel_id UUID NOT NULL,
    bha_configuration_id UUID,
    
    -- Personnel
    operator_user_id UUID,
    supervisor_user_id UUID,
    engineer_user_id UUID,
    
    -- Planning
    planned_start_date BIGINT,
    planned_end_date BIGINT,
    estimated_duration_hours DECIMAL(6,2),
    
    -- Execution
    actual_start_date BIGINT,
    actual_end_date BIGINT,
    actual_duration_hours DECIMAL(6,2),
    
    -- Job status
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    status_reason TEXT,
    current_phase VARCHAR(100),
    
    -- Planned operational parameters
    max_planned_pressure_psi INTEGER,
    max_planned_tension_lbf INTEGER,
    max_planned_speed_ft_min INTEGER,
    planned_pump_rate_bpm DECIMAL(6,2),
    
    -- Actual operational parameters
    max_actual_pressure_psi INTEGER,
    max_actual_tension_lbf INTEGER,
    max_actual_speed_ft_min INTEGER,
    max_actual_depth_ft DECIMAL(10,2),
    
    -- Chemicals/Treatments
    chemicals_used JSONB,
    total_fluid_pumped_bbl DECIMAL(10,2),
    nitrogen_volume_scf DECIMAL(12,2),
    
    -- Results
    job_success BOOLEAN,
    objectives_achieved BOOLEAN,
    meters_deployed DECIMAL(10,2),
    cycles_performed INTEGER,
    
    -- Times
    productive_time_hours DECIMAL(6,2),
    non_productive_time_hours DECIMAL(6,2),
    rigging_time_hours DECIMAL(6,2),
    
    -- NPT breakdown
    npt_breakdown JSONB,
    
    -- Costs
    estimated_cost_usd DECIMAL(12,2),
    actual_cost_usd DECIMAL(12,2),
    
    -- Incidents and observations
    incidents_count INTEGER DEFAULT 0,
    has_stuck_pipe BOOLEAN DEFAULT FALSE,
    has_hse_incident BOOLEAN DEFAULT FALSE,
    
    -- Generated reports
    job_report_generated BOOLEAN DEFAULT FALSE,
    job_report_path VARCHAR(500),
    
    -- Metadata
    description TEXT,
    notes TEXT,
    lessons_learned TEXT,
    metadata JSONB,
    
    -- Audit
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,
    approved_by UUID,
    approved_time BIGINT,
    
    CONSTRAINT fk_ct_jobs_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_jobs_unit FOREIGN KEY (unit_id) 
        REFERENCES ct_units(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ct_jobs_reel FOREIGN KEY (reel_id) 
        REFERENCES ct_reels(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ct_jobs_well FOREIGN KEY (well_id) 
        REFERENCES asset(id) ON DELETE SET NULL
);

CREATE INDEX idx_ct_jobs_tenant ON ct_jobs(tenant_id);
CREATE INDEX idx_ct_jobs_number ON ct_jobs(job_number);
CREATE INDEX idx_ct_jobs_status ON ct_jobs(status);
CREATE INDEX idx_ct_jobs_unit ON ct_jobs(unit_id);
CREATE INDEX idx_ct_jobs_reel ON ct_jobs(reel_id);
CREATE INDEX idx_ct_jobs_dates ON ct_jobs(actual_start_date, actual_end_date);
CREATE INDEX idx_ct_jobs_type ON ct_jobs(job_type);
CREATE INDEX idx_ct_jobs_well ON ct_jobs(well_name);

-- =====================================================
-- Table: ct_job_phases
-- Description: Phases of each job
-- =====================================================
CREATE TABLE ct_job_phases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL,
    
    -- Phase information
    phase_number INTEGER NOT NULL,
    phase_name VARCHAR(100) NOT NULL,
    phase_type VARCHAR(50) NOT NULL,
    
    -- Timing
    planned_start_time BIGINT,
    planned_duration_minutes INTEGER,
    actual_start_time BIGINT,
    actual_end_time BIGINT,
    actual_duration_minutes INTEGER,
    
    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    
    -- Phase parameters
    start_depth_ft DECIMAL(10,2),
    end_depth_ft DECIMAL(10,2),
    average_speed_ft_min DECIMAL(6,2),
    average_pressure_psi INTEGER,
    average_tension_lbf INTEGER,
    
    -- Description and notes
    description TEXT,
    notes TEXT,
    
    -- Metadata
    metadata JSONB,
    
    created_time BIGINT NOT NULL,
    
    CONSTRAINT fk_ct_job_phases_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_job_phases_job FOREIGN KEY (job_id) 
        REFERENCES ct_jobs(id) ON DELETE CASCADE,
    CONSTRAINT uk_phase_number UNIQUE (job_id, phase_number)
);

CREATE INDEX idx_ct_job_phases_tenant ON ct_job_phases(tenant_id);
CREATE INDEX idx_ct_job_phases_job ON ct_job_phases(job_id);
CREATE INDEX idx_ct_job_phases_type ON ct_job_phases(phase_type);

-- =====================================================
-- Table: ct_job_events
-- Description: Events during job execution
-- =====================================================
CREATE TABLE ct_job_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL,
    
    -- Timing
    event_time BIGINT NOT NULL,
    event_sequence INTEGER,
    
    -- Event type
    event_type VARCHAR(50) NOT NULL,
    event_category VARCHAR(50),
    severity VARCHAR(20),
    
    -- Event details
    event_title VARCHAR(255) NOT NULL,
    event_description TEXT,
    
    -- Operational context at event
    depth_at_event_ft DECIMAL(10,2),
    pressure_at_event_psi INTEGER,
    tension_at_event_lbf INTEGER,
    speed_at_event_ft_min DECIMAL(6,2),
    
    -- Recording user
    recorded_by UUID,
    
    -- Actions taken
    action_taken TEXT,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_time BIGINT,
    
    -- Metadata
    metadata JSONB,
    
    created_time BIGINT NOT NULL,
    
    CONSTRAINT fk_ct_job_events_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_job_events_job FOREIGN KEY (job_id) 
        REFERENCES ct_jobs(id) ON DELETE CASCADE
);

CREATE INDEX idx_ct_job_events_tenant ON ct_job_events(tenant_id);
CREATE INDEX idx_ct_job_events_job ON ct_job_events(job_id);
CREATE INDEX idx_ct_job_events_time ON ct_job_events(event_time);
CREATE INDEX idx_ct_job_events_type ON ct_job_events(event_type);
CREATE INDEX idx_ct_job_events_severity ON ct_job_events(severity);

-- =====================================================
-- Table: ct_fatigue_log
-- Description: Fatigue calculation history
-- =====================================================
CREATE TABLE ct_fatigue_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    reel_id UUID NOT NULL,
    job_id UUID,
    
    -- Timing
    calculation_time BIGINT NOT NULL,
    
    -- Cycle data
    cycle_number INTEGER,
    cycle_type VARCHAR(50),
    
    -- Cycle parameters
    pressure_psi INTEGER,
    tension_lbf INTEGER,
    bend_radius_inch DECIMAL(6,2),
    temperature_f INTEGER,
    
    -- Stress calculation
    hoop_stress_psi INTEGER,
    axial_stress_psi INTEGER,
    bending_stress_psi INTEGER,
    von_mises_stress_psi INTEGER,
    
    -- Fatigue
    cycles_to_failure BIGINT,
    fatigue_increment_percent DECIMAL(8,6),
    fatigue_before_percent DECIMAL(5,2),
    fatigue_after_percent DECIMAL(5,2),
    
    -- Applied factors
    corrosion_factor DECIMAL(3,2),
    weld_factor DECIMAL(3,2),
    temperature_factor DECIMAL(3,2),
    
    -- Metadata
    calculation_method VARCHAR(50) DEFAULT 'PALMGREN_MINER',
    notes TEXT,
    metadata JSONB,
    
    created_time BIGINT NOT NULL,
    
    CONSTRAINT fk_ct_fatigue_log_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_fatigue_log_reel FOREIGN KEY (reel_id) 
        REFERENCES ct_reels(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_fatigue_log_job FOREIGN KEY (job_id) 
        REFERENCES ct_jobs(id) ON DELETE SET NULL
);

CREATE INDEX idx_ct_fatigue_log_tenant ON ct_fatigue_log(tenant_id);
CREATE INDEX idx_ct_fatigue_log_reel ON ct_fatigue_log(reel_id);
CREATE INDEX idx_ct_fatigue_log_job ON ct_fatigue_log(job_id);
CREATE INDEX idx_ct_fatigue_log_time ON ct_fatigue_log(calculation_time);

-- Add foreign key from ct_units to ct_reels (deferred)
ALTER TABLE ct_units 
ADD CONSTRAINT fk_ct_units_current_reel 
FOREIGN KEY (current_reel_id) 
REFERENCES ct_reels(id) ON DELETE SET NULL;
