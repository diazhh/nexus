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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for optimization recommendation.
 * Recommendations are stored in custom table po_recommendation because:
 * 1. They have complex workflow states (PENDING -> APPROVED -> EXECUTED)
 * 2. They require audit trail for compliance
 * 3. They need complex queries for analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {

    /**
     * Recommendation ID
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
     * Asset type (pf_well, pf_esp_system, etc.)
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
     * Current status in workflow
     */
    private RecommendationStatus status;

    /**
     * Priority level (1=highest, 5=lowest)
     */
    private Integer priority;

    /**
     * Human-readable title
     */
    @NotBlank(message = "Title is required")
    private String title;

    /**
     * Detailed description of the recommendation
     */
    private String description;

    /**
     * Current value before optimization
     */
    private BigDecimal currentValue;

    /**
     * Recommended new value
     */
    private BigDecimal recommendedValue;

    /**
     * Unit of measurement (Hz, BPD, MSCF/D, etc.)
     */
    private String unit;

    /**
     * Expected production increase (BPD)
     */
    private BigDecimal expectedProductionIncrease;

    /**
     * Expected production increase percentage
     */
    private BigDecimal expectedProductionIncreasePercent;

    /**
     * Expected cost savings (USD/day)
     */
    private BigDecimal expectedCostSavings;

    /**
     * Expected efficiency improvement percentage
     */
    private BigDecimal expectedEfficiencyImprovement;

    /**
     * Confidence level of the recommendation (0.0 to 1.0)
     */
    private Double confidence;

    /**
     * Additional parameters as JSON
     */
    private JsonNode parameters;

    /**
     * Reference to optimization result that generated this recommendation
     */
    private UUID optimizationResultId;

    /**
     * User who created the recommendation (or null if system-generated)
     */
    private UUID createdBy;

    /**
     * User who approved the recommendation
     */
    private UUID approvedBy;

    /**
     * User who executed the recommendation
     */
    private UUID executedBy;

    /**
     * Rejection reason (if rejected)
     */
    private String rejectionReason;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Approval timestamp
     */
    private Long approvedTime;

    /**
     * Execution timestamp
     */
    private Long executedTime;

    /**
     * Expiry timestamp
     */
    private Long expiryTime;

    /**
     * Notes or comments
     */
    private String notes;
}
