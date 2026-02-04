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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.thingsboard.nexus.po.dto.ml.MlModelStatus;
import org.thingsboard.nexus.po.dto.ml.MlModelType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA Entity for registered ML models.
 *
 * Tracks ML model versions, metrics, and deployment status.
 * Each model can have multiple versions, but only one can be ACTIVE per type.
 */
@Entity
@Table(name = "po_ml_model",
        indexes = {
                @Index(name = "idx_ml_model_tenant", columnList = "tenant_id"),
                @Index(name = "idx_ml_model_type", columnList = "model_type"),
                @Index(name = "idx_ml_model_status", columnList = "status"),
                @Index(name = "idx_ml_model_created", columnList = "created_time DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ml_model_version", columnNames = {"tenant_id", "name", "version"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoMlModel {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 50)
    private MlModelType modelType;

    @Column(name = "lift_system_type", length = 50)
    private String liftSystemType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MlModelStatus status = MlModelStatus.TRAINING;

    // Model Metrics
    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;

    @Column(name = "precision_score", precision = 5, scale = 4)
    private BigDecimal precisionScore;

    @Column(name = "recall", precision = 5, scale = 4)
    private BigDecimal recall;

    @Column(name = "f1_score", precision = 5, scale = 4)
    private BigDecimal f1Score;

    @Column(name = "auc_roc", precision = 5, scale = 4)
    private BigDecimal aucRoc;

    // Training Information
    @Column(name = "training_start_time")
    private Long trainingStartTime;

    @Column(name = "training_end_time")
    private Long trainingEndTime;

    @Column(name = "training_samples")
    private Integer trainingSamples;

    @Column(name = "failure_events")
    private Integer failureEvents;

    @Column(name = "wells_count")
    private Integer wellsCount;

    // Configuration
    @Type(JsonType.class)
    @Column(name = "hyperparameters", columnDefinition = "jsonb")
    private JsonNode hyperparameters;

    @Type(JsonType.class)
    @Column(name = "feature_importance", columnDefinition = "jsonb")
    private JsonNode featureImportance;

    // Storage
    @Column(name = "model_path", length = 500)
    private String modelPath;

    @Column(name = "mlflow_run_id", length = 100)
    private String mlflowRunId;

    // Audit
    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;
}
