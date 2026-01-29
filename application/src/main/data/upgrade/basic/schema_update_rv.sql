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

-- =====================================================
-- NEXUS RESERVOIR MODULE (rv) - Database Schema
-- =====================================================

-- =====================================================
-- 1. RV_CATALOGS TABLE
-- Stores catalog/enumeration data for the reservoir module
-- =====================================================

CREATE TABLE IF NOT EXISTS rv_catalogs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    catalog_type VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    metadata JSONB,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_time BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_time BIGINT,
    CONSTRAINT rv_catalogs_pkey PRIMARY KEY (id),
    CONSTRAINT rv_catalogs_unq UNIQUE (tenant_id, catalog_type, code)
);

-- Indexes for rv_catalogs
CREATE INDEX IF NOT EXISTS idx_rv_catalogs_tenant_type ON rv_catalogs(tenant_id, catalog_type);
CREATE INDEX IF NOT EXISTS idx_rv_catalogs_code ON rv_catalogs(code);

-- =====================================================
-- 2. SEED DATA - WELL TYPES
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'WELL_TYPE', 'PRODUCER', 'Productor', 'Pozo productor de hidrocarburos', '{"icon": "oil_barrel"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'WELL_TYPE' AND code = 'PRODUCER');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'WELL_TYPE', 'INJECTOR_WATER', 'Inyector de Agua', 'Pozo inyector de agua', '{"icon": "water_drop"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'WELL_TYPE' AND code = 'INJECTOR_WATER');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'WELL_TYPE', 'INJECTOR_GAS', 'Inyector de Gas', 'Pozo inyector de gas', '{"icon": "gas_meter"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'WELL_TYPE' AND code = 'INJECTOR_GAS');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'WELL_TYPE', 'OBSERVATION', 'Observación', 'Pozo de observación/monitoreo', '{"icon": "visibility"}'::JSONB, 4
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'WELL_TYPE' AND code = 'OBSERVATION');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'WELL_TYPE', 'DISPOSAL', 'Disposición', 'Pozo de disposición de agua', '{"icon": "delete"}'::JSONB, 5
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'WELL_TYPE' AND code = 'DISPOSAL');

-- =====================================================
-- 3. SEED DATA - LITHOLOGY TYPES
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'LITHOLOGY', 'SANDSTONE', 'Arenisca', 'Roca sedimentaria clástica', '{"porosity_range": "15-30%", "permeability": "high"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'LITHOLOGY' AND code = 'SANDSTONE');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'LITHOLOGY', 'LIMESTONE', 'Caliza', 'Roca carbonatada', '{"porosity_range": "5-25%", "permeability": "variable"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'LITHOLOGY' AND code = 'LIMESTONE');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'LITHOLOGY', 'DOLOMITE', 'Dolomita', 'Carbonato de calcio y magnesio', '{"porosity_range": "10-20%", "permeability": "medium"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'LITHOLOGY' AND code = 'DOLOMITE');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'LITHOLOGY', 'SHALE', 'Lutita', 'Roca sedimentaria de grano fino', '{"porosity_range": "0-10%", "permeability": "very_low"}'::JSONB, 4
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'LITHOLOGY' AND code = 'SHALE');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'LITHOLOGY', 'CONGLOMERATE', 'Conglomerado', 'Roca sedimentaria de fragmentos gruesos', '{"porosity_range": "10-25%", "permeability": "medium_high"}'::JSONB, 5
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'LITHOLOGY' AND code = 'CONGLOMERATE');

