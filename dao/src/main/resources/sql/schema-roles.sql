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

-- Role table: Stores custom roles per tenant and system roles
CREATE TABLE IF NOT EXISTS role (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    tenant_id UUID,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    is_system BOOLEAN DEFAULT FALSE,
    additional_info VARCHAR,
    version BIGINT DEFAULT 1,
    CONSTRAINT role_pkey PRIMARY KEY (id),
    CONSTRAINT role_tenant_name_unq UNIQUE (tenant_id, name),
    CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_role_tenant_id ON role(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_role_is_system ON role(is_system) WHERE is_system = TRUE;
CREATE INDEX IF NOT EXISTS idx_role_name ON role(name);

-- Role permission table: Stores permissions matrix (Resource × Operation)
CREATE TABLE IF NOT EXISTS role_permission (
    id UUID NOT NULL,
    role_id UUID NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    CONSTRAINT role_permission_pkey PRIMARY KEY (id),
    CONSTRAINT role_permission_role_resource_op_unq 
        UNIQUE (role_id, resource_type, operation),
    CONSTRAINT fk_role_permission_role 
        FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_role_permission_role_id ON role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_composite 
    ON role_permission(role_id, resource_type, operation);

-- Add role_id column to tb_user table
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS role_id UUID;

-- Add foreign key constraint
ALTER TABLE tb_user ADD CONSTRAINT fk_user_role 
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE SET NULL;

-- Create index on role_id
CREATE INDEX IF NOT EXISTS idx_user_role_id ON tb_user(role_id) WHERE role_id IS NOT NULL;

-- Make authority column nullable for users with custom roles
ALTER TABLE tb_user ALTER COLUMN authority DROP NOT NULL;
