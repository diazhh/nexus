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
-- NEXUS DATA DISTRIBUTION SYSTEM
-- Schema for data distribution from devices to Digital Twins
-- =====================================================

-- -----------------------------------------------------
-- Table: nx_data_source_config
-- Configures relationship between device and Digital Twin assets
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS nx_data_source_config (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    device_id UUID NOT NULL,
    module_key VARCHAR(50) NOT NULL,
    target_asset_id UUID,
    target_asset_type VARCHAR(100),
    distribution_mode VARCHAR(50) DEFAULT 'MAPPED',
    mapping_config VARCHAR,
    is_active BOOLEAN DEFAULT TRUE,
    additional_info VARCHAR,
    CONSTRAINT nx_data_source_config_pkey PRIMARY KEY (id),
    CONSTRAINT nx_data_source_config_device_unq UNIQUE (tenant_id, device_id),
    CONSTRAINT fk_nx_data_source_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_nx_data_source_device FOREIGN KEY (device_id)
        REFERENCES device(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nx_data_source_tenant ON nx_data_source_config(tenant_id);
CREATE INDEX IF NOT EXISTS idx_nx_data_source_device ON nx_data_source_config(device_id);
CREATE INDEX IF NOT EXISTS idx_nx_data_source_module ON nx_data_source_config(module_key);
CREATE INDEX IF NOT EXISTS idx_nx_data_source_active ON nx_data_source_config(tenant_id, is_active) WHERE is_active = TRUE;

-- -----------------------------------------------------
-- Table: nx_data_mapping_rule
-- Defines how telemetry is transformed and distributed
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS nx_data_mapping_rule (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    data_source_config_id UUID NOT NULL,
    source_key VARCHAR(255) NOT NULL,
    target_key VARCHAR(255) NOT NULL,
    target_asset_id UUID,
    target_asset_relation VARCHAR(100),
    transformation_type VARCHAR(50) DEFAULT 'DIRECT',
    transformation_config VARCHAR,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT nx_data_mapping_rule_pkey PRIMARY KEY (id),
    CONSTRAINT fk_nx_mapping_data_source FOREIGN KEY (data_source_config_id)
        REFERENCES nx_data_source_config(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nx_mapping_source ON nx_data_mapping_rule(data_source_config_id);
CREATE INDEX IF NOT EXISTS idx_nx_mapping_source_key ON nx_data_mapping_rule(source_key);
CREATE INDEX IF NOT EXISTS idx_nx_mapping_active ON nx_data_mapping_rule(data_source_config_id, is_active) WHERE is_active = TRUE;

-- -----------------------------------------------------
-- Table: nx_data_aggregation
-- Defines aggregation rules from child to parent assets
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS nx_data_aggregation (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    module_key VARCHAR(50) NOT NULL,
    aggregation_name VARCHAR(255) NOT NULL,
    source_asset_type VARCHAR(100) NOT NULL,
    target_asset_type VARCHAR(100) NOT NULL,
    source_key VARCHAR(255) NOT NULL,
    target_key VARCHAR(255) NOT NULL,
    aggregation_type VARCHAR(50) NOT NULL,
    aggregation_window BIGINT DEFAULT 60000,
    filter_expression VARCHAR,
    is_active BOOLEAN DEFAULT TRUE,
    additional_info VARCHAR,
    CONSTRAINT nx_data_aggregation_pkey PRIMARY KEY (id),
    CONSTRAINT nx_data_aggregation_name_unq UNIQUE (tenant_id, aggregation_name),
    CONSTRAINT fk_nx_aggregation_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nx_aggregation_tenant ON nx_data_aggregation(tenant_id);
CREATE INDEX IF NOT EXISTS idx_nx_aggregation_module ON nx_data_aggregation(module_key);
CREATE INDEX IF NOT EXISTS idx_nx_aggregation_source_type ON nx_data_aggregation(source_asset_type);
CREATE INDEX IF NOT EXISTS idx_nx_aggregation_active ON nx_data_aggregation(tenant_id, is_active) WHERE is_active = TRUE;

-- -----------------------------------------------------
-- Table: nx_distribution_log
-- Audit log for data distribution operations
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS nx_distribution_log (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    device_id UUID,
    target_asset_id UUID,
    module_key VARCHAR(50),
    operation_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    keys_processed INTEGER DEFAULT 0,
    error_message TEXT,
    processing_time_ms BIGINT,
    CONSTRAINT nx_distribution_log_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_nx_dist_log_tenant_time ON nx_distribution_log(tenant_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_nx_dist_log_device ON nx_distribution_log(device_id);
CREATE INDEX IF NOT EXISTS idx_nx_dist_log_status ON nx_distribution_log(status);
CREATE INDEX IF NOT EXISTS idx_nx_dist_log_module ON nx_distribution_log(module_key);
