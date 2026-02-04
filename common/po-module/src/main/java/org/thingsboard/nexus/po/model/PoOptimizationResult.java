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
import org.thingsboard.nexus.po.dto.OptimizationResultDto.OptimizationRunStatus;
import org.thingsboard.nexus.po.dto.OptimizationType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA Entity for optimization results.
 *
 * This is a CUSTOM TABLE (not TB Core) because:
 * 1. Multiple versions per asset need to be tracked
 * 2. Complex queries for ML training
 * 3. Historical analysis requirements
 */
@Entity
@Table(name = "po_optimization_result", indexes = {
        @Index(name = "idx_po_opt_result_tenant", columnList = "tenant_id"),
        @Index(name = "idx_po_opt_result_asset", columnList = "asset_id"),
        @Index(name = "idx_po_opt_result_type", columnList = "optimization_type"),
        @Index(name = "idx_po_opt_result_timestamp", columnList = "timestamp DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoOptimizationResult {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "asset_id", nullable = false, columnDefinition = "uuid")
    private UUID assetId;

    @Column(name = "asset_type", length = 50)
    private String assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "optimization_type", nullable = false, length = 50)
    private OptimizationType optimizationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_status", length = 30)
    private OptimizationRunStatus runStatus;

    @Column(name = "algorithm", length = 100)
    private String algorithm;

    @Column(name = "algorithm_version", length = 20)
    private String algorithmVersion;

    @Type(JsonType.class)
    @Column(name = "input_parameters", columnDefinition = "jsonb")
    private JsonNode inputParameters;

    @Type(JsonType.class)
    @Column(name = "output_results", columnDefinition = "jsonb")
    private JsonNode outputResults;

    @Column(name = "optimal_value", precision = 18, scale = 6)
    private BigDecimal optimalValue;

    @Column(name = "optimal_value_unit", length = 20)
    private String optimalValueUnit;

    @Column(name = "objective_value", precision = 18, scale = 6)
    private BigDecimal objectiveValue;

    @Column(name = "iterations")
    private Integer iterations;

    @Column(name = "converged")
    private Boolean converged;

    @Column(name = "computation_time_ms")
    private Long computationTimeMs;

    @Column(name = "data_quality_score")
    private Double dataQualityScore;

    @Column(name = "data_window_start")
    private Long dataWindowStart;

    @Column(name = "data_window_end")
    private Long dataWindowEnd;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "triggered_by", columnDefinition = "uuid")
    private UUID triggeredBy;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;
}
