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

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- Initial data for roles and permissions system
-- This script creates system roles and their permissions

-- System Administrator Role (Global - no tenant)
INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
VALUES (
    'e8a8e8e0-1234-4321-8765-000000000001',
    EXTRACT(EPOCH FROM NOW()) * 1000,
    NULL,
    'System Administrator',
    'Full system access across all tenants',
    TRUE,
    1
) ON CONFLICT (id) DO NOTHING;

-- System Administrator Permission - ALL on ALL
INSERT INTO role_permission (id, role_id, resource_type, operation)
VALUES (
    'e8a8e8e0-1234-4321-8765-000000000002',
    'e8a8e8e0-1234-4321-8765-000000000001',
    'ALL',
    'ALL'
) ON CONFLICT (role_id, resource_type, operation) DO NOTHING;

-- Function to create default tenant roles
CREATE OR REPLACE FUNCTION create_default_tenant_roles(p_tenant_id UUID)
RETURNS VOID AS $$
DECLARE
    v_tenant_admin_role_id UUID;
    v_customer_user_role_id UUID;
BEGIN
    -- Generate UUIDs for roles
    v_tenant_admin_role_id := gen_random_uuid();
    v_customer_user_role_id := gen_random_uuid();
    
    -- Tenant Administrator Role
    INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
    VALUES (
        v_tenant_admin_role_id,
        EXTRACT(EPOCH FROM NOW()) * 1000,
        p_tenant_id,
        'Tenant Administrator',
        'Full access to tenant resources',
        TRUE,
        1
    ) ON CONFLICT (tenant_id, name) DO NOTHING;
    
    -- Tenant Administrator Permissions - ALL on multiple resources
    INSERT INTO role_permission (id, role_id, resource_type, operation)
    SELECT 
        gen_random_uuid(),
        v_tenant_admin_role_id,
        res.resource_type,
        'ALL'
    FROM (
        VALUES 
            ('DEVICE'), ('ASSET'), ('DASHBOARD'), ('USER'), 
            ('CUSTOMER'), ('RULE_CHAIN'), ('ENTITY_VIEW'),
            ('WIDGETS_BUNDLE'), ('WIDGET_TYPE'), ('DEVICE_PROFILE'),
            ('ASSET_PROFILE'), ('TB_RESOURCE'), ('OTA_PACKAGE'),
            ('EDGE'), ('RPC'), ('ALARM'), ('NOTIFICATION'),
            ('OAUTH2_CLIENT'), ('MOBILE_APP'), ('JOB'), ('AI_MODEL')
    ) AS res(resource_type)
    ON CONFLICT (role_id, resource_type, operation) DO NOTHING;
    
    -- Customer User Role
    INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
    VALUES (
        v_customer_user_role_id,
        EXTRACT(EPOCH FROM NOW()) * 1000,
        p_tenant_id,
        'Customer User',
        'Limited access for customer users',
        TRUE,
        1
    ) ON CONFLICT (tenant_id, name) DO NOTHING;
    
    -- Customer User Permissions - Limited access
    INSERT INTO role_permission (id, role_id, resource_type, operation)
    SELECT 
        gen_random_uuid(),
        v_customer_user_role_id,
        perm.resource_type,
        perm.operation
    FROM (
        VALUES 
            ('DEVICE', 'READ'),
            ('DEVICE', 'READ_CREDENTIALS'),
            ('DEVICE', 'CLAIM_DEVICES'),
            ('ASSET', 'READ'),
            ('DASHBOARD', 'READ'),
            ('ENTITY_VIEW', 'READ'),
            ('ALARM', 'READ'),
            ('ALARM', 'WRITE'),
            ('RPC', 'RPC_CALL')
    ) AS perm(resource_type, operation)
    ON CONFLICT (role_id, resource_type, operation) DO NOTHING;
    
END;
$$ LANGUAGE plpgsql;

-- Trigger to create default roles when new tenant is created
CREATE OR REPLACE FUNCTION trigger_create_default_roles()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM create_default_tenant_roles(NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tenant_roles_trigger ON tenant;
CREATE TRIGGER tenant_roles_trigger
    AFTER INSERT ON tenant
    FOR EACH ROW
    EXECUTE FUNCTION trigger_create_default_roles();