-- =====================================================
-- 4. SEED DATA - DRIVE MECHANISMS
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'DRIVE_MECHANISM', 'SOLUTION_GAS', 'Gas en Solución', 'Empuje por gas disuelto liberado', '{"typical_rf": "5-30%"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'DRIVE_MECHANISM' AND code = 'SOLUTION_GAS');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'DRIVE_MECHANISM', 'GAS_CAP', 'Casquete de Gas', 'Empuje por expansión de casquete', '{"typical_rf": "20-40%"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'DRIVE_MECHANISM' AND code = 'GAS_CAP');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'DRIVE_MECHANISM', 'WATER_DRIVE', 'Empuje por Agua', 'Empuje por acuífero activo', '{"typical_rf": "35-75%"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'DRIVE_MECHANISM' AND code = 'WATER_DRIVE');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'DRIVE_MECHANISM', 'GRAVITY_DRAINAGE', 'Drenaje Gravitacional', 'Segregación gravitacional', '{"typical_rf": "40-80%"}'::JSONB, 4
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'DRIVE_MECHANISM' AND code = 'GRAVITY_DRAINAGE');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'DRIVE_MECHANISM', 'COMBINATION', 'Combinado', 'Múltiples mecanismos', '{"typical_rf": "variable"}'::JSONB, 5
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'DRIVE_MECHANISM' AND code = 'COMBINATION');

-- =====================================================
-- 5. SEED DATA - VENEZUELAN FORMATIONS (Cuenca Oriental)
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'OFICINA', 'Formación Oficina', 'Mioceno - Cuenca Oriental', '{"age": "Mioceno", "basin": "Oriental", "lithology": "Areniscas/Lutitas"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'OFICINA');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'MORICHAL', 'Miembro Morichal', 'Mioceno Temprano - Faja del Orinoco', '{"age": "Mioceno Temprano", "basin": "Oriental", "lithology": "Areniscas"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'MORICHAL');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'YABO', 'Miembro Yabo', 'Mioceno - Faja del Orinoco', '{"age": "Mioceno", "basin": "Oriental", "lithology": "Areniscas"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'YABO');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'PILON', 'Miembro Pilón', 'Mioceno - Faja del Orinoco', '{"age": "Mioceno", "basin": "Oriental", "lithology": "Areniscas"}'::JSONB, 4
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'PILON');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'MERECURE', 'Formación Merecure', 'Oligoceno - Cuenca Oriental', '{"age": "Oligoceno", "basin": "Oriental", "lithology": "Areniscas"}'::JSONB, 5
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'MERECURE');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'TEMBLADOR', 'Formación Temblador', 'Cretácico - Cuenca Oriental', '{"age": "Cretácico", "basin": "Oriental", "lithology": "Areniscas", "type": "Source/Reservoir"}'::JSONB, 6
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'TEMBLADOR');

-- =====================================================
-- 6. SEED DATA - VENEZUELAN FORMATIONS (Cuenca Maracaibo)
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'MISOA', 'Formación Misoa', 'Eoceno - Cuenca Maracaibo', '{"age": "Eoceno", "basin": "Maracaibo", "lithology": "Areniscas"}'::JSONB, 10
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'MISOA');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'LA_ROSA', 'Formación La Rosa', 'Mioceno - Cuenca Maracaibo', '{"age": "Mioceno", "basin": "Maracaibo", "lithology": "Areniscas/Lutitas"}'::JSONB, 11
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'LA_ROSA');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'LAGUNILLAS', 'Formación Lagunillas', 'Mioceno - Cuenca Maracaibo', '{"age": "Mioceno", "basin": "Maracaibo", "lithology": "Areniscas"}'::JSONB, 12
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'LAGUNILLAS');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'LA_LUNA', 'Formación La Luna', 'Cretácico - Roca Madre', '{"age": "Cretácico", "basin": "Maracaibo", "lithology": "Calizas/Margas", "type": "Source Rock"}'::JSONB, 13
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'LA_LUNA');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'MARACA', 'Formación Maraca', 'Cretácico - Cuenca Maracaibo', '{"age": "Cretácico", "basin": "Maracaibo", "lithology": "Calizas"}'::JSONB, 14
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'MARACA');

-- =====================================================
-- 7. SEED DATA - VENEZUELAN FORMATIONS (Cuenca Barinas-Apure)
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'GOBERNADOR', 'Formación Gobernador', 'Eoceno - Cuenca Barinas-Apure', '{"age": "Eoceno", "basin": "Barinas-Apure", "lithology": "Areniscas"}'::JSONB, 20
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'GOBERNADOR');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'ESCANDALOSA', 'Formación Escandalosa', 'Cretácico - Cuenca Barinas-Apure', '{"age": "Cretácico", "basin": "Barinas-Apure", "lithology": "Areniscas"}'::JSONB, 21
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'ESCANDALOSA');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FORMATION_VE', 'NAVAY', 'Formación Navay', 'Cretácico - Cuenca Barinas-Apure', '{"age": "Cretácico", "basin": "Barinas-Apure", "lithology": "Lutitas", "type": "Seal/Source"}'::JSONB, 22
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FORMATION_VE' AND code = 'NAVAY');

