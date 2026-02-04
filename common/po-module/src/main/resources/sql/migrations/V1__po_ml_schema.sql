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
-- Production Optimization (PO) Module - ML Schema
-- Version: 1.0.0
-- Date: 2026-02-04
-- Description: Schema for ML/AI predictive analytics tables
-- ============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Table: po_ml_config
-- Description: ML configuration per tenant
-- ============================================================================
CREATE TABLE IF NOT EXISTS po_ml_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL UNIQUE,

    -- Failure Prediction Configuration
    failure_enabled BOOLEAN DEFAULT TRUE,
    failure_probability_threshold DECIMAL(5,4) DEFAULT 0.7000,
    failure_prediction_horizon_days INTEGER DEFAULT 30,
    failure_auto_create_alarm BOOLEAN DEFAULT TRUE,
    failure_auto_create_work_order BOOLEAN DEFAULT FALSE,

    -- Anomaly Detection Configuration
    anomaly_enabled BOOLEAN DEFAULT TRUE,
    anomaly_sensitivity VARCHAR(20) DEFAULT 'MEDIUM',
    anomaly_score_threshold DECIMAL(5,4) DEFAULT 0.8000,
    anomaly_auto_create_alarm BOOLEAN DEFAULT TRUE,

    -- Health Score Configuration
    health_score_enabled BOOLEAN DEFAULT TRUE,
    health_critical_threshold INTEGER DEFAULT 30,
    health_poor_threshold INTEGER DEFAULT 50,
    health_fair_threshold INTEGER DEFAULT 70,
    health_good_threshold INTEGER DEFAULT 85,

    -- Health Score Weights (must sum to 1.0)
    health_weight_pump DECIMAL(3,2) DEFAULT 0.30,
    health_weight_motor DECIMAL(3,2) DEFAULT 0.25,
    health_weight_production DECIMAL(3,2) DEFAULT 0.25,
    health_weight_maintenance DECIMAL(3,2) DEFAULT 0.20,

    -- Notification Configuration
    notification_enabled BOOLEAN DEFAULT TRUE,
    notification_recipients JSONB,

    -- Model Configuration
    auto_retrain_enabled BOOLEAN DEFAULT FALSE,
    retrain_interval_days INTEGER DEFAULT 30,
    last_retrain_time BIGINT,

    -- Metadata
    created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    updated_time BIGINT,

    CONSTRAINT chk_health_weights CHECK (
        health_weight_pump + health_weight_motor +
        health_weight_production + health_weight_maintenance = 1.00
    )
);

CREATE INDEX idx_po_ml_config_tenant ON po_ml_config(tenant_id);

-- ============================================================================
-- Table: po_ml_model
-- Description: ML model registry
-- ============================================================================
CREATE TABLE IF NOT EXISTS po_ml_model (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,

    -- Model Information
    name VARCHAR(255) NOT NULL,
    model_type VARCHAR(50) NOT NULL, -- FAILURE_PREDICTION, ANOMALY_DETECTION, HEALTH_SCORE
    lift_system_type VARCHAR(50), -- ESP, BEAM, GAS_LIFT, PCP, etc.
    version VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TRAINING', -- TRAINING, ACTIVE, ARCHIVED, FAILED

    -- Performance Metrics
    accuracy DECIMAL(5,4),
    precision_score DECIMAL(5,4),
    recall DECIMAL(5,4),
    f1_score DECIMAL(5,4),
    auc_roc DECIMAL(5,4),

    -- Training Information
    training_start_time BIGINT,
    training_end_time BIGINT,
    training_samples INTEGER,
    failure_events INTEGER,
    wells_count INTEGER,

    -- Hyperparameters
    hyperparameters JSONB,

    -- Feature Importance
    feature_importance JSONB,

    -- Storage
    model_path VARCHAR(500),
    mlflow_run_id VARCHAR(100),

    -- Audit
    created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    created_by UUID,

    CONSTRAINT uk_po_ml_model_version UNIQUE (tenant_id, name, version)
);

CREATE INDEX idx_po_ml_model_tenant ON po_ml_model(tenant_id);
CREATE INDEX idx_po_ml_model_type ON po_ml_model(model_type);
CREATE INDEX idx_po_ml_model_status ON po_ml_model(status);
CREATE INDEX idx_po_ml_model_lift_system ON po_ml_model(lift_system_type);
CREATE INDEX idx_po_ml_model_created ON po_ml_model(created_time DESC);

