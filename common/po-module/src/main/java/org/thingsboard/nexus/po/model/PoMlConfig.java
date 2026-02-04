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
package org.thingsboard.nexus.po.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA Entity for ML configuration per tenant.
 *
 * Stores configuration settings for:
 * - Failure prediction thresholds
 * - Anomaly detection parameters
 * - Health score weights and thresholds
 * - Automatic actions
 */
@Entity
@Table(name = "po_ml_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoMlConfig {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID tenantId;

    // Failure Prediction Configuration
    @Column(name = "failure_alert_threshold", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal failureAlertThreshold = new BigDecimal("60.00");

    @Column(name = "prediction_horizon_days")
    @Builder.Default
    private Integer predictionHorizonDays = 14;

    @Column(name = "analysis_frequency_hours")
    @Builder.Default
    private Integer analysisFrequencyHours = 1;

    @Column(name = "lookback_days")
    @Builder.Default
    private Integer lookbackDays = 7;

    // Anomaly Detection Configuration
    @Column(name = "anomaly_contamination", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal anomalyContamination = new BigDecimal("0.0500");

    @Column(name = "anomaly_window_hours")
    @Builder.Default
    private Integer anomalyWindowHours = 24;

    @Type(JsonType.class)
    @Column(name = "anomaly_features", columnDefinition = "jsonb")
    private JsonNode anomalyFeatures;

    // Health Score Weights
    @Column(name = "health_weight_mechanical", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal healthWeightMechanical = new BigDecimal("0.40");

    @Column(name = "health_weight_electrical", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal healthWeightElectrical = new BigDecimal("0.35");

    @Column(name = "health_weight_production", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal healthWeightProduction = new BigDecimal("0.15");

    @Column(name = "health_weight_thermal", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal healthWeightThermal = new BigDecimal("0.10");

    // Health Score Thresholds
    @Column(name = "health_threshold_healthy")
    @Builder.Default
    private Integer healthThresholdHealthy = 80;

    @Column(name = "health_threshold_warning")
    @Builder.Default
    private Integer healthThresholdWarning = 60;

    @Column(name = "health_threshold_at_risk")
    @Builder.Default
    private Integer healthThresholdAtRisk = 40;

    // Automatic Actions
    @Column(name = "auto_email_enabled")
    @Builder.Default
    private Boolean autoEmailEnabled = true;

    @Column(name = "auto_email_threshold", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal autoEmailThreshold = new BigDecimal("70.00");

    @Column(name = "auto_alarm_enabled")
    @Builder.Default
    private Boolean autoAlarmEnabled = true;

    @Column(name = "auto_work_order_enabled")
    @Builder.Default
    private Boolean autoWorkOrderEnabled = false;

    @Column(name = "auto_push_notification_enabled")
    @Builder.Default
    private Boolean autoPushNotificationEnabled = false;

    // Timestamps
    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @Column(name = "updated_time")
    private Long updatedTime;
}
