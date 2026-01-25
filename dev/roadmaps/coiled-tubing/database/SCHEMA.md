# Esquema de Base de Datos - Módulo Coiled Tubing

## Convenciones de Nomenclatura

### Prefijo del Módulo
Todas las tablas específicas del módulo CT usan el prefijo `ct_`

### Estándares
- Nombres de tablas: `snake_case` con prefijo `ct_`
- Nombres de columnas: `snake_case`
- Primary keys: `id UUID`
- Foreign keys: `{tabla_referenciada}_id`
- Timestamps: `BIGINT` (epoch milliseconds)
- Booleans: `BOOLEAN`
- JSON data: `JSONB`

## Tablas Principales

### 1. ct_units (Unidades de Coiled Tubing)

Almacena información de las unidades CT físicas.

```sql
CREATE TABLE ct_units (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Información básica
    unit_code VARCHAR(50) NOT NULL UNIQUE,
    unit_name VARCHAR(255) NOT NULL,
    asset_id UUID NOT NULL, -- Referencia al asset root en TB
    
    -- Especificaciones técnicas
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    year_manufactured INTEGER,
    
    -- Capacidades
    max_pressure_psi INTEGER,
    max_tension_lbf INTEGER,
    max_speed_ft_min INTEGER,
    max_tubing_od_inch DECIMAL(4,3),
    
    -- Sistemas integrados (referencias a assets hijos)
    hydraulic_system_asset_id UUID,
    injection_system_asset_id UUID,
    control_system_asset_id UUID,
    power_pack_asset_id UUID,
    gooseneck_asset_id UUID,
    
    -- Estado operacional
    operational_status VARCHAR(50) NOT NULL, -- 'OPERATIONAL', 'STANDBY', 'MAINTENANCE', 'OFFLINE', 'DECOMMISSIONED'
    current_location VARCHAR(255),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    
    -- Tracking operacional
    total_operational_hours DECIMAL(10,2) DEFAULT 0,
    total_jobs_completed INTEGER DEFAULT 0,
    total_meters_deployed DECIMAL(12,2) DEFAULT 0,
    
    -- Reel actualmente acoplado
    current_reel_id UUID,
    reel_coupled_date BIGINT,
    
    -- Mantenimiento
    last_maintenance_date BIGINT,
    last_maintenance_hours DECIMAL(10,2),
    next_maintenance_due_hours DECIMAL(10,2),
    maintenance_interval_hours INTEGER DEFAULT 500,
    
    -- Certificaciones
    last_pressure_test_date BIGINT,
    last_pressure_test_psi INTEGER,
    certification_expiry_date BIGINT,
    
    -- Metadata
    notes TEXT,
    metadata JSONB,
    
    -- Auditoría
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,
    
    CONSTRAINT fk_ct_units_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_units_asset FOREIGN KEY (asset_id) 
        REFERENCES asset(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ct_units_current_reel FOREIGN KEY (current_reel_id) 
        REFERENCES ct_reels(id) ON DELETE SET NULL
);

CREATE INDEX idx_ct_units_tenant ON ct_units(tenant_id);
CREATE INDEX idx_ct_units_code ON ct_units(unit_code);
CREATE INDEX idx_ct_units_status ON ct_units(operational_status);
CREATE INDEX idx_ct_units_location ON ct_units(current_location);
CREATE INDEX idx_ct_units_asset ON ct_units(asset_id);
CREATE INDEX idx_ct_units_current_reel ON ct_units(current_reel_id);
```

### 2. ct_reels (Carretes de Tubería)

Almacena información de los reels de tubería continua.

