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
import java.util.Map;
import java.util.UUID;

/**
 * KPI Data Transfer Object for drilling efficiency analysis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrillingEfficiencyKpiDto {

    /**
     * Entity identifier (run, well, or rig)
     */
    private UUID entityId;

    /**
     * Entity type (RUN, WELL, RIG)
     */
    private String entityType;

    // --- Overall Efficiency ---

    /**
     * Overall drilling efficiency percentage
     */
    private BigDecimal overallEfficiencyPercent;

    /**
     * Technical limit efficiency percentage
     */
    private BigDecimal technicalLimitEfficiencyPercent;

    /**
     * Invisible Lost Time (ILT) percentage
     */
    private BigDecimal iltPercent;

    // --- Time Breakdown ---

    /**
     * Total time analyzed (hours)
     */
    private BigDecimal totalTimeHours;

    /**
     * Productive time (hours)
     */
    private BigDecimal productiveTimeHours;

    /**
     * Productive time percentage
     */
    private BigDecimal productiveTimePercent;

    /**
     * NPT hours
     */
    private BigDecimal nptHours;

    /**
     * NPT percentage
     */
    private BigDecimal nptPercent;

    /**
     * Flat time hours (non-drilling productive time)
     */
    private BigDecimal flatTimeHours;

    /**
     * Flat time percentage
     */
    private BigDecimal flatTimePercent;

    // --- Activity Breakdown ---

    /**
     * Time breakdown by activity
     */
    private Map<String, BigDecimal> timeByActivity;

    /**
     * Percentage breakdown by activity
     */
    private Map<String, BigDecimal> percentByActivity;

    /**
     * Detailed activity breakdown
     */
    private List<ActivityBreakdown> activityBreakdown;

    // --- Drilling Efficiency ---

    /**
     * On-bottom drilling time (hours)
     */
    private BigDecimal onBottomDrillingHours;

    /**
     * On-bottom drilling percentage
     */
    private BigDecimal onBottomDrillingPercent;

    /**
     * Rotating hours
     */
    private BigDecimal rotatingHours;

    /**
     * Sliding hours
     */
    private BigDecimal slidingHours;

    /**
     * Rotating vs sliding ratio
     */
    private BigDecimal rotatingToSlidingRatio;

    // --- ROP Efficiency ---

    /**
     * Actual average ROP (ft/hr)
     */
    private BigDecimal actualAvgRopFtHr;

    /**
     * Technical limit ROP (ft/hr)
     */
    private BigDecimal technicalLimitRopFtHr;

    /**
     * ROP efficiency percentage
     */
    private BigDecimal ropEfficiencyPercent;

    /**
     * Footage that could have been drilled at technical limit
     */
    private BigDecimal potentialAdditionalFootageFt;

    // --- Connection Efficiency ---

    /**
     * Connection efficiency percentage
     */
    private BigDecimal connectionEfficiencyPercent;

    /**
     * Time lost in connections vs benchmark (hours)
     */
    private BigDecimal connectionTimeLostHours;

    // --- Trip Efficiency ---

    /**
     * Trip efficiency percentage
     */
    private BigDecimal tripEfficiencyPercent;

    /**
     * Actual trip speed (ft/hr)
     */
    private BigDecimal actualTripSpeedFtHr;

    /**
     * Benchmark trip speed (ft/hr)
     */
    private BigDecimal benchmarkTripSpeedFtHr;

    /**
     * Time lost in tripping (hours)
     */
    private BigDecimal tripTimeLostHours;

    // --- NPT Analysis ---

    /**
     * NPT breakdown by category
     */
    private List<NptBreakdown> nptBreakdown;

    /**
     * Top NPT categories
     */
    private List<String> topNptCategories;

    /**
     * Preventable NPT hours
     */
    private BigDecimal preventableNptHours;

    /**
     * Preventable NPT percentage
     */
    private BigDecimal preventableNptPercent;

    // --- ILT Analysis ---

    /**
     * ILT breakdown
     */
    private List<IltBreakdown> iltBreakdown;

    /**
     * Total ILT hours
     */
    private BigDecimal totalIltHours;

    /**
     * ILT in connections (hours)
     */
    private BigDecimal iltConnectionsHours;

    /**
     * ILT in tripping (hours)
     */
    private BigDecimal iltTrippingHours;

    /**
     * ILT in drilling (hours)
     */
    private BigDecimal iltDrillingHours;

    // --- Benchmark Comparison ---

    /**
     * Offset well efficiency percentage
     */
    private BigDecimal offsetWellEfficiencyPercent;

    /**
     * Field average efficiency percentage
     */
    private BigDecimal fieldAvgEfficiencyPercent;

    /**
     * Best in class efficiency percentage
     */
    private BigDecimal bestInClassEfficiencyPercent;

    /**
     * Quartile ranking (1-4)
     */
    private Integer quartileRanking;

    // --- Improvement Opportunities ---

    /**
     * Top improvement opportunities
     */
    private List<ImprovementOpportunity> improvementOpportunities;

    /**
     * Potential time savings (hours)
     */
    private BigDecimal potentialTimeSavingsHours;

    /**
     * Potential cost savings ($)
     */
    private BigDecimal potentialCostSavings;

    /**
     * Nested class for activity breakdown
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityBreakdown {
        private String activityCode;
        private String activityName;
        private String category;
        private BigDecimal hours;
        private BigDecimal percentage;
        private Boolean isProductive;
    }

    /**
     * Nested class for NPT breakdown
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NptBreakdown {
        private String category;
        private String subCategory;
        private BigDecimal hours;
        private BigDecimal percentage;
        private Integer occurrences;
        private Boolean isPreventable;
    }

    /**
     * Nested class for ILT breakdown
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IltBreakdown {
        private String activity;
        private BigDecimal actualHours;
        private BigDecimal benchmarkHours;
        private BigDecimal iltHours;
        private BigDecimal iltPercent;
    }

    /**
     * Nested class for improvement opportunities
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImprovementOpportunity {
        private String area;
        private String description;
        private BigDecimal potentialSavingsHours;
        private BigDecimal potentialSavingsCost;
        private String priority;
        private String recommendation;
    }
}
