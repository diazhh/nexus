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
package org.thingsboard.nexus.po.dto.ml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.nexus.po.dto.HealthLevel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for ML prediction results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoMlPredictionDto {

    private UUID id;
    private UUID tenantId;
    private UUID wellAssetId;
    private String wellName;
    private UUID modelId;
    private PredictionType predictionType;

    // Failure Prediction
    private BigDecimal probability;
    private Integer daysToFailure;
    private BigDecimal confidence;

    // Anomaly Detection
    private Boolean isAnomaly;
    private BigDecimal anomalyScore;

    // Health Score
    private Integer healthScore;
    private HealthLevel healthLevel;
    private HealthTrend healthTrend;

    // Details
    private List<ContributingFactorDto> contributingFactors;
    private List<AnomalousFeatureDto> anomalousFeatures;
    private Map<String, Integer> componentScores;

    // Actions
    private Boolean alarmCreated;
    private UUID alarmId;
    private Boolean notificationSent;
    private Boolean workOrderCreated;
    private UUID workOrderId;

    // User Actions
    private Boolean acknowledged;
    private Boolean dismissed;
    private String dismissReason;

    private Long createdTime;

    /**
     * Contributing factor for failure prediction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributingFactorDto {
        private String feature;
        private Double currentValue;
        private Double threshold;
        private String impact;  // HIGH, MEDIUM, LOW
        private String trend;   // UP, DOWN, STABLE
        private String unit;
    }

    /**
     * Anomalous feature detected.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalousFeatureDto {
        private String feature;
        private Double currentValue;
        private Double expectedMin;
        private Double expectedMax;
        private Double deviationScore;
        private String unit;
    }
}