```sql
CREATE TABLE ct_reels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Información básica
    reel_code VARCHAR(50) NOT NULL UNIQUE,
    reel_name VARCHAR(255) NOT NULL,
    asset_id UUID NOT NULL, -- Referencia al asset en TB
    
    -- Especificaciones de tubería
    tubing_od_inch DECIMAL(4,3) NOT NULL,
    tubing_id_inch DECIMAL(4,3) NOT NULL,
    wall_thickness_inch DECIMAL(4,3) NOT NULL,
    total_length_ft DECIMAL(10,2) NOT NULL,
    
    -- Material
    material_grade VARCHAR(50) NOT NULL, -- 'QT-700', 'QT-800', 'QT-900', etc.
    material_yield_strength_psi INTEGER,
    material_tensile_strength_psi INTEGER,
    youngs_modulus_psi BIGINT,
    
    -- Configuración de fatiga
    has_welds BOOLEAN DEFAULT FALSE,
    weld_stress_concentration_factor DECIMAL(3,2) DEFAULT 1.0,
    corrosion_environment VARCHAR(50), -- 'NONE', 'H2S', 'CO2', 'MIXED'
    corrosion_factor DECIMAL(3,2) DEFAULT 1.0,
    
    -- Geometría de operación
    reel_core_diameter_inch DECIMAL(6,2),
    typical_gooseneck_radius_inch DECIMAL(6,2),
    
    -- Estado del reel
    status VARCHAR(50) NOT NULL, -- 'AVAILABLE', 'IN_USE', 'INSPECTION', 'RETIRED', 'DAMAGED'
    current_unit_id UUID, -- NULL si no está acoplado
    current_location VARCHAR(255),
    
    -- Fatiga acumulada
    accumulated_fatigue_percent DECIMAL(5,2) DEFAULT 0.00,
    total_cycles INTEGER DEFAULT 0,
    estimated_remaining_cycles INTEGER,
    fatigue_calculation_method VARCHAR(50) DEFAULT 'PALMGREN_MINER',
    
    -- Tracking operacional
    total_jobs_used INTEGER DEFAULT 0,
    total_meters_deployed DECIMAL(12,2) DEFAULT 0,
    total_hours_in_use DECIMAL(10,2) DEFAULT 0,
    
    -- Inspección
    last_inspection_date BIGINT,
    last_inspection_type VARCHAR(50), -- 'VISUAL', 'MAGNETIC', 'ULTRASONIC', 'PRESSURE_TEST'
    last_inspection_result VARCHAR(50), -- 'PASSED', 'CONDITIONAL', 'FAILED'
    next_inspection_due_date BIGINT,
    
    -- Condición física
    has_corrosion BOOLEAN DEFAULT FALSE,
    has_mechanical_damage BOOLEAN DEFAULT FALSE,
    ovality_percent DECIMAL(4,2), -- Deformación oval
    wall_thickness_loss_percent DECIMAL(4,2),
    
    -- Fechas importantes
    manufacturing_date BIGINT,
    first_use_date BIGINT,
    retirement_date BIGINT,
    
    -- Metadata
    notes TEXT,
    metadata JSONB,
    
    -- Auditoría
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
```

### 3. ct_jobs (Trabajos/Operaciones)

Almacena información de los trabajos de coiled tubing.

```sql
CREATE TABLE ct_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Identificación del trabajo
    job_number VARCHAR(50) NOT NULL UNIQUE,
    job_name VARCHAR(255) NOT NULL,
    job_type VARCHAR(100) NOT NULL, -- 'WELL_CLEANOUT', 'ACID_STIMULATION', 'NITROGEN_LIFTING', 'MILLING', etc.
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    
    -- Información del pozo
    well_id UUID, -- Referencia a asset del pozo si existe
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
    
    -- Estado del trabajo
    status VARCHAR(50) NOT NULL, -- 'PLANNED', 'APPROVED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'CANCELLED', 'FAILED'
    status_reason TEXT,
    current_phase VARCHAR(100), -- 'RIGGING_UP', 'RUNNING_IN', 'ON_DEPTH', 'TREATMENT', 'PULLING_OUT', 'RIGGING_DOWN'
    
    -- Parámetros operacionales planeados
    max_planned_pressure_psi INTEGER,
    max_planned_tension_lbf INTEGER,
    max_planned_speed_ft_min INTEGER,
    planned_pump_rate_bpm DECIMAL(6,2),
    
    -- Parámetros operacionales alcanzados
    max_actual_pressure_psi INTEGER,
    max_actual_tension_lbf INTEGER,
    max_actual_speed_ft_min INTEGER,
    max_actual_depth_ft DECIMAL(10,2),
    
    -- Químicos/Tratamientos
    chemicals_used JSONB, -- Array de {chemical_name, volume, concentration}
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
    
    -- NPT breakdown
    npt_breakdown JSONB, -- {category: 'WEATHER', hours: 2.5}, etc.
    
    -- Costos (opcional)
    estimated_cost_usd DECIMAL(12,2),
    actual_cost_usd DECIMAL(12,2),
    
    -- Incidentes y observaciones
    incidents_count INTEGER DEFAULT 0,
    has_stuck_pipe BOOLEAN DEFAULT FALSE,
    has_hse_incident BOOLEAN DEFAULT FALSE,
    
    -- Reportes generados
    job_report_generated BOOLEAN DEFAULT FALSE,
    job_report_path VARCHAR(500),
    
    -- Metadata
    description TEXT,
    notes TEXT,
    lessons_learned TEXT,
    metadata JSONB,
    
    -- Auditoría
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
```

