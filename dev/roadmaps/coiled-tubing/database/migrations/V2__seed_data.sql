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

-- Datos de ejemplo para el módulo Coiled Tubing
-- Versión: 1.0.0
-- Fecha: Enero 2026

-- ============================================================================
-- UNIDADES CT DE EJEMPLO
-- ============================================================================

-- Unidad 1: CT-UNIT-001 (Operacional)
INSERT INTO ct_units (
    id, tenant_id, unit_code, unit_name, asset_id,
    manufacturer, model, serial_number, year_manufactured,
    max_pressure_rating_psi, max_tension_rating_lbf, max_speed_rating_ft_min,
    operational_status, current_location, total_operational_hours,
    created_time
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    '13814000-1dd2-11b2-8080-808080808080', -- Tenant por defecto
    'CT-UNIT-001',
    'Coiled Tubing Unit Alpha',
    '22222222-2222-2222-2222-222222222222', -- Asset ID (debe existir en TB)
    'Halliburton',
    'Cobra 2000',
    'HAL-CT-2019-001',
    2019,
    35000, 80000, 100,
    'STANDBY',
    'Vaca Muerta, Argentina',
    2450.5,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Unidad 2: CT-UNIT-002 (En operación)
INSERT INTO ct_units (
    id, tenant_id, unit_code, unit_name, asset_id,
    manufacturer, model, operational_status, current_location,
    total_operational_hours, total_jobs_completed,
    created_time
) VALUES (
    '11111111-1111-1111-1111-111111111112',
    '13814000-1dd2-11b2-8080-808080808080',
    'CT-UNIT-002',
    'Coiled Tubing Unit Beta',
    '22222222-2222-2222-2222-222222222223',
    'Schlumberger',
    'FlexRig 500',
    'OPERATIONAL',
    'Permian Basin, Texas',
    3120.0,
    87,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- ============================================================================
-- REELS DE EJEMPLO
-- ============================================================================

-- Reel 1: Alta fatiga (necesita reemplazo pronto)
INSERT INTO ct_reels (
    id, tenant_id, reel_code, reel_name, asset_id,
    tubing_od_inch, tubing_id_inch, material_grade,
    total_length_ft, reel_core_diameter_inch, typical_gooseneck_radius_inch,
    status, accumulated_fatigue_percent, total_cycles,
    corrosion_environment, weld_stress_concentration_factor,
    created_time
) VALUES (
    '33333333-3333-3333-3333-333333333331',
    '13814000-1dd2-11b2-8080-808080808080',
    'REEL-001',
    'Reel Alpha - 2.375" QT-800',
    '44444444-4444-4444-4444-444444444441',
    2.375, 1.995, 'QT-800',
    18000.0, 96.0, 72.0,
    'AVAILABLE',
    82.5, 1250,
    'MILDLY_SOUR', 1.1,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Reel 2: Baja fatiga (nuevo)
INSERT INTO ct_reels (
    id, tenant_id, reel_code, reel_name, asset_id,
    tubing_od_inch, tubing_id_inch, material_grade,
    total_length_ft, status, accumulated_fatigue_percent, total_cycles,
    corrosion_environment,
    created_time
) VALUES (
    '33333333-3333-3333-3333-333333333332',
    '13814000-1dd2-11b2-8080-808080808080',
    'REEL-002',
    'Reel Beta - 2.875" QT-900',
    '44444444-4444-4444-4444-444444444442',
    2.875, 2.441, 'QT-900',
    20000.0, 'AVAILABLE', 15.3, 180,
    'SWEET',
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Reel 3: En uso
INSERT INTO ct_reels (
    id, tenant_id, reel_code, reel_name, asset_id,
    tubing_od_inch, tubing_id_inch, material_grade,
    total_length_ft, status, current_unit_id,
    accumulated_fatigue_percent, total_cycles,
    created_time
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    '13814000-1dd2-11b2-8080-808080808080',
    'REEL-003',
    'Reel Gamma - 2.375" QT-1000',
    '44444444-4444-4444-4444-444444444443',
    2.375, 1.995, 'QT-1000',
    19500.0, 'IN_USE', '11111111-1111-1111-1111-111111111112',
    45.8, 520,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- ============================================================================
-- TRABAJOS DE EJEMPLO
-- ============================================================================

-- Job 1: Completado exitosamente
INSERT INTO ct_jobs (
    id, tenant_id, job_number, job_name, job_type,
    well_name, field_name, client_name,
    well_depth_md_ft, target_depth_to_ft,
    unit_id, reel_id,
    planned_start_date, actual_start_date, actual_end_date,
    status, job_success, objectives_achieved,
    max_actual_pressure_psi, max_actual_tension_lbf,
    max_actual_depth_ft, cycles_performed,
    productive_time_hours, total_jobs_completed,
    created_time
) VALUES (
    '55555555-5555-5555-5555-555555555551',
    '13814000-1dd2-11b2-8080-808080808080',
    'JOB-2026-001',
    'Well Cleanout - Vaca Muerta VM-123',
    'WELL_CLEANOUT',
    'VM-123', 'Vaca Muerta', 'YPF',
    12500.0, 12000.0,
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-333333333331',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '7 days')) * 1000,
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '7 days')) * 1000,
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '6 days')) * 1000,
    'COMPLETED', TRUE, TRUE,
    18500, 45000, 12000.0, 2,
    18.5, 1,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Job 2: En progreso
INSERT INTO ct_jobs (
    id, tenant_id, job_number, job_name, job_type,
    well_name, field_name, client_name,
    well_depth_md_ft, target_depth_to_ft,
    unit_id, reel_id,
    planned_start_date, actual_start_date,
    status, current_phase,
    max_actual_pressure_psi, max_actual_depth_ft,
    created_time
) VALUES (
    '55555555-5555-5555-5555-555555555552',
    '13814000-1dd2-11b2-8080-808080808080',
    'JOB-2026-002',
    'Acid Stimulation - Permian PB-456',
    'ACID_STIMULATION',
    'PB-456', 'Permian Basin', 'ExxonMobil',
    15800.0, 15500.0,
    '11111111-1111-1111-1111-111111111112',
    '33333333-3333-3333-3333-333333333333',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '2 days')) * 1000,
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '2 days')) * 1000,
    'IN_PROGRESS', 'ON_DEPTH',
    22000, 15500.0,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Job 3: Planificado
