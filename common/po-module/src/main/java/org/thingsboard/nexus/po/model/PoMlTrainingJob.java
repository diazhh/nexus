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
import org.thingsboard.nexus.po.dto.ml.TrainingJobStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity for ML training job tracking.
 *
 * Tracks the status and progress of model training jobs.
 */
@Entity
@Table(name = "po_ml_training_job",
        indexes = {
                @Index(name = "idx_ml_job_tenant", columnList = "tenant_id"),
                @Index(name = "idx_ml_job_status", columnList = "status"),
                @Index(name = "idx_ml_job_created", columnList = "created_time DESC")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoMlTrainingJob {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TrainingJobStatus status = TrainingJobStatus.PENDING;

    // Training Configuration
    @Column(name = "data_start_date")
    private LocalDate dataStartDate;

    @Column(name = "data_end_date")
    private LocalDate dataEndDate;

    @Type(JsonType.class)
    @Column(name = "hyperparameters", columnDefinition = "jsonb")
    private JsonNode hyperparameters;

    // Progress Tracking
    @Column(name = "progress_percent")
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "current_epoch")
    private Integer currentEpoch;

    @Column(name = "total_epochs")
    private Integer totalEpochs;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    // Results
    @Column(name = "result_model_id", columnDefinition = "uuid")
    private UUID resultModelId;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    // Timestamps
    @Column(name = "started_time")
    private Long startedTime;

    @Column(name = "completed_time")
    private Long completedTime;

    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;
}