### 4. ct_job_phases (Fases de Trabajos)

Detalle de las fases de cada trabajo.

```sql
CREATE TABLE ct_job_phases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL,
    
    -- Información de la fase
    phase_number INTEGER NOT NULL,
    phase_name VARCHAR(100) NOT NULL,
    phase_type VARCHAR(50) NOT NULL, -- 'RIGGING_UP', 'RUNNING_IN', 'ON_DEPTH', 'TREATMENT', 'PULLING_OUT', 'RIGGING_DOWN'
    
    -- Timing
    planned_start_time BIGINT,
    planned_duration_minutes INTEGER,
    actual_start_time BIGINT,
    actual_end_time BIGINT,
    actual_duration_minutes INTEGER,
    
    -- Estado
    status VARCHAR(50) NOT NULL, -- 'PLANNED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED'
    
    -- Parámetros de la fase
    start_depth_ft DECIMAL(10,2),
    end_depth_ft DECIMAL(10,2),
    average_speed_ft_min DECIMAL(6,2),
    average_pressure_psi INTEGER,
    average_tension_lbf INTEGER,
    
    -- Descripción y notas
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
```

### 5. ct_job_events (Eventos Durante Trabajos)

Registro de eventos durante la ejecución de trabajos.

```sql
CREATE TABLE ct_job_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL,
    
    -- Timing
    event_time BIGINT NOT NULL,
    event_sequence INTEGER,
    
    -- Tipo de evento
    event_type VARCHAR(50) NOT NULL, -- 'START', 'PHASE_CHANGE', 'ALARM', 'PARAMETER_CHANGE', 'INCIDENT', 'NOTE', 'COMPLETE'
    event_category VARCHAR(50), -- 'OPERATIONAL', 'SAFETY', 'EQUIPMENT', 'WELL', 'OTHER'
    severity VARCHAR(20), -- 'INFO', 'WARNING', 'CRITICAL'
    
    -- Detalles del evento
    event_title VARCHAR(255) NOT NULL,
    event_description TEXT,
    
    -- Contexto operacional en el momento del evento
    depth_at_event_ft DECIMAL(10,2),
    pressure_at_event_psi INTEGER,
    tension_at_event_lbf INTEGER,
    speed_at_event_ft_min DECIMAL(6,2),
    
    -- Usuario que registra
    recorded_by UUID,
    
    -- Acciones tomadas
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
```

### 6. ct_fatigue_log (Log de Cálculos de Fatiga)

Historial de cálculos de fatiga para cada reel.

```sql
CREATE TABLE ct_fatigue_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    reel_id UUID NOT NULL,
    job_id UUID, -- NULL si es cálculo de mantenimiento/inspección
    
    -- Timing
    calculation_time BIGINT NOT NULL,
    
    -- Datos del ciclo
    cycle_number INTEGER,
    cycle_type VARCHAR(50), -- 'DEPLOYMENT', 'RETRIEVAL', 'STATIC', 'MAINTENANCE_TEST'
    
    -- Parámetros del ciclo
    pressure_psi INTEGER,
    tension_lbf INTEGER,
    bend_radius_inch DECIMAL(6,2),
    temperature_f INTEGER,
    
    -- Cálculo de estrés
    hoop_stress_psi INTEGER,
    axial_stress_psi INTEGER,
    bending_stress_psi INTEGER,
    von_mises_stress_psi INTEGER,
    
    -- Fatiga
    cycles_to_failure BIGINT,
    fatigue_increment_percent DECIMAL(8,6),
    fatigue_before_percent DECIMAL(5,2),
    fatigue_after_percent DECIMAL(5,2),
    
    -- Factores aplicados
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
```

