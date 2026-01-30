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
package org.thingsboard.nexus.dr.dto.kpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * KPI Data Transfer Object for connection time analysis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionTimeKpiDto {

    /**
     * Entity identifier (run, well, or rig)
     */
    private UUID entityId;

    /**
     * Entity type (RUN, WELL, RIG)
     */
    private String entityType;

    // --- Summary Statistics ---

    /**
     * Total number of connections
     */
    private Integer totalConnections;

    /**
     * Total connection time (minutes)
     */
    private BigDecimal totalConnectionTimeMin;

    /**
     * Average connection time (minutes)
     */
    private BigDecimal avgConnectionTimeMin;

    /**
     * Median connection time (minutes)
     */
    private BigDecimal medianConnectionTimeMin;

    /**
     * Standard deviation (minutes)
     */
    private BigDecimal stdDevMin;

    /**
     * Best (minimum) connection time (minutes)
     */
    private BigDecimal bestConnectionTimeMin;

    /**
     * Worst (maximum) connection time (minutes)
     */
    private BigDecimal worstConnectionTimeMin;

    // --- Percentile Analysis ---

    /**
     * P10 connection time (minutes)
     */
    private BigDecimal p10Min;

    /**
     * P25 connection time (minutes)
     */
    private BigDecimal p25Min;

    /**
     * P50 connection time (minutes) - same as median
     */
    private BigDecimal p50Min;

    /**
     * P75 connection time (minutes)
     */
    private BigDecimal p75Min;

    /**
     * P90 connection time (minutes)
     */
    private BigDecimal p90Min;

    // --- Trend Analysis ---

    /**
     * Connection times over depth/time
     */
    private List<ConnectionDataPoint> connectionTrend;

    /**
     * Moving average window size used
     */
    private Integer movingAvgWindowSize;

    /**
     * Connection time trend (improving/stable/degrading)
     */
    private String trend;

    /**
     * Trend slope (negative = improving)
     */
    private BigDecimal trendSlope;

    // --- Breakdown by Phase ---

    /**
     * Average slips-to-slips time (minutes)
     */
    private BigDecimal avgSlipsToSlipsMin;

    /**
     * Average survey time (minutes)
     */
    private BigDecimal avgSurveyTimeMin;

    /**
     * Average circulation time (minutes)
     */
    private BigDecimal avgCirculationTimeMin;

    // --- Comparison ---

    /**
     * Target connection time (minutes)
     */
    private BigDecimal targetConnectionTimeMin;

    /**
     * Connections meeting target count
     */
    private Integer connectionsMeetingTarget;

    /**
     * Percentage of connections meeting target
     */
    private BigDecimal targetAchievementPercent;

    /**
     * Benchmark connection time (minutes)
     */
    private BigDecimal benchmarkConnectionTimeMin;

    /**
     * Performance vs benchmark percentage
     */
    private BigDecimal vsBenchmarkPercent;

    // --- Distribution Analysis ---

    /**
     * Connection time distribution buckets
     */
    private List<DistributionBucket> distribution;

    /**
     * Outlier threshold (minutes)
     */
    private BigDecimal outlierThresholdMin;

    /**
     * Number of outliers
     */
    private Integer outlierCount;

    // --- Time Savings ---

    /**
     * Potential time savings if all connections at P25 (hours)
     */
    private BigDecimal potentialSavingsAtP25Hours;

    /**
     * Potential time savings if all connections at best (hours)
     */
    private BigDecimal potentialSavingsAtBestHours;

    /**
     * Nested class for connection data points
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConnectionDataPoint {
        private Integer connectionNumber;
        private BigDecimal depthFt;
        private Long timestamp;
        private BigDecimal connectionTimeMin;
        private BigDecimal movingAvgMin;
    }

    /**
     * Nested class for distribution buckets
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DistributionBucket {
        private String range;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private Integer count;
        private BigDecimal percentage;
    }
}
