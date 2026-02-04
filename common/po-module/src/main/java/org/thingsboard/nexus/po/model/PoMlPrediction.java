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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.thingsboard.nexus.po.dto.HealthLevel;
import org.thingsboard.nexus.po.dto.ml.HealthTrend;
import org.thingsboard.nexus.po.dto.ml.PredictionType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA Entity for ML prediction results.
 *
 * Stores predictions for:
 * - Failure probability
 * - Anomaly detection
 * - Health scores
 */
@Entity
@Table(name = "po_ml_prediction",
        indexes = {
                @Index(name = "idx_ml_pred_tenant", columnList = "tenant_id"),
                @Index(name = "idx_ml_pred_well", columnList = "well_asset_id, created_time DESC"),
                @Index(name = "idx_ml_pred_type", columnList = "prediction_type, created_time DESC"),
                @Index(name = "idx_ml_pred_model", columnList = "model_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoMlPrediction {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "well_asset_id", nullable = false, columnDefinition = "uuid")
    private UUID wellAssetId;

    @Column(name = "model_id", columnDefinition = "uuid")
    private UUID modelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_type", nullable = false, length = 50)
    private PredictionType predictionType;

    // Failure Prediction Results
    @Column(name = "probability", precision = 5, scale = 4)
    private BigDecimal probability;

    @Column(name = "days_to_failure")
    private Integer daysToFailure;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    // Anomaly Detection Results
    @Column(name = "is_anomaly")
    private Boolean isAnomaly;

    @Column(name = "anomaly_score", precision = 5, scale = 4)
    private BigDecimal anomalyScore;

    // Health Score Results
    @Column(name = "health_score")
    private Integer healthScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_level", length = 20)
    private HealthLevel healthLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_trend", length = 20)
    private HealthTrend healthTrend;

    // Detailed Results
    @Type(JsonType.class)
    @Column(name = "contributing_factors", columnDefinition = "jsonb")
    private JsonNode contributingFactors;

    @Type(JsonType.class)
    @Column(name = "anomalous_features", columnDefinition = "jsonb")
    private JsonNode anomalousFeatures;

    @Type(JsonType.class)
    @Column(name = "component_scores", columnDefinition = "jsonb")
    private JsonNode componentScores;

    // Actions Taken
    @Column(name = "alarm_created")
    @Builder.Default
    private Boolean alarmCreated = false;

    @Column(name = "alarm_id", columnDefinition = "uuid")
    private UUID alarmId;

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "work_order_created")
    @Builder.Default
    private Boolean workOrderCreated = false;

    @Column(name = "work_order_id", columnDefinition = "uuid")
    private UUID workOrderId;

    // User Actions
    @Column(name = "acknowledged")
    @Builder.Default
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by", columnDefinition = "uuid")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_time")
    private Long acknowledgedTime;

    @Column(name = "dismissed")
    @Builder.Default
    private Boolean dismissed = false;

    @Column(name = "dismissed_by", columnDefinition = "uuid")
    private UUID dismissedBy;

    @Column(name = "dismiss_reason", length = 500)
    private String dismissReason;

    // Timestamps
    @Column(name = "created_time", nullable = false)
    private Long createdTime;
}