### 7. ct_maintenance (Registros de Mantenimiento)

```sql
CREATE TABLE ct_maintenance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Asset relacionado
    unit_id UUID,
    reel_id UUID,
    component_type VARCHAR(50), -- 'UNIT', 'REEL', 'HYDRAULIC_SYSTEM', 'INJECTION_SYSTEM', etc.
    
    -- Tipo de mantenimiento
    maintenance_type VARCHAR(50) NOT NULL, -- 'PREVENTIVE', 'CORRECTIVE', 'PREDICTIVE', 'EMERGENCY'
    maintenance_category VARCHAR(50), -- 'INSPECTION', 'REPAIR', 'REPLACEMENT', 'CALIBRATION', 'OVERHAUL'
    
    -- Planificación
    planned_date BIGINT,
    planned_duration_hours DECIMAL(6,2),
    
    -- Ejecución
    actual_start_date BIGINT,
    actual_end_date BIGINT,
    actual_duration_hours DECIMAL(6,2),
    
    -- Estado
    status VARCHAR(50) NOT NULL, -- 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'OVERDUE'
    
    -- Detalles del trabajo
    work_description TEXT NOT NULL,
    findings TEXT,
    actions_taken TEXT,
    parts_replaced JSONB, -- Array de {part_name, part_number, quantity, cost}
    
    -- Personal
    performed_by UUID,
    approved_by UUID,
    
    -- Métricas
    operational_hours_at_maintenance DECIMAL(10,2),
    next_maintenance_due_hours DECIMAL(10,2),
    
    -- Resultados
    maintenance_successful BOOLEAN,
    requires_follow_up BOOLEAN DEFAULT FALSE,
    follow_up_date BIGINT,
    
    -- Costos
    labor_cost_usd DECIMAL(10,2),
    parts_cost_usd DECIMAL(10,2),
    total_cost_usd DECIMAL(10,2),
    
    -- Documentación
    work_order_number VARCHAR(50),
    attachments JSONB, -- Array de referencias a archivos
    
    -- Metadata
    notes TEXT,
    metadata JSONB,
    
    -- Auditoría
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,
    
    CONSTRAINT fk_ct_maintenance_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_maintenance_unit FOREIGN KEY (unit_id) 
        REFERENCES ct_units(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_maintenance_reel FOREIGN KEY (reel_id) 
        REFERENCES ct_reels(id) ON DELETE CASCADE,
    CONSTRAINT chk_maintenance_asset CHECK (unit_id IS NOT NULL OR reel_id IS NOT NULL)
);

CREATE INDEX idx_ct_maintenance_tenant ON ct_maintenance(tenant_id);
CREATE INDEX idx_ct_maintenance_unit ON ct_maintenance(unit_id);
CREATE INDEX idx_ct_maintenance_reel ON ct_maintenance(reel_id);
CREATE INDEX idx_ct_maintenance_status ON ct_maintenance(status);
CREATE INDEX idx_ct_maintenance_date ON ct_maintenance(planned_date);
```

### 8. ct_inspections (Inspecciones)

