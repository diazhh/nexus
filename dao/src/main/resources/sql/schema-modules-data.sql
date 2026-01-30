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
-- NEXUS MODULE SEED DATA
-- Initial modules available in the system
-- ============================================================================

-- Insert Coiled Tubing Module
INSERT INTO nx_module (id, created_time, module_key, module_name, description, version, category, icon, route_path, is_system_module, is_available, display_order)
VALUES (
    '1e746126-eacc-11ee-a951-0242ac120001',
    EXTRACT(EPOCH FROM NOW()) * 1000,
    'coiled-tubing',
    'Coiled Tubing',
    'Coiled Tubing operations management - Units, Reels, Jobs, Fatigue tracking',
    '1.0.0',
    'OPERATIONS',
    'mdi:pipe',
    '/ct',
    FALSE,
    TRUE,
    100
) ON CONFLICT (module_key) DO NOTHING;

-- Insert Reservoir Module (Yacimientos)
INSERT INTO nx_module (id, created_time, module_key, module_name, description, version, category, icon, route_path, is_system_module, is_available, display_order)
VALUES (
    '1e746126-eacc-11ee-a951-0242ac120002',
    EXTRACT(EPOCH FROM NOW()) * 1000,
    'reservoir',
    'Reservoir Engineering',
    'Reservoir characterization and analysis - Basins, Fields, Wells, PVT, Material Balance',
    '1.0.0',
    'ENGINEERING',
    'mdi:oil',
    '/rv',
    FALSE,
    TRUE,
    200
) ON CONFLICT (module_key) DO NOTHING;

-- Insert Drilling Module (Perforación)
INSERT INTO nx_module (id, created_time, module_key, module_name, description, version, category, icon, route_path, is_system_module, is_available, display_order)
VALUES (
    '1e746126-eacc-11ee-a951-0242ac120003',
    EXTRACT(EPOCH FROM NOW()) * 1000,
    'drilling',
    'Drilling Operations',
    'Drilling operations management - Rigs, BHAs, Runs, Real-time monitoring, Well Control',
    '1.0.0',
    'OPERATIONS',
    'mdi:oil-lamp',
    '/dr',
    FALSE,
    TRUE,
    300
) ON CONFLICT (module_key) DO NOTHING;

-- ============================================================================
-- MODULE PERMISSIONS
-- ============================================================================

-- Coiled Tubing Permissions
INSERT INTO nx_module_permission (id, module_id, permission_key, permission_name, description, permission_group, is_default)
VALUES
    ('2e746126-eacc-11ee-a951-0242ac120001', '1e746126-eacc-11ee-a951-0242ac120001', 'CT_UNITS_VIEW', 'View Units', 'View CT units list and details', 'UNITS', TRUE),
    ('2e746126-eacc-11ee-a951-0242ac120002', '1e746126-eacc-11ee-a951-0242ac120001', 'CT_UNITS_EDIT', 'Edit Units', 'Create and edit CT units', 'UNITS', FALSE),
    ('2e746126-eacc-11ee-a951-0242ac120003', '1e746126-eacc-11ee-a951-0242ac120001', 'CT_REELS_VIEW', 'View Reels', 'View reels list and details', 'REELS', TRUE),
    ('2e746126-eacc-11ee-a951-0242ac120004', '1e746126-eacc-11ee-a951-0242ac120001', 'CT_REELS_EDIT', 'Edit Reels', 'Create and edit reels', 'REELS', FALSE),
    ('2e746126-eacc-11ee-a951-0242ac120005', '1e746126-eacc-11ee-a951-0242ac120001', 'CT_JOBS_VIEW', 'View Jobs', 'View jobs list and details', 'JOBS', TRUE),
    ('2e746126-eacc-11ee-a951-0242ac120006', '1e746126-eacc-11ee-a951-0242ac120001', 'CT_JOBS_MANAGE', 'Manage Jobs', 'Create, edit and manage jobs', 'JOBS', FALSE)
