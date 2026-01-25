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

-- Migración inicial del esquema Coiled Tubing
-- Versión: 1.0.0
-- Fecha: Enero 2026

-- Extensión para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- TABLA: ct_units (Unidades de Coiled Tubing)
-- ============================================================================
CREATE TABLE ct_units (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Identificación
    unit_code VARCHAR(50) NOT NULL UNIQUE,
    unit_name VARCHAR(255) NOT NULL,
    asset_id UUID NOT NULL,
    
    -- Información del equipo
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    year_manufactured INTEGER,
    
    -- Especificaciones técnicas
    max_pressure_rating_psi INTEGER,
    max_tension_rating_lbf INTEGER,
    max_speed_rating_ft_min INTEGER,
    power_rating_hp INTEGER,
    
    -- Estado operacional
    operational_status VARCHAR(50) NOT NULL DEFAULT 'STANDBY',
    current_location VARCHAR(255),
    total_operational_hours DECIMAL(10,2) DEFAULT 0,
    total_jobs_completed INTEGER DEFAULT 0,
    
    -- Reel actual
    current_reel_id UUID,
    reel_coupled_date BIGINT,
    
    -- Mantenimiento
    last_maintenance_date BIGINT,
    last_maintenance_hours DECIMAL(10,2),
    next_maintenance_due_hours DECIMAL(10,2),
    
    -- Metadata
    description TEXT,
    notes TEXT,
    metadata JSONB,
    
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    
    CONSTRAINT fk_ct_units_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_ct_units_tenant ON ct_units(tenant_id);
CREATE INDEX idx_ct_units_status ON ct_units(operational_status);
CREATE INDEX idx_ct_units_location ON ct_units(current_location);
CREATE INDEX idx_ct_units_reel ON ct_units(current_reel_id);

-- ============================================================================
-- TABLA: ct_reels (Reels de Tubería)
-- ============================================================================
CREATE TABLE ct_reels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Identificación
    reel_code VARCHAR(50) NOT NULL UNIQUE,
    reel_name VARCHAR(255) NOT NULL,
    asset_id UUID NOT NULL,
    
    -- Información del reel
    manufacturer VARCHAR(100),
    serial_number VARCHAR(100),
    year_manufactured INTEGER,
    
    -- Especificaciones de tubería
    tubing_od_inch DECIMAL(6,3),
    tubing_id_inch DECIMAL(6,3),
    tubing_wall_thickness_inch DECIMAL(6,4),
    material_grade VARCHAR(50),
    material_type VARCHAR(50),
    total_length_ft DECIMAL(10,2),
    
    -- Geometría del reel
    reel_core_diameter_inch DECIMAL(8,2),
    reel_flange_diameter_inch DECIMAL(8,2),
    typical_gooseneck_radius_inch DECIMAL(8,2),
    
    -- Estado
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    current_unit_id UUID,
    current_location VARCHAR(255),
    
    -- Fatiga y uso
    accumulated_fatigue_percent DECIMAL(6,3) DEFAULT 0,
    total_cycles INTEGER DEFAULT 0,
    total_operational_hours DECIMAL(10,2) DEFAULT 0,
    total_jobs_completed INTEGER DEFAULT 0,
    estimated_remaining_cycles INTEGER,
    
    -- Factores de corrección
    corrosion_environment VARCHAR(50) DEFAULT 'SWEET',
    weld_stress_concentration_factor DECIMAL(4,3) DEFAULT 1.0,
    
    -- Inspección
    last_inspection_date BIGINT,
    last_inspection_result VARCHAR(50),
    next_inspection_due_date BIGINT,
    
    -- Metadata
    description TEXT,
    notes TEXT,
    metadata JSONB,
    
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    
    CONSTRAINT fk_ct_reels_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_ct_reels_tenant ON ct_reels(tenant_id);
CREATE INDEX idx_ct_reels_status ON ct_reels(status);
CREATE INDEX idx_ct_reels_unit ON ct_reels(current_unit_id);
CREATE INDEX idx_ct_reels_fatigue ON ct_reels(accumulated_fatigue_percent);

-- ============================================================================
-- TABLA: ct_jobs (Trabajos/Operaciones)
-- ============================================================================
CREATE TABLE ct_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Identificación
    job_number VARCHAR(50) NOT NULL UNIQUE,
    job_name VARCHAR(255) NOT NULL,
    job_type VARCHAR(100) NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    
    -- Información del pozo
    well_id UUID,
    well_name VARCHAR(255) NOT NULL,
    field_name VARCHAR(255),
    client_name VARCHAR(255),
    
    -- Datos del pozo
    well_depth_md_ft DECIMAL(10,2),
    well_depth_tvd_ft DECIMAL(10,2),
    target_depth_from_ft DECIMAL(10,2),
    target_depth_to_ft DECIMAL(10,2),
    wellhead_pressure_psi INTEGER,
    
    -- Recursos asignados
    unit_id UUID NOT NULL,
    reel_id UUID NOT NULL,
    bha_configuration_id UUID,
    
    -- Personal
    operator_user_id UUID,
    supervisor_user_id UUID,
    engineer_user_id UUID,
    
    -- Planificación
    planned_start_date BIGINT,
    planned_end_date BIGINT,
    estimated_duration_hours DECIMAL(6,2),
    
    -- Ejecución
    actual_start_date BIGINT,
    actual_end_date BIGINT,
    actual_duration_hours DECIMAL(6,2),
    
    -- Estado
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    status_reason TEXT,
    current_phase VARCHAR(100),
    
    -- Parámetros planeados
    max_planned_pressure_psi INTEGER,
    max_planned_tension_lbf INTEGER,
    max_planned_speed_ft_min INTEGER,
    planned_pump_rate_bpm DECIMAL(6,2),
    
    -- Parámetros alcanzados
    max_actual_pressure_psi INTEGER,
    max_actual_tension_lbf INTEGER,
    max_actual_speed_ft_min INTEGER,
    max_actual_depth_ft DECIMAL(10,2),
    
    -- Químicos/Tratamientos
    chemicals_used JSONB,
    total_fluid_pumped_bbl DECIMAL(10,2),
    nitrogen_volume_scf DECIMAL(12,2),
    
    -- Resultados
    job_success BOOLEAN,
    objectives_achieved BOOLEAN,
    meters_deployed DECIMAL(10,2),
    cycles_performed INTEGER,
    
    -- Tiempos
    productive_time_hours DECIMAL(6,2),
    non_productive_time_hours DECIMAL(6,2),
    rigging_time_hours DECIMAL(6,2),
    npt_breakdown JSONB,
    
    -- Costos
    estimated_cost_usd DECIMAL(12,2),
    actual_cost_usd DECIMAL(12,2),
    
    -- Incidentes
    incidents_count INTEGER DEFAULT 0,
    has_stuck_pipe BOOLEAN DEFAULT FALSE,
    has_hse_incident BOOLEAN DEFAULT FALSE,
    
    -- Reportes
    job_report_generated BOOLEAN DEFAULT FALSE,
    job_report_path VARCHAR(500),
    
    -- Metadata
    description TEXT,
    notes TEXT,
    metadata JSONB,
    
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    
    CONSTRAINT fk_ct_jobs_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_jobs_unit FOREIGN KEY (unit_id) 
        REFERENCES ct_units(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ct_jobs_reel FOREIGN KEY (reel_id) 
        REFERENCES ct_reels(id) ON DELETE RESTRICT
);

CREATE INDEX idx_ct_jobs_tenant ON ct_jobs(tenant_id);
CREATE INDEX idx_ct_jobs_status ON ct_jobs(status);
CREATE INDEX idx_ct_jobs_unit ON ct_jobs(unit_id);
CREATE INDEX idx_ct_jobs_reel ON ct_jobs(reel_id);
CREATE INDEX idx_ct_jobs_well ON ct_jobs(well_name);
CREATE INDEX idx_ct_jobs_dates ON ct_jobs(planned_start_date, planned_end_date);

-- ============================================================================
-- TABLA: ct_fatigue_log (Log de Cálculos de Fatiga)
-- ============================================================================
CREATE TABLE ct_fatigue_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    reel_id UUID NOT NULL,
    job_id UUID,
    
    -- Timing
    timestamp BIGINT NOT NULL,
    cycle_number INTEGER,
    
    -- Parámetros operacionales
    pressure_psi DECIMAL(10,2),
    tension_lbf DECIMAL(10,2),
    bend_radius_in DECIMAL(8,2),
    temperature_f DECIMAL(6,2),
    
    -- Esfuerzos calculados
    hoop_stress_psi DECIMAL(12,2),
    axial_stress_psi DECIMAL(12,2),
    bending_stress_psi DECIMAL(12,2),
    von_mises_stress_psi DECIMAL(12,2),
    
    -- Cálculo de fatiga
    cycles_to_failure BIGINT,
    fatigue_increment DECIMAL(12,10),
    accumulated_fatigue_percent DECIMAL(6,3),
    
    -- Factores de corrección
    corrosion_factor DECIMAL(4,3),
    weld_factor DECIMAL(4,3),
    temperature_factor DECIMAL(4,3),
    
    -- Metadata
    calculation_method VARCHAR(50),
    notes TEXT,
    
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
CREATE INDEX idx_ct_fatigue_log_time ON ct_fatigue_log(timestamp);
CREATE INDEX idx_ct_fatigue_log_reel_time ON ct_fatigue_log(reel_id, timestamp DESC);

-- ============================================================================
-- COMENTARIOS
-- ============================================================================
COMMENT ON TABLE ct_units IS 'Unidades de Coiled Tubing (equipos completos)';
COMMENT ON TABLE ct_reels IS 'Reels de tubería continua';
COMMENT ON TABLE ct_jobs IS 'Trabajos/operaciones de coiled tubing';
COMMENT ON TABLE ct_fatigue_log IS 'Historial de cálculos de fatiga';

COMMENT ON COLUMN ct_units.operational_status IS 'STANDBY, RIGGING_UP, OPERATIONAL, MAINTENANCE, OUT_OF_SERVICE';
COMMENT ON COLUMN ct_reels.status IS 'AVAILABLE, IN_USE, MAINTENANCE, INSPECTION, RETIRED';
COMMENT ON COLUMN ct_jobs.status IS 'PLANNED, APPROVED, IN_PROGRESS, PAUSED, COMPLETED, CANCELLED, FAILED';
COMMENT ON COLUMN ct_jobs.job_type IS 'WELL_CLEANOUT, ACID_STIMULATION, NITROGEN_LIFTING, MILLING, FISHING, LOGGING';
