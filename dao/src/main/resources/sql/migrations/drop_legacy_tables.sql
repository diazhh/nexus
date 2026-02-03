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

-- Script para eliminar tablas legacy de activos físicos
-- Estas tablas ya no se usan porque los activos ahora se almacenan como ThingsBoard Assets

-- Eliminar foreign keys que apuntan a las tablas legacy
ALTER TABLE ct_jobs DROP CONSTRAINT IF EXISTS fk_ct_jobs_unit;
ALTER TABLE ct_jobs DROP CONSTRAINT IF EXISTS fk_ct_jobs_reel;
ALTER TABLE ct_fatigue_log DROP CONSTRAINT IF EXISTS fk_ct_fatigue_log_reel;
ALTER TABLE dr_runs DROP CONSTRAINT IF EXISTS fk_dr_runs_rig;
ALTER TABLE dr_runs DROP CONSTRAINT IF EXISTS fk_dr_runs_bha;

-- Eliminar las tablas legacy de activos físicos
-- Nota: Los datos transaccionales (ct_jobs, dr_runs, etc.) se mantienen
-- Las columnas unit_id, reel_id, rig_id, bha_id ahora contienen asset UUIDs
DROP TABLE IF EXISTS ct_units CASCADE;
DROP TABLE IF EXISTS ct_reels CASCADE;
DROP TABLE IF EXISTS dr_rigs CASCADE;
DROP TABLE IF EXISTS dr_bhas CASCADE;

-- Verificar que las tablas fueron eliminadas
-- SELECT table_name FROM information_schema.tables
-- WHERE table_schema = 'public' AND table_name IN ('ct_units', 'ct_reels', 'dr_rigs', 'dr_bhas');
