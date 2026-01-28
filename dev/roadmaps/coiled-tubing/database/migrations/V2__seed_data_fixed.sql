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

-- Datos de ejemplo para el módulo Coiled Tubing (CORREGIDO)
-- Usa el tenant_id real de la base de datos

-- Obtener tenant_id real
DO $$
DECLARE
    v_tenant_id UUID;
BEGIN
    SELECT id INTO v_tenant_id FROM tenant LIMIT 1;
    
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
        v_tenant_id,
        'CT-UNIT-001',
        'Coiled Tubing Unit Alpha',
        '22222222-2222-2222-2222-222222222222',
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
        v_tenant_id,
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
    
    -- Reel 1: Alta fatiga
    INSERT INTO ct_reels (
        id, tenant_id, reel_code, reel_name, asset_id,
        tubing_od_inch, tubing_id_inch, material_grade,
        total_length_ft, reel_core_diameter_inch, typical_gooseneck_radius_inch,
        status, accumulated_fatigue_percent, total_cycles,
        corrosion_environment, weld_stress_concentration_factor,
        created_time
    ) VALUES (
        '33333333-3333-3333-3333-333333333331',
        v_tenant_id,
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
    
    -- Reel 2: Baja fatiga
    INSERT INTO ct_reels (
        id, tenant_id, reel_code, reel_name, asset_id,
        tubing_od_inch, tubing_id_inch, material_grade,
        total_length_ft, status, accumulated_fatigue_percent, total_cycles,
        created_time
    ) VALUES (
        '33333333-3333-3333-3333-333333333332',
        v_tenant_id,
        'REEL-002',
        'Reel Beta - 2.875" QT-900',
        '44444444-4444-4444-4444-444444444442',
        2.875, 2.441, 'QT-900',
        20000.0,
        'AVAILABLE',
        15.2, 180,
        EXTRACT(EPOCH FROM NOW()) * 1000
    );
    
    -- Reel 3: En uso
    INSERT INTO ct_reels (
        id, tenant_id, reel_code, reel_name, asset_id,
        tubing_od_inch, tubing_id_inch, material_grade,
        total_length_ft, current_unit_id, status, accumulated_fatigue_percent, total_cycles,
        created_time
    ) VALUES (
        '33333333-3333-3333-3333-333333333333',
        v_tenant_id,
        'REEL-003',
        'Reel Gamma - 2.375" QT-800',
        '44444444-4444-4444-4444-444444444443',
        2.375, 1.995, 'QT-800',
        19500.0,
        '11111111-1111-1111-1111-111111111112',
        'IN_USE',
        45.8, 650,
        EXTRACT(EPOCH FROM NOW()) * 1000
    );
    
    -- ============================================================================
    -- JOBS DE EJEMPLO
    -- ============================================================================
    
    -- Job 1: Completado exitosamente
    INSERT INTO ct_jobs (
        id, tenant_id, job_number, job_name, job_type,
        unit_id, reel_id,
        well_name, field_name, client_name,
        planned_start_date, actual_start_date, actual_end_date,
        status, productive_time_hours,
        created_time
    ) VALUES (
        '55555555-5555-5555-5555-555555555551',
        v_tenant_id,
        'JOB-2026-001',
        'Well Cleanout VM-001',
        'WELL_CLEANOUT',
        '11111111-1111-1111-1111-111111111111',
        '33333333-3333-3333-3333-333333333331',
        'VM-001',
        'Vaca Muerta',
        'YPF',
        EXTRACT(EPOCH FROM (NOW() - INTERVAL '7 days')) * 1000,
        EXTRACT(EPOCH FROM (NOW() - INTERVAL '7 days')) * 1000,
        EXTRACT(EPOCH FROM (NOW() - INTERVAL '6 days')) * 1000,
        'COMPLETED',
        18.5,
        EXTRACT(EPOCH FROM NOW()) * 1000
    );
    
    -- Job 2: En progreso
    INSERT INTO ct_jobs (
        id, tenant_id, job_number, job_name, job_type,
        unit_id, reel_id,
        well_name, field_name, client_name,
        planned_start_date, actual_start_date,
        status, current_phase,
        created_time
    ) VALUES (
        '55555555-5555-5555-5555-555555555552',
        v_tenant_id,
        'JOB-2026-002',
        'Acid Stimulation PB-045',
        'ACID_STIMULATION',
        '11111111-1111-1111-1111-111111111112',
        '33333333-3333-3333-3333-333333333333',
        'PB-045',
        'Permian Basin',
        'Chevron',
        EXTRACT(EPOCH FROM (NOW() - INTERVAL '2 days')) * 1000,
        EXTRACT(EPOCH FROM (NOW() - INTERVAL '2 days')) * 1000,
        'IN_PROGRESS',
        'PUMPING',
        EXTRACT(EPOCH FROM NOW()) * 1000
    );
    
    RAISE NOTICE 'Seed data insertado correctamente para tenant: %', v_tenant_id;
END $$;