```sql
CREATE TABLE ct_inspections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Asset inspeccionado
    reel_id UUID NOT NULL,
    
    -- Tipo de inspección
    inspection_type VARCHAR(50) NOT NULL, -- 'VISUAL', 'MAGNETIC_PARTICLE', 'ULTRASONIC', 'PRESSURE_TEST', 'DIMENSIONAL'
    inspection_standard VARCHAR(100), -- 'API 5ST', 'NACE MR0175', etc.
    
    -- Planificación
    scheduled_date BIGINT,
    
    -- Ejecución
    inspection_date BIGINT NOT NULL,
    inspector_name VARCHAR(255),
    inspector_certification VARCHAR(100),
    
    -- Resultados generales
    overall_result VARCHAR(50) NOT NULL, -- 'PASSED', 'CONDITIONAL', 'FAILED'
    pass_criteria_met BOOLEAN,
    
    -- Mediciones específicas
    wall_thickness_measurements JSONB, -- Array de {location, thickness_inch, min_acceptable}
    ovality_measurements JSONB,
    corrosion_detected BOOLEAN DEFAULT FALSE,
    corrosion_locations JSONB,
    mechanical_damage_detected BOOLEAN DEFAULT FALSE,
    damage_locations JSONB,
    
    -- Prueba de presión (si aplica)
    pressure_test_performed BOOLEAN DEFAULT FALSE,
    test_pressure_psi INTEGER,
    hold_time_minutes INTEGER,
    pressure_test_result VARCHAR(20), -- 'PASSED', 'FAILED'
    
    -- Recomendaciones
    recommendation VARCHAR(50), -- 'RETURN_TO_SERVICE', 'LIMITED_SERVICE', 'REPAIR_REQUIRED', 'RETIRE'
    limitations TEXT,
    required_actions TEXT,
    
    -- Próxima inspección
    next_inspection_due_date BIGINT,
    next_inspection_type VARCHAR(50),
    
    -- Documentación
    inspection_report_path VARCHAR(500),
    attachments JSONB,
    
    -- Metadata
    notes TEXT,
    metadata JSONB,
    
    -- Auditoría
    created_by UUID,
    created_time BIGINT NOT NULL,
    
    CONSTRAINT fk_ct_inspections_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_inspections_reel FOREIGN KEY (reel_id) 
        REFERENCES ct_reels(id) ON DELETE CASCADE
);

CREATE INDEX idx_ct_inspections_tenant ON ct_inspections(tenant_id);
CREATE INDEX idx_ct_inspections_reel ON ct_inspections(reel_id);
CREATE INDEX idx_ct_inspections_date ON ct_inspections(inspection_date);
CREATE INDEX idx_ct_inspections_result ON ct_inspections(overall_result);
```

### 9. ct_bha_configurations (Configuraciones de BHA)

```sql
CREATE TABLE ct_bha_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Identificación
    config_name VARCHAR(255) NOT NULL,
    config_code VARCHAR(50),
    config_type VARCHAR(100), -- 'CLEANOUT', 'MILLING', 'DRILLING', 'CEMENTING', 'STIMULATION'
    
    -- Descripción
    description TEXT,
    application TEXT,
    
    -- Configuración de herramientas (de abajo hacia arriba)
    tools_configuration JSONB NOT NULL,
    /* Ejemplo:
    [
        {"position": 1, "tool_name": "Jetting Tool", "tool_oD_inch": 1.5, "length_ft": 2.5},
        {"position": 2, "tool_name": "Motor", "tool_od_inch": 1.75, "length_ft": 8.0},
        {"position": 3, "tool_name": "Weight", "tool_od_inch": 1.5, "length_ft": 5.0}
    ]
    */
    
    -- Dimensiones totales
    total_length_ft DECIMAL(6,2),
    max_od_inch DECIMAL(4,3),
    total_weight_lbf DECIMAL(8,2),
    
    -- Compatibilidad
    compatible_tubing_sizes JSONB, -- Array de ODs compatibles
    max_recommended_pressure_psi INTEGER,
    
    -- Uso
    is_template BOOLEAN DEFAULT FALSE,
    times_used INTEGER DEFAULT 0,
    
    -- Metadata
    notes TEXT,
    metadata JSONB,
    
    -- Auditoría
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,
    
    CONSTRAINT fk_ct_bha_configs_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_ct_bha_configs_tenant ON ct_bha_configurations(tenant_id);
CREATE INDEX idx_ct_bha_configs_type ON ct_bha_configurations(config_type);
CREATE INDEX idx_ct_bha_configs_template ON ct_bha_configurations(is_template);
```

### 10. ct_personnel_assignments (Asignaciones de Personal)