INSERT INTO ct_jobs (
    id, tenant_id, job_number, job_name, job_type,
    well_name, field_name,
    well_depth_md_ft, target_depth_to_ft,
    unit_id, reel_id,
    planned_start_date, estimated_duration_hours,
    status, priority,
    max_planned_pressure_psi, max_planned_tension_lbf,
    created_time
) VALUES (
    '55555555-5555-5555-5555-555555555553',
    '13814000-1dd2-11b2-8080-808080808080',
    'JOB-2026-003',
    'Nitrogen Lifting - VM-789',
    'NITROGEN_LIFTING',
    'VM-789', 'Vaca Muerta',
    10500.0, 10000.0,
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-333333333332',
    EXTRACT(EPOCH FROM (NOW() + INTERVAL '3 days')) * 1000,
    12.0,
    'PLANNED', 'HIGH',
    15000, 35000,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- ============================================================================
-- LOGS DE FATIGA DE EJEMPLO
-- ============================================================================

-- Algunos cálculos de fatiga para REEL-001
INSERT INTO ct_fatigue_log (
    id, tenant_id, reel_id, job_id,
    timestamp, cycle_number,
    pressure_psi, tension_lbf, bend_radius_in, temperature_f,
    hoop_stress_psi, axial_stress_psi, bending_stress_psi, von_mises_stress_psi,
    cycles_to_failure, fatigue_increment, accumulated_fatigue_percent,
    corrosion_factor, weld_factor, temperature_factor,
    calculation_method,
    created_time
) VALUES 
(
    '66666666-6666-6666-6666-666666666661',
    '13814000-1dd2-11b2-8080-808080808080',
    '33333333-3333-3333-3333-333333333331',
    '55555555-5555-5555-5555-555555555551',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '7 days')) * 1000,
    1248,
    18500.0, 45000.0, 72.0, 85.0,
    32450.0, 28900.0, 15600.0, 48750.0,
    125000, 0.000008, 82.45,
    1.2, 1.1, 1.015,
    'PALMGREN_MINER',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '7 days')) * 1000
),
(
    '66666666-6666-6666-6666-666666666662',
    '13814000-1dd2-11b2-8080-808080808080',
    '33333333-3333-3333-3333-333333333331',
    '55555555-5555-5555-5555-555555555551',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '6 days' + INTERVAL '12 hours')) * 1000,
    1249,
    17800.0, 42000.0, 72.0, 82.0,
    31200.0, 27000.0, 15600.0, 47200.0,
    135000, 0.0000074, 82.46,
    1.2, 1.1, 1.012,
    'PALMGREN_MINER',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '6 days' + INTERVAL '12 hours')) * 1000
);

-- Cálculos para REEL-003 (en uso actual)
INSERT INTO ct_fatigue_log (
    id, tenant_id, reel_id, job_id,
    timestamp, cycle_number,
    pressure_psi, tension_lbf, bend_radius_in, temperature_f,
    hoop_stress_psi, axial_stress_psi, bending_stress_psi, von_mises_stress_psi,
    cycles_to_failure, fatigue_increment, accumulated_fatigue_percent,
    corrosion_factor, weld_factor, temperature_factor,
    calculation_method,
    created_time
) VALUES 
(
    '66666666-6666-6666-6666-666666666663',
    '13814000-1dd2-11b2-8080-808080808080',
    '33333333-3333-3333-3333-333333333333',
    '55555555-5555-5555-5555-555555555552',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '2 days')) * 1000,
    519,
    22000.0, 48000.0, 72.0, 90.0,
    38600.0, 30800.0, 15600.0, 52400.0,
    95000, 0.0000105, 45.75,
    1.0, 1.0, 1.020,
    'PALMGREN_MINER',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '2 days')) * 1000
),
(
    '66666666-6666-6666-6666-666666666664',
    '13814000-1dd2-11b2-8080-808080808080',
    '33333333-3333-3333-3333-333333333333',
    '55555555-5555-5555-5555-555555555552',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '1 day')) * 1000,
    520,
    21500.0, 47000.0, 72.0, 88.0,
    37750.0, 30200.0, 15600.0, 51800.0,
    98000, 0.0000102, 45.80,
    1.0, 1.0, 1.018,
    'PALMGREN_MINER',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '1 day')) * 1000
);

-- ============================================================================
-- COMENTARIOS
-- ============================================================================
COMMENT ON TABLE ct_units IS 'Datos de ejemplo: 2 unidades CT';
COMMENT ON TABLE ct_reels IS 'Datos de ejemplo: 3 reels con diferentes niveles de fatiga';
COMMENT ON TABLE ct_jobs IS 'Datos de ejemplo: 3 trabajos (completado, en progreso, planificado)';
COMMENT ON TABLE ct_fatigue_log IS 'Datos de ejemplo: 4 cálculos de fatiga';