-- ============================================================================
-- Table: po_ml_prediction
-- Description: ML prediction results
-- ============================================================================
CREATE TABLE IF NOT EXISTS po_ml_prediction (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    well_asset_id UUID NOT NULL,
    model_id UUID,

    -- Prediction Type
    prediction_type VARCHAR(50) NOT NULL, -- FAILURE, ANOMALY, HEALTH_SCORE

    -- Failure Prediction Results
    probability DECIMAL(5,4),
    days_to_failure INTEGER,
    confidence DECIMAL(5,4),

    -- Anomaly Detection Results
    is_anomaly BOOLEAN DEFAULT FALSE,
    anomaly_score DECIMAL(5,4),

    -- Health Score Results
    health_score INTEGER,
    health_level VARCHAR(20), -- CRITICAL, POOR, FAIR, GOOD, EXCELLENT
    health_trend VARCHAR(20), -- IMPROVING, STABLE, DEGRADING

    -- Detailed Results
    contributing_factors JSONB,
    anomalous_features JSONB,
    component_scores JSONB,

    -- Actions Taken
    alarm_created BOOLEAN DEFAULT FALSE,
    alarm_id UUID,
    notification_sent BOOLEAN DEFAULT FALSE,
    work_order_created BOOLEAN DEFAULT FALSE,
    work_order_id UUID,

    -- User Actions
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by UUID,
    acknowledged_time BIGINT,
    dismissed BOOLEAN DEFAULT FALSE,
    dismissed_by UUID,
    dismiss_reason VARCHAR(500),

    -- Timestamp
    created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,

    CONSTRAINT fk_po_ml_prediction_model FOREIGN KEY (model_id)
        REFERENCES po_ml_model(id) ON DELETE SET NULL
);

CREATE INDEX idx_po_ml_prediction_tenant ON po_ml_prediction(tenant_id);
CREATE INDEX idx_po_ml_prediction_well ON po_ml_prediction(well_asset_id, created_time DESC);
CREATE INDEX idx_po_ml_prediction_type ON po_ml_prediction(prediction_type, created_time DESC);
CREATE INDEX idx_po_ml_prediction_model ON po_ml_prediction(model_id);
CREATE INDEX idx_po_ml_prediction_created ON po_ml_prediction(created_time DESC);
CREATE INDEX idx_po_ml_prediction_high_risk ON po_ml_prediction(probability DESC)
    WHERE prediction_type = 'FAILURE' AND probability >= 0.7;
CREATE INDEX idx_po_ml_prediction_anomaly ON po_ml_prediction(is_anomaly)
    WHERE prediction_type = 'ANOMALY' AND is_anomaly = TRUE;
CREATE INDEX idx_po_ml_prediction_pending ON po_ml_prediction(acknowledged, dismissed)
    WHERE acknowledged = FALSE AND dismissed = FALSE;

-- ============================================================================
-- Table: po_ml_training_job
-- Description: ML training job tracking
-- ============================================================================
CREATE TABLE IF NOT EXISTS po_ml_training_job (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,

    -- Job Information
    model_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    -- Training Configuration
    data_start_date DATE,
    data_end_date DATE,
    hyperparameters JSONB,

    -- Progress Tracking
    progress_percent INTEGER DEFAULT 0,
    current_epoch INTEGER,
    total_epochs INTEGER,
    current_step VARCHAR(100),

    -- Results
    result_model_id UUID,
    error_message VARCHAR(2000),

    -- Timestamps
    started_time BIGINT,
    completed_time BIGINT,
    created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    created_by UUID,

    CONSTRAINT fk_po_ml_job_result_model FOREIGN KEY (result_model_id)
        REFERENCES po_ml_model(id) ON DELETE SET NULL
);

CREATE INDEX idx_po_ml_job_tenant ON po_ml_training_job(tenant_id);
CREATE INDEX idx_po_ml_job_status ON po_ml_training_job(status);
CREATE INDEX idx_po_ml_job_created ON po_ml_training_job(created_time DESC);
CREATE INDEX idx_po_ml_job_running ON po_ml_training_job(status)
    WHERE status IN ('PENDING', 'RUNNING');