```sql
CREATE TABLE ct_personnel_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    
    -- Job o Unit
    job_id UUID,
    unit_id UUID,
    
    -- Personal
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'OPERATOR', 'SUPERVISOR', 'ENGINEER', 'HELPER', 'SAFETY_OBSERVER'
    
    -- Período de asignación
    assignment_start_date BIGINT NOT NULL,
    assignment_end_date BIGINT,
    
    -- Estado
    status VARCHAR(50) NOT NULL, -- 'ACTIVE', 'COMPLETED', 'CANCELLED'
    
    -- Horas trabajadas
    hours_worked DECIMAL(6,2),
    
    -- Metadata
    notes TEXT,
    metadata JSONB,
    
    created_time BIGINT NOT NULL,
    
    CONSTRAINT fk_ct_personnel_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_personnel_job FOREIGN KEY (job_id) 
        REFERENCES ct_jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_personnel_unit FOREIGN KEY (unit_id) 
        REFERENCES ct_units(id) ON DELETE CASCADE,
    CONSTRAINT chk_personnel_assignment CHECK (job_id IS NOT NULL OR unit_id IS NOT NULL)
);

CREATE INDEX idx_ct_personnel_tenant ON ct_personnel_assignments(tenant_id);
CREATE INDEX idx_ct_personnel_job ON ct_personnel_assignments(job_id);
CREATE INDEX idx_ct_personnel_unit ON ct_personnel_assignments(unit_id);
CREATE INDEX idx_ct_personnel_user ON ct_personnel_assignments(user_id);
```

## Vistas Útiles

### Vista: Fleet Utilization

```sql
CREATE VIEW vw_ct_fleet_utilization AS
SELECT 
    u.id as unit_id,
    u.unit_code,
    u.unit_name,
    u.operational_status,
    u.current_location,
    u.total_operational_hours,
    u.total_jobs_completed,
    COUNT(j.id) FILTER (WHERE j.status = 'IN_PROGRESS') as active_jobs,
    COUNT(j.id) FILTER (WHERE j.status = 'PLANNED') as planned_jobs,
    COALESCE(SUM(j.actual_duration_hours) FILTER (
        WHERE j.actual_start_date >= EXTRACT(EPOCH FROM (NOW() - INTERVAL '30 days')) * 1000
    ), 0) as hours_last_30_days,
    (u.total_operational_hours - COALESCE(u.last_maintenance_hours, 0)) as hours_since_maintenance,
    u.next_maintenance_due_hours - (u.total_operational_hours - COALESCE(u.last_maintenance_hours, 0)) as hours_to_maintenance
FROM ct_units u
LEFT JOIN ct_jobs j ON j.unit_id = u.id
WHERE u.operational_status != 'DECOMMISSIONED'
GROUP BY u.id;
```

### Vista: Reel Status Summary

```sql
CREATE VIEW vw_ct_reel_status AS
SELECT 
    r.id as reel_id,
    r.reel_code,
    r.reel_name,
    r.tubing_od_inch,
    r.total_length_ft,
    r.material_grade,
    r.status,
    r.current_unit_id,
    u.unit_code as current_unit_code,
    r.accumulated_fatigue_percent,
    r.total_cycles,
    r.estimated_remaining_cycles,
    CASE 
        WHEN r.accumulated_fatigue_percent >= 95 THEN 'CRITICAL'
        WHEN r.accumulated_fatigue_percent >= 80 THEN 'HIGH'
        WHEN r.accumulated_fatigue_percent >= 60 THEN 'MODERATE'
        ELSE 'GOOD'
    END as fatigue_level,
    r.total_jobs_used,
    r.last_inspection_date,
    r.next_inspection_due_date,
    CASE 
        WHEN r.next_inspection_due_date < EXTRACT(EPOCH FROM NOW()) * 1000 THEN TRUE
        ELSE FALSE
    END as inspection_overdue
FROM ct_reels r
LEFT JOIN ct_units u ON u.id = r.current_unit_id
WHERE r.status != 'RETIRED';
```

### Vista: Active Jobs Dashboard

