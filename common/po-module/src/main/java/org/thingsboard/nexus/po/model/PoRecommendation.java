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
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA Entity for optimization recommendations.
 *
 * This is a CUSTOM TABLE (not TB Core) because:
 * 1. Complex workflow states (PENDING -> APPROVED -> EXECUTED)
 * 2. Audit trail required for compliance
 * 3. Complex business logic and queries
 */
@Entity
@Table(name = "po_recommendation", indexes = {
        @Index(name = "idx_po_rec_tenant", columnList = "tenant_id"),
        @Index(name = "idx_po_rec_asset", columnList = "asset_id"),
        @Index(name = "idx_po_rec_status", columnList = "status"),
        @Index(name = "idx_po_rec_type", columnList = "optimization_type"),
        @Index(name = "idx_po_rec_created", columnList = "created_time DESC"),
        @Index(name = "idx_po_rec_expiry", columnList = "expiry_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoRecommendation {

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
    @Column(name = "status", nullable = false, length = 20)
    private RecommendationStatus status;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "current_value", precision = 18, scale = 6)
    private BigDecimal currentValue;

    @Column(name = "recommended_value", precision = 18, scale = 6)
    private BigDecimal recommendedValue;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "expected_production_increase", precision = 18, scale = 6)
    private BigDecimal expectedProductionIncrease;

    @Column(name = "expected_production_increase_pct", precision = 8, scale = 4)
    private BigDecimal expectedProductionIncreasePercent;

    @Column(name = "expected_cost_savings", precision = 18, scale = 2)
    private BigDecimal expectedCostSavings;

    @Column(name = "expected_efficiency_improvement", precision = 8, scale = 4)
    private BigDecimal expectedEfficiencyImprovement;

    @Column(name = "confidence")
    private Double confidence;

    @Type(JsonType.class)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private JsonNode parameters;

    @Column(name = "optimization_result_id", columnDefinition = "uuid")
    private UUID optimizationResultId;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "approved_by", columnDefinition = "uuid")
    private UUID approvedBy;

    @Column(name = "executed_by", columnDefinition = "uuid")
    private UUID executedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @Column(name = "approved_time")
    private Long approvedTime;

    @Column(name = "executed_time")
    private Long executedTime;

    @Column(name = "expiry_time")
    private Long expiryTime;

    @Column(name = "notes", length = 2000)
    private String notes;
}