-- =====================================================
-- 8. SEED DATA - FAJA DEL ORINOCO REGIONS
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FAJA_REGION', 'BOYACA', 'Boyacá', 'Sector occidental de la Faja', '{"depth_range": "300-500m", "gor_range": "50-100 scf/stb", "api_range": "8-10", "foamy_strength": "WEAK", "expected_rf": "5-8%"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FAJA_REGION' AND code = 'BOYACA');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FAJA_REGION', 'JUNIN', 'Junín', 'Sector centro-occidental de la Faja', '{"depth_range": "300-500m", "gor_range": "80-150 scf/stb", "api_range": "8-10", "foamy_strength": "WEAK_MODERATE", "expected_rf": "6-10%"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FAJA_REGION' AND code = 'JUNIN');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FAJA_REGION', 'AYACUCHO', 'Ayacucho', 'Sector centro-oriental de la Faja', '{"depth_range": "800-1200m", "gor_range": "150-300 scf/stb", "api_range": "8-12", "foamy_strength": "STRONG", "expected_rf": "10-12%"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FAJA_REGION' AND code = 'AYACUCHO');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'FAJA_REGION', 'CARABOBO', 'Carabobo', 'Sector oriental de la Faja', '{"depth_range": "800-1200m", "gor_range": "200-400 scf/stb", "api_range": "8-12", "foamy_strength": "VERY_STRONG", "expected_rf": "10-15%"}'::JSONB, 4
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'FAJA_REGION' AND code = 'CARABOBO');

-- =====================================================
-- 9. SEED DATA - COMPLETION TYPES
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'COMPLETION_TYPE', 'OPENHOLE', 'Hueco Abierto', 'Completación sin revestimiento en zona productora', '{"description": "No casing in pay zone"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'COMPLETION_TYPE' AND code = 'OPENHOLE');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'COMPLETION_TYPE', 'CASED_PERFORATED', 'Revestido Perforado', 'Casing cementado con disparos', '{"description": "Standard cased hole completion"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'COMPLETION_TYPE' AND code = 'CASED_PERFORATED');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'COMPLETION_TYPE', 'GRAVEL_PACK', 'Empaque de Grava', 'Control de arena con grava', '{"description": "Sand control with gravel"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'COMPLETION_TYPE' AND code = 'GRAVEL_PACK');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'COMPLETION_TYPE', 'FRAC_PACK', 'Frac Pack', 'Fracturamiento con empaque de grava', '{"description": "Hydraulic fracturing with gravel"}'::JSONB, 4
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'COMPLETION_TYPE' AND code = 'FRAC_PACK');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'COMPLETION_TYPE', 'SLOTTED_LINER', 'Liner Ranurado', 'Liner con ranuras para control de arena', '{"description": "Slotted liner for sand control"}'::JSONB, 5
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'COMPLETION_TYPE' AND code = 'SLOTTED_LINER');