```sql
CREATE VIEW vw_ct_active_jobs AS
SELECT 
    j.id as job_id,
    j.job_number,
    j.job_name,
    j.job_type,
    j.status,
    j.current_phase,
    j.well_name,
    j.client_name,
    u.unit_code,
    u.unit_name,
    r.reel_code,
    j.actual_start_date,
    EXTRACT(EPOCH FROM NOW()) * 1000 - j.actual_start_date as elapsed_time_ms,
    j.estimated_duration_hours,
    j.max_actual_depth_ft,
    j.target_depth_to_ft,
    CASE 
        WHEN j.target_depth_to_ft > 0 THEN 
            (j.max_actual_depth_ft / j.target_depth_to_ft * 100)
        ELSE 0
    END as depth_progress_percent,
    (SELECT COUNT(*) FROM ct_job_events e 
     WHERE e.job_id = j.id AND e.severity = 'CRITICAL') as critical_alarms_count
FROM ct_jobs j
INNER JOIN ct_units u ON u.id = j.unit_id
INNER JOIN ct_reels r ON r.id = j.reel_id
WHERE j.status IN ('IN_PROGRESS', 'PAUSED');
```

## Funciones y Triggers

### Función: Actualizar Fatiga de Reel

```sql
CREATE OR REPLACE FUNCTION fn_update_reel_fatigue()
RETURNS TRIGGER AS $$
BEGIN
    -- Actualizar fatiga acumulada en ct_reels
    UPDATE ct_reels
    SET accumulated_fatigue_percent = NEW.fatigue_after_percent,
        total_cycles = total_cycles + 1,
        updated_time = EXTRACT(EPOCH FROM NOW()) * 1000
    WHERE id = NEW.reel_id;
    
    -- Recalcular ciclos restantes estimados
    UPDATE ct_reels
    SET estimated_remaining_cycles = GREATEST(
        0,
        FLOOR((100 - accumulated_fatigue_percent) / 
              (accumulated_fatigue_percent / NULLIF(total_cycles, 0)))
    )
    WHERE id = NEW.reel_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_reel_fatigue
AFTER INSERT ON ct_fatigue_log
FOR EACH ROW
EXECUTE FUNCTION fn_update_reel_fatigue();
```

### Función: Actualizar Horas Operacionales de Unidad

```sql
CREATE OR REPLACE FUNCTION fn_update_unit_hours()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND NEW.actual_duration_hours IS NOT NULL THEN
        UPDATE ct_units
        SET total_operational_hours = total_operational_hours + NEW.actual_duration_hours,
            total_jobs_completed = total_jobs_completed + 1,
            total_meters_deployed = total_meters_deployed + COALESCE(NEW.meters_deployed, 0),
            updated_time = EXTRACT(EPOCH FROM NOW()) * 1000
        WHERE id = NEW.unit_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_unit_hours
AFTER UPDATE OF status ON ct_jobs
FOR EACH ROW
EXECUTE FUNCTION fn_update_unit_hours();
```

## Scripts de Migración

### Migration V1: Initial Schema

```sql
-- /database/migrations/V1__initial_ct_schema.sql
-- Ejecutar todas las sentencias CREATE TABLE en orden de dependencias
```

### Migration V2: Add Indexes

```sql
-- /database/migrations/V2__add_ct_indexes.sql
-- Todos los CREATE INDEX statements
```

### Migration V3: Add Views and Functions

```sql
-- /database/migrations/V3__add_ct_views_functions.sql
-- Todas las vistas y funciones
```

## Datos Iniciales (Seed Data)

```sql
-- Insertar tipos de trabajos estándar
INSERT INTO ct_job_types (name, description) VALUES
('WELL_CLEANOUT', 'Well cleanout operations'),
('ACID_STIMULATION', 'Acid stimulation treatments'),
('NITROGEN_LIFTING', 'Nitrogen lifting operations'),
('MILLING', 'Milling operations'),
('CEMENT_SQUEEZE', 'Cement squeeze jobs'),
('FISHING', 'Fishing operations'),
('PERFORATION', 'Perforation operations');

-- Insertar configuraciones BHA estándar
-- ...
```

## Consideraciones de Performance

### Particionamiento
Para tablas con alto volumen como `ct_job_events` y `ct_fatigue_log`, considerar particionamiento por fecha:

```sql
CREATE TABLE ct_job_events (
    -- ... columnas ...
) PARTITION BY RANGE (event_time);

CREATE TABLE ct_job_events_2026_01 PARTITION OF ct_job_events
    FOR VALUES FROM (1704067200000) TO (1706745600000);
```

### Archivado de Datos Históricos
Implementar proceso de archivado para jobs completados > 2 años.

### Vacuum y Analyze
Configurar autovacuum para tablas de alta escritura.

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026