-- ============================================================================
-- Table: po_ml_feature_stats
-- Description: Feature statistics for model training
-- ============================================================================
CREATE TABLE IF NOT EXISTS po_ml_feature_stats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    model_id UUID,
    well_asset_id UUID,

    -- Feature Information
    feature_name VARCHAR(100) NOT NULL,
    feature_type VARCHAR(50), -- SENSOR, DERIVED, CATEGORICAL

    -- Statistics
    sample_count BIGINT,
    mean_value DECIMAL(18,6),
    std_dev DECIMAL(18,6),
    min_value DECIMAL(18,6),
    max_value DECIMAL(18,6),
    percentile_5 DECIMAL(18,6),
    percentile_25 DECIMAL(18,6),
    percentile_50 DECIMAL(18,6),
    percentile_75 DECIMAL(18,6),
    percentile_95 DECIMAL(18,6),

    -- For categorical features
    value_counts JSONB,

    -- Calculation metadata
    calculation_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    data_start_time BIGINT,
    data_end_time BIGINT,

    CONSTRAINT uk_po_ml_feature_stats UNIQUE (tenant_id, model_id, well_asset_id, feature_name),
    CONSTRAINT fk_po_ml_feature_stats_model FOREIGN KEY (model_id)
        REFERENCES po_ml_model(id) ON DELETE CASCADE
);

CREATE INDEX idx_po_ml_feature_stats_tenant ON po_ml_feature_stats(tenant_id);
CREATE INDEX idx_po_ml_feature_stats_model ON po_ml_feature_stats(model_id);
CREATE INDEX idx_po_ml_feature_stats_well ON po_ml_feature_stats(well_asset_id);

-- ============================================================================
-- Helper Functions
-- ============================================================================

-- Function to get the active model for a tenant and type
CREATE OR REPLACE FUNCTION po_get_active_model(
    p_tenant_id UUID,
    p_model_type VARCHAR(50)
) RETURNS po_ml_model AS $$
DECLARE
    v_model po_ml_model%ROWTYPE;
BEGIN
    SELECT * INTO v_model
    FROM po_ml_model
    WHERE tenant_id = p_tenant_id
      AND model_type = p_model_type
      AND status = 'ACTIVE'
    ORDER BY created_time DESC
    LIMIT 1;

    RETURN v_model;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate prediction statistics for a tenant
CREATE OR REPLACE FUNCTION po_get_prediction_stats(
    p_tenant_id UUID,
    p_days INTEGER DEFAULT 7
) RETURNS TABLE (
    prediction_type VARCHAR(50),
    total_count BIGINT,
    high_risk_count BIGINT,
    anomaly_count BIGINT,
    pending_action_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.prediction_type,
        COUNT(*)::BIGINT AS total_count,
        COUNT(*) FILTER (WHERE p.probability >= 0.7)::BIGINT AS high_risk_count,
        COUNT(*) FILTER (WHERE p.is_anomaly = TRUE)::BIGINT AS anomaly_count,
        COUNT(*) FILTER (WHERE p.acknowledged = FALSE AND p.dismissed = FALSE)::BIGINT AS pending_action_count
    FROM po_ml_prediction p
    WHERE p.tenant_id = p_tenant_id
      AND p.created_time >= (EXTRACT(EPOCH FROM NOW()) * 1000 - p_days * 86400000)
    GROUP BY p.prediction_type;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Data Retention Policy (cleanup old predictions)
-- This can be called by a scheduled job
-- ============================================================================

CREATE OR REPLACE FUNCTION po_cleanup_old_predictions(
    p_retention_days INTEGER DEFAULT 90
) RETURNS INTEGER AS $$
DECLARE
    v_deleted INTEGER;
    v_cutoff_time BIGINT;
BEGIN
    v_cutoff_time := (EXTRACT(EPOCH FROM NOW()) * 1000) - (p_retention_days * 86400000);

    DELETE FROM po_ml_prediction
    WHERE created_time < v_cutoff_time
      AND acknowledged = TRUE;

    GET DIAGNOSTICS v_deleted = ROW_COUNT;

    RETURN v_deleted;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON TABLE po_ml_config IS 'ML/AI configuration per tenant for Production Optimization';
COMMENT ON TABLE po_ml_model IS 'Registry of trained ML models with performance metrics';
COMMENT ON TABLE po_ml_prediction IS 'Prediction results from ML models including failure, anomaly, and health scores';
COMMENT ON TABLE po_ml_training_job IS 'Tracking of ML model training jobs';
COMMENT ON TABLE po_ml_feature_stats IS 'Feature statistics used for model training and normalization';

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================