-- =====================================================
-- 10. SEED DATA - ARTIFICIAL LIFT METHODS
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'ARTIFICIAL_LIFT', 'NATURAL_FLOW', 'Flujo Natural', 'Producción sin levantamiento artificial', '{"energy_source": "reservoir"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'ARTIFICIAL_LIFT' AND code = 'NATURAL_FLOW');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'ARTIFICIAL_LIFT', 'ESP', 'Bomba Electrosumergible', 'Electric Submersible Pump', '{"energy_source": "electric", "typical_depth": "up to 3000m"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'ARTIFICIAL_LIFT' AND code = 'ESP');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'ARTIFICIAL_LIFT', 'SRP', 'Bombeo Mecánico', 'Sucker Rod Pump - Balancín', '{"energy_source": "mechanical", "typical_depth": "up to 2500m"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'ARTIFICIAL_LIFT' AND code = 'SRP');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'ARTIFICIAL_LIFT', 'GAS_LIFT', 'Levantamiento por Gas', 'Inyección de gas para reducir columna hidrostática', '{"energy_source": "compressed_gas", "typical_depth": "variable"}'::JSONB, 4
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'ARTIFICIAL_LIFT' AND code = 'GAS_LIFT');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'ARTIFICIAL_LIFT', 'PCP', 'Bomba de Cavidad Progresiva', 'Progressive Cavity Pump', '{"energy_source": "rotational", "typical_depth": "up to 1500m", "good_for": "heavy_oil"}'::JSONB, 5
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'ARTIFICIAL_LIFT' AND code = 'PCP');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'ARTIFICIAL_LIFT', 'JET_PUMP', 'Bomba Jet', 'Jet/Hydraulic Pump', '{"energy_source": "hydraulic", "typical_depth": "deep_wells"}'::JSONB, 6
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'ARTIFICIAL_LIFT' AND code = 'JET_PUMP');

-- =====================================================
-- 11. SEED DATA - IPR METHODS
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'IPR_METHOD', 'VOGEL', 'Vogel', 'Método de Vogel para yacimientos saturados', '{"application": "saturated_reservoirs", "formula": "q/qmax = 1 - 0.2(Pwf/Pr) - 0.8(Pwf/Pr)^2"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'IPR_METHOD' AND code = 'VOGEL');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'IPR_METHOD', 'DARCY', 'Darcy (Lineal)', 'Ecuación de Darcy para flujo lineal', '{"application": "undersaturated_reservoirs", "formula": "q = J(Pr - Pwf)"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'IPR_METHOD' AND code = 'DARCY');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'IPR_METHOD', 'FETKOVICH', 'Fetkovich', 'Método de Fetkovich para gas y petróleo', '{"application": "gas_and_oil", "formula": "q = C(Pr^2 - Pwf^2)^n"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'IPR_METHOD' AND code = 'FETKOVICH');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'IPR_METHOD', 'JONES', 'Jones', 'Método de Jones con efectos de turbulencia', '{"application": "high_rate_wells", "formula": "Pr - Pwf = Aq + Bq^2"}'::JSONB, 4
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'IPR_METHOD' AND code = 'JONES');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'IPR_METHOD', 'COMPOSITE', 'Compuesto', 'Método compuesto Darcy + Vogel', '{"application": "subsaturated_to_saturated", "formula": "Linear above Pb, Vogel below Pb"}'::JSONB, 5
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'IPR_METHOD' AND code = 'COMPOSITE');

-- =====================================================
-- 12. SEED DATA - DECLINE TYPES
-- =====================================================

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'DECLINE_TYPE', 'EXPONENTIAL', 'Exponencial', 'Declinación exponencial (b=0)', '{"b_value": 0, "formula": "q = qi * exp(-Di*t)", "characteristic": "constant_decline_rate"}'::JSONB, 1
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'DECLINE_TYPE' AND code = 'EXPONENTIAL');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'DECLINE_TYPE', 'HYPERBOLIC', 'Hiperbólico', 'Declinación hiperbólica (0<b<1)', '{"b_value": "0-1", "formula": "q = qi / (1 + b*Di*t)^(1/b)", "characteristic": "decreasing_decline_rate"}'::JSONB, 2
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'DECLINE_TYPE' AND code = 'HYPERBOLIC');

INSERT INTO rv_catalogs (tenant_id, catalog_type, code, name, description, metadata, sort_order)
SELECT '13814000-1dd2-11b2-8080-808080808080'::UUID, 'DECLINE_TYPE', 'HARMONIC', 'Armónico', 'Declinación armónica (b=1)', '{"b_value": 1, "formula": "q = qi / (1 + Di*t)", "characteristic": "slowest_decline"}'::JSONB, 3
WHERE NOT EXISTS (SELECT 1 FROM rv_catalogs WHERE catalog_type = 'DECLINE_TYPE' AND code = 'HARMONIC');

-- =====================================================
-- End of Reservoir Module Schema
-- =====================================================
