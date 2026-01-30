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
-- NEXUS MODULE MANAGEMENT SYSTEM
-- This schema manages which operational modules are available per tenant
-- ============================================================================

-- nx_module: Catalog of available modules in the system
CREATE TABLE IF NOT EXISTS nx_module (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    module_key VARCHAR(100) NOT NULL,
    module_name VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    category VARCHAR(50),
    icon VARCHAR(100),
    route_path VARCHAR(255),
    is_system_module BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    additional_info VARCHAR,
    CONSTRAINT nx_module_pkey PRIMARY KEY (id),
    CONSTRAINT nx_module_key_unq UNIQUE (module_key)
);

-- Indexes for nx_module
CREATE INDEX IF NOT EXISTS idx_nx_module_key ON nx_module(module_key);
CREATE INDEX IF NOT EXISTS idx_nx_module_category ON nx_module(category);
CREATE INDEX IF NOT EXISTS idx_nx_module_available ON nx_module(is_available) WHERE is_available = TRUE;

-- nx_tenant_module: Relationship between tenants and their activated modules
CREATE TABLE IF NOT EXISTS nx_tenant_module (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    module_id UUID NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    activation_date BIGINT NOT NULL,
    deactivation_date BIGINT,
    activated_by UUID,
    configuration VARCHAR,
    additional_info VARCHAR,
    CONSTRAINT nx_tenant_module_pkey PRIMARY KEY (id),
    CONSTRAINT nx_tenant_module_tenant_module_unq UNIQUE (tenant_id, module_id),
    CONSTRAINT fk_nx_tenant_module_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_nx_tenant_module_module FOREIGN KEY (module_id)
        REFERENCES nx_module(id) ON DELETE CASCADE
);

-- Indexes for nx_tenant_module
CREATE INDEX IF NOT EXISTS idx_nx_tenant_module_tenant ON nx_tenant_module(tenant_id);
CREATE INDEX IF NOT EXISTS idx_nx_tenant_module_module ON nx_tenant_module(module_id);
CREATE INDEX IF NOT EXISTS idx_nx_tenant_module_active ON nx_tenant_module(tenant_id, is_active)
    WHERE is_active = TRUE;

-- nx_module_permission: Permissions specific to each module
CREATE TABLE IF NOT EXISTS nx_module_permission (
    id UUID NOT NULL,
    module_id UUID NOT NULL,
    permission_key VARCHAR(200) NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    description TEXT,
    permission_group VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE,
    CONSTRAINT nx_module_permission_pkey PRIMARY KEY (id),
    CONSTRAINT nx_module_permission_module_key_unq UNIQUE (module_id, permission_key),
    CONSTRAINT fk_nx_module_permission_module FOREIGN KEY (module_id)
        REFERENCES nx_module(id) ON DELETE CASCADE
);

-- Indexes for nx_module_permission
CREATE INDEX IF NOT EXISTS idx_nx_module_permission_module ON nx_module_permission(module_id);
CREATE INDEX IF NOT EXISTS idx_nx_module_permission_key ON nx_module_permission(permission_key);

-- nx_role_module_permission: Relationship between roles and module permissions
CREATE TABLE IF NOT EXISTS nx_role_module_permission (
    id UUID NOT NULL,
    role_id UUID NOT NULL,
    module_permission_id UUID NOT NULL,
    granted BOOLEAN DEFAULT TRUE,
    granted_by UUID,
    granted_time BIGINT NOT NULL,
    CONSTRAINT nx_role_module_permission_pkey PRIMARY KEY (id),
    CONSTRAINT nx_role_module_permission_role_perm_unq UNIQUE (role_id, module_permission_id),
    CONSTRAINT fk_nx_role_module_permission_role FOREIGN KEY (role_id)
        REFERENCES role(id) ON DELETE CASCADE,
    CONSTRAINT fk_nx_role_module_permission_perm FOREIGN KEY (module_permission_id)
        REFERENCES nx_module_permission(id) ON DELETE CASCADE
);

-- Indexes for nx_role_module_permission
CREATE INDEX IF NOT EXISTS idx_nx_role_module_permission_role ON nx_role_module_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_nx_role_module_permission_perm ON nx_role_module_permission(module_permission_id);
