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
package org.thingsboard.nexus.po.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for equipment health score.
 * Health scores are stored as TB SERVER_SCOPE attributes on the asset.
 *
 * Attribute pattern (NO custom table):
 * - health_score: Overall health score (0.0 - 1.0)
 * - health_level: EXCELLENT, GOOD, FAIR, POOR, CRITICAL
 * - failure_probability: Probability of failure (0.0 - 1.0)
 * - remaining_useful_life_days: Estimated days until failure
 * - health_factors: JSON map of contributing factors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreDto {

    // Attribute key constants for TB storage
    public static final String ATTR_HEALTH_SCORE = "health_score";
    public static final String ATTR_HEALTH_LEVEL = "health_level";
    public static final String ATTR_FAILURE_PROBABILITY = "failure_probability";
    public static final String ATTR_REMAINING_USEFUL_LIFE = "remaining_useful_life_days";
    public static final String ATTR_HEALTH_FACTORS = "health_factors";
    public static final String ATTR_HEALTH_UPDATED_AT = "health_updated_at";

    /**
     * Asset ID (well or lift system)
     */
    private UUID assetId;

    /**
     * Tenant ID
     */
    private UUID tenantId;

    /**
     * Asset type (pf_well, pf_esp_system, etc.)
     */
    private String assetType;

    /**
     * Asset name for display
     */
    private String assetName;

    /**
     * Overall health score (0.0 to 1.0)
     */
    private double score;

    /**
     * Health level classification
     */
    private HealthLevel level;

    /**
     * Probability of failure in next 30 days (0.0 to 1.0)
     */
    private double failureProbability;

    /**
     * Estimated remaining useful life in days
     */
    private Integer remainingUsefulLifeDays;

    /**
     * Contributing factors to health score
     * Key: factor name, Value: factor score (0.0 to 1.0)
     */
    private Map<String, Double> factors;

    /**
     * Issues detected that affect health
     */
    private List<HealthIssue> issues;

    /**
     * Timestamp when health was calculated
     */
    private Long calculatedAt;

    /**
     * Trend compared to previous calculation
     */
    private HealthTrend trend;

    /**
     * Previous score for comparison
     */
    private Double previousScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthIssue {
        private String code;
        private String description;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String recommendation;
        private Double impact; // Impact on health score
    }

    public enum HealthTrend {
        IMPROVING,
        STABLE,
        DECLINING,
        UNKNOWN
    }

    /**
     * Get health level from score.
     */
    public HealthLevel getLevel() {
        if (level != null) return level;
        return HealthLevel.fromScore(score);
    }
}