ON CONFLICT (module_id, permission_key) DO NOTHING;

-- Reservoir Permissions
INSERT INTO nx_module_permission (id, module_id, permission_key, permission_name, description, permission_group, is_default)
VALUES
    ('3e746126-eacc-11ee-a951-0242ac120001', '1e746126-eacc-11ee-a951-0242ac120002', 'RV_BASINS_VIEW', 'View Basins', 'View basins list and details', 'BASINS', TRUE),
    ('3e746126-eacc-11ee-a951-0242ac120002', '1e746126-eacc-11ee-a951-0242ac120002', 'RV_BASINS_EDIT', 'Edit Basins', 'Create and edit basins', 'BASINS', FALSE),
    ('3e746126-eacc-11ee-a951-0242ac120003', '1e746126-eacc-11ee-a951-0242ac120002', 'RV_FIELDS_VIEW', 'View Fields', 'View fields list and details', 'FIELDS', TRUE),
    ('3e746126-eacc-11ee-a951-0242ac120004', '1e746126-eacc-11ee-a951-0242ac120002', 'RV_FIELDS_EDIT', 'Edit Fields', 'Create and edit fields', 'FIELDS', FALSE),
    ('3e746126-eacc-11ee-a951-0242ac120005', '1e746126-eacc-11ee-a951-0242ac120002', 'RV_WELLS_VIEW', 'View Wells', 'View wells list and details', 'WELLS', TRUE),
    ('3e746126-eacc-11ee-a951-0242ac120006', '1e746126-eacc-11ee-a951-0242ac120002', 'RV_WELLS_EDIT', 'Edit Wells', 'Create and edit wells', 'WELLS', FALSE),
    ('3e746126-eacc-11ee-a951-0242ac120007', '1e746126-eacc-11ee-a951-0242ac120002', 'RV_CALCULATOR', 'Use Calculator', 'Use reservoir calculations', 'TOOLS', TRUE)
ON CONFLICT (module_id, permission_key) DO NOTHING;

-- Drilling Permissions
INSERT INTO nx_module_permission (id, module_id, permission_key, permission_name, description, permission_group, is_default)
VALUES
    ('4e746126-eacc-11ee-a951-0242ac120001', '1e746126-eacc-11ee-a951-0242ac120003', 'DR_RIGS_VIEW', 'View Rigs', 'View rigs list and details', 'RIGS', TRUE),
    ('4e746126-eacc-11ee-a951-0242ac120002', '1e746126-eacc-11ee-a951-0242ac120003', 'DR_RIGS_EDIT', 'Edit Rigs', 'Create and edit rigs', 'RIGS', FALSE),
    ('4e746126-eacc-11ee-a951-0242ac120003', '1e746126-eacc-11ee-a951-0242ac120003', 'DR_BHAS_VIEW', 'View BHAs', 'View BHA configurations', 'BHAS', TRUE),
    ('4e746126-eacc-11ee-a951-0242ac120004', '1e746126-eacc-11ee-a951-0242ac120003', 'DR_BHAS_EDIT', 'Edit BHAs', 'Create and edit BHAs', 'BHAS', FALSE),
    ('4e746126-eacc-11ee-a951-0242ac120005', '1e746126-eacc-11ee-a951-0242ac120003', 'DR_RUNS_VIEW', 'View Runs', 'View drilling runs', 'RUNS', TRUE),
    ('4e746126-eacc-11ee-a951-0242ac120006', '1e746126-eacc-11ee-a951-0242ac120003', 'DR_RUNS_MANAGE', 'Manage Runs', 'Create and manage runs', 'RUNS', FALSE),
    ('4e746126-eacc-11ee-a951-0242ac120007', '1e746126-eacc-11ee-a951-0242ac120003', 'DR_WELLCONTROL', 'Well Control Monitor', 'Access well control monitoring', 'SAFETY', TRUE)
ON CONFLICT (module_id, permission_key) DO NOTHING;
