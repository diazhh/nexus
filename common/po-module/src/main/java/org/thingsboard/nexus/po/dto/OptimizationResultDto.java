/*
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
package org.thingsboard.nexus.po.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for optimization run result.
 * Results are stored in custom table po_optimization_result because:
 * 1. Multiple versions per well need to be tracked
 * 2. Complex queries for ML training
 * 3. Historical analysis requirements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationResultDto {

    /**
     * Result ID
     */
    private UUID id;

    /**
     * Tenant ID
     */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    /**
     * Target asset ID (well or lift system)
     */
    @NotNull(message = "Asset ID is required")
    private UUID assetId;

    /**
     * Asset type
     */
    private String assetType;

    /**
     * Asset name for display
     */
    private String assetName;

    /**
     * Optimization type
     */
    @NotNull(message = "Optimization type is required")
    private OptimizationType type;

    /**
     * Optimization run status
     */
    private OptimizationRunStatus runStatus;

    /**
     * Algorithm used
     */
    private String algorithm;

    /**
     * Algorithm version
     */
    private String algorithmVersion;

    /**
     * Input parameters used for optimization
     */
    private JsonNode inputParameters;

    /**
     * Output results from optimization
     */
    private JsonNode outputResults;

    /**
     * Optimal value found
     */
    private BigDecimal optimalValue;

    /**
     * Unit of optimal value
     */
    private String optimalValueUnit;

    /**
     * Objective function value at optimum
     */
    private BigDecimal objectiveValue;

    /**
     * Number of iterations performed
     */
    private Integer iterations;

    /**
     * Convergence achieved
     */
    private Boolean converged;

    /**
     * Computation time in milliseconds
     */
    private Long computationTimeMs;

    /**
     * Data quality score of input data
     */
    private Double dataQualityScore;

    /**
     * Start time of data window used
     */
    private Long dataWindowStart;

    /**
     * End time of data window used
     */
    private Long dataWindowEnd;

    /**
     * Recommendations generated from this result
     */
    private List<RecommendationDto> recommendations;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * User who triggered the optimization (null if scheduled)
     */
    private UUID triggeredBy;

    /**
     * Timestamp when optimization was run
     */
    private Long timestamp;

    /**
     * Optimization run status enum
     */
    public enum OptimizationRunStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
        NO_DATA,
        LOW_DATA_QUALITY
    }
}
