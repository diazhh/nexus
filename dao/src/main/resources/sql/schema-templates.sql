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

-- Template System Tables

CREATE TABLE IF NOT EXISTS template_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_code VARCHAR(100) UNIQUE NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Clasificación
    module_code VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    
    -- Versión
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    is_active BOOLEAN DEFAULT true,
    
    -- Definición de la plantilla (JSON)
    template_structure JSONB NOT NULL,
    
    -- Variables requeridas
    required_variables JSONB NOT NULL,
    
    -- Metadata
    created_by UUID NOT NULL,
    created_time BIGINT NOT NULL,
    updated_by UUID,
    updated_time BIGINT,
    
    -- Tenant (para multi-tenancy)
    tenant_id UUID NOT NULL,
    
    CONSTRAINT fk_template_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_template_module_type ON template_definitions(module_code, entity_type);
CREATE INDEX IF NOT EXISTS idx_template_tenant ON template_definitions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_template_active ON template_definitions(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_template_code ON template_definitions(template_code);

CREATE TABLE IF NOT EXISTS template_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    version VARCHAR(20) NOT NULL,
    template_structure JSONB NOT NULL,
    change_description TEXT,
    created_by UUID NOT NULL,
    created_time BIGINT NOT NULL,
    
    CONSTRAINT fk_version_template FOREIGN KEY (template_id) 
        REFERENCES template_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uk_template_version UNIQUE (template_id, version)
);

CREATE INDEX IF NOT EXISTS idx_version_template ON template_versions(template_id);

CREATE TABLE IF NOT EXISTS template_instances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    template_version VARCHAR(20) NOT NULL,
    
    -- Asset raíz creado
    root_asset_id UUID NOT NULL,
    
    -- Variables usadas en la instanciación
    instance_variables JSONB NOT NULL,
    
    -- Metadata
    created_by UUID NOT NULL,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    
    CONSTRAINT fk_instance_template FOREIGN KEY (template_id) 
        REFERENCES template_definitions(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_instance_template ON template_instances(template_id);
CREATE INDEX IF NOT EXISTS idx_instance_root_asset ON template_instances(root_asset_id);
CREATE INDEX IF NOT EXISTS idx_instance_tenant ON template_instances(tenant_id);
