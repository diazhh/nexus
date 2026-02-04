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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for ML model information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoMlModelDto {

    private UUID id;
    private UUID tenantId;

    private String name;
    private MlModelType modelType;
    private String liftSystemType;
    private String version;
    private MlModelStatus status;

    // Metrics
    private BigDecimal accuracy;
    private BigDecimal precisionScore;
    private BigDecimal recall;
    private BigDecimal f1Score;
    private BigDecimal aucRoc;

    // Training Info
    private Long trainingStartTime;
    private Long trainingEndTime;
    private Integer trainingSamples;
    private Integer failureEvents;
    private Integer wellsCount;

    // Configuration
    private JsonNode hyperparameters;
    private Map<String, Double> featureImportance;

    // Storage
    private String modelPath;
    private String mlflowRunId;

    // Audit
    private Long createdTime;
    private UUID createdBy;
}
