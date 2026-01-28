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

-- Migración V3: Agregar columnas faltantes para sincronizar con entidades JPA

-- ============================================================================
-- TABLA: ct_reels - Agregar corrosion_factor
-- ============================================================================
ALTER TABLE ct_reels ADD COLUMN IF NOT EXISTS corrosion_factor NUMERIC(3, 2);

COMMENT ON COLUMN ct_reels.corrosion_factor IS 'Factor de corrección por corrosión aplicado en cálculos de fatiga';

-- ============================================================================
-- TABLA: ct_jobs - Agregar columnas de aprobación y certificación
-- ============================================================================
ALTER TABLE ct_jobs ADD COLUMN IF NOT EXISTS approved_by UUID;
ALTER TABLE ct_jobs ADD COLUMN IF NOT EXISTS approved_time BIGINT;
ALTER TABLE ct_jobs ADD COLUMN IF NOT EXISTS certification_expiry_date BIGINT;
ALTER TABLE ct_jobs ADD COLUMN IF NOT EXISTS certification_status VARCHAR(50);

COMMENT ON COLUMN ct_jobs.approved_by IS 'Usuario que aprobó el trabajo';
COMMENT ON COLUMN ct_jobs.approved_time IS 'Timestamp de aprobación del trabajo';
COMMENT ON COLUMN ct_jobs.certification_expiry_date IS 'Fecha de expiración de certificación';
COMMENT ON COLUMN ct_jobs.certification_status IS 'Estado de certificación del trabajo';

-- ============================================================================
-- TABLA: ct_fatigue_log - Agregar factores de corrección
-- ============================================================================
ALTER TABLE ct_fatigue_log ADD COLUMN IF NOT EXISTS corrosion_factor NUMERIC(4, 3);
ALTER TABLE ct_fatigue_log ADD COLUMN IF NOT EXISTS weld_factor NUMERIC(4, 3);
ALTER TABLE ct_fatigue_log ADD COLUMN IF NOT EXISTS temperature_factor NUMERIC(4, 3);

COMMENT ON COLUMN ct_fatigue_log.corrosion_factor IS 'Factor de corrección por corrosión aplicado';
COMMENT ON COLUMN ct_fatigue_log.weld_factor IS 'Factor de corrección por soldaduras aplicado';
COMMENT ON COLUMN ct_fatigue_log.temperature_factor IS 'Factor de corrección por temperatura aplicado';

-- ============================================================================
-- Índices adicionales para nuevas columnas
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_ct_jobs_approved_by ON ct_jobs(approved_by) WHERE approved_by IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ct_jobs_certification_status ON ct_jobs(certification_status) WHERE certification_status IS NOT NULL;
