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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for ML configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoMlConfigDto {

    private UUID id;
    private UUID tenantId;

    // Failure Prediction
    private BigDecimal failureAlertThreshold;
    private Integer predictionHorizonDays;
    private Integer analysisFrequencyHours;
    private Integer lookbackDays;

    // Anomaly Detection
    private BigDecimal anomalyContamination;
    private Integer anomalyWindowHours;
    private List<String> anomalyFeatures;

    // Health Score Weights
    private BigDecimal healthWeightMechanical;
    private BigDecimal healthWeightElectrical;
    private BigDecimal healthWeightProduction;
    private BigDecimal healthWeightThermal;

    // Health Score Thresholds
    private Integer healthThresholdHealthy;
    private Integer healthThresholdWarning;
    private Integer healthThresholdAtRisk;

    // Auto Actions
    private Boolean autoEmailEnabled;
    private BigDecimal autoEmailThreshold;
    private Boolean autoAlarmEnabled;
    private Boolean autoWorkOrderEnabled;
    private Boolean autoPushNotificationEnabled;

    // Timestamps
    private Long createdTime;
    private Long updatedTime;
}
