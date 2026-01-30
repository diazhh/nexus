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
 * KPI Data Transfer Object for a well.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WellKpiDto {

    /**
     * Well identifier
     */
    private UUID wellId;

    /**
     * Well name
     */
    private String wellName;

    /**
     * Field identifier
     */
    private UUID fieldId;

    /**
     * Field name
     */
    private String fieldName;

    // --- Overall Well KPIs ---

    /**
     * Total measured depth (ft)
     */
    private BigDecimal totalDepthFt;

    /**
     * Total true vertical depth (ft)
     */
    private BigDecimal totalTvdFt;

    /**
     * Total days to drill
     */
    private BigDecimal totalDrillingDays;

    /**
     * Spud date (epoch ms)
     */
    private Long spudDate;

    /**
     * Completion date (epoch ms)
     */
    private Long completionDate;

    // --- Section KPIs ---

    /**
     * Number of hole sections
     */
    private Integer sectionCount;

    /**
     * KPIs per section
     */
    private List<SectionKpiDto> sectionKpis;

    // --- Run Statistics ---

    /**
     * Total number of runs
     */
    private Integer totalRuns;

    /**
     * Total footage drilled (ft)
     */
    private BigDecimal totalFootageDrilledFt;

    // --- Time Distribution ---

    /**
     * Total drilling hours
     */
    private BigDecimal totalDrillingHours;

    /**
     * Total tripping hours
     */
    private BigDecimal totalTrippingHours;

    /**
     * Total circulating hours
     */
    private BigDecimal totalCirculatingHours;

    /**
     * Total casing/cementing hours
     */
    private BigDecimal totalCasingCementingHours;

    /**
     * Total NPT hours
     */
    private BigDecimal totalNptHours;

    /**
     * Total waiting on weather hours
     */
    private BigDecimal totalWowHours;

    // --- Performance KPIs ---

    /**
     * Overall average ROP (ft/hr)
     */
    private BigDecimal overallAvgRopFtHr;

    /**
     * Best 24-hour footage
     */
    private BigDecimal best24HourFootage;

    /**
     * Best single run footage
     */
    private BigDecimal bestRunFootage;

    /**
     * Average connection time (minutes)
     */
    private BigDecimal avgConnectionTimeMin;

    /**
     * Total connections made
     */
    private Integer totalConnections;

    // --- Directional KPIs ---

    /**
     * Maximum inclination reached (degrees)
     */
    private BigDecimal maxInclinationDeg;

    /**
     * Final azimuth (degrees)
     */
    private BigDecimal finalAzimuthDeg;

    /**
     * Horizontal displacement (ft)
     */
    private BigDecimal horizontalDisplacementFt;

    /**
     * Total surveys taken
     */
    private Integer totalSurveys;

    /**
     * Maximum DLS encountered (deg/100ft)
     */
    private BigDecimal maxDlsDegPer100ft;

    // --- Efficiency KPIs ---

    /**
     * Overall drilling efficiency percentage
     */
    private BigDecimal drillingEfficiencyPercent;

    /**
     * NPT percentage
     */
    private BigDecimal nptPercent;

    /**
     * Productive time percentage
     */
    private BigDecimal productiveTimePercent;

    /**
     * Days ahead/behind plan
     */
    private BigDecimal daysVsPlan;

    // --- Cost KPIs ---

    /**
     * Total well cost ($)
     */
    private BigDecimal totalCost;

    /**
     * Cost per foot ($/ft)
     */
    private BigDecimal costPerFoot;

    /**
     * Cost per day ($/day)
     */
    private BigDecimal costPerDay;

    /**
     * Cost vs AFE percentage
     */
    private BigDecimal costVsAfePercent;

    // --- BHA Statistics ---

    /**
     * Total BHAs used
     */
    private Integer totalBhasUsed;

    /**
     * Total bits used
     */
    private Integer totalBitsUsed;

    /**
     * Average footage per bit (ft)
     */
    private BigDecimal avgFootagePerBit;

    // --- Mud Log Summary ---

    /**
     * Total mud log samples
     */
    private Integer totalMudLogSamples;

    /**
     * Formations encountered
     */
    private List<String> formationsEncountered;

    /**
     * Maximum gas reading (units)
     */
    private BigDecimal maxGasUnits;

    // --- Safety KPIs ---

    /**
     * Total safety observations
     */
    private Integer totalSafetyObservations;

    /**
     * Recordable incidents
     */
    private Integer recordableIncidents;

    /**
     * Near misses
     */
    private Integer nearMisses;

    /**
     * Nested class for section-level KPIs
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SectionKpiDto {
        private String sectionName;
        private BigDecimal holeSizeIn;
        private BigDecimal startDepthFt;
        private BigDecimal endDepthFt;
        private BigDecimal footageFt;
        private BigDecimal drillingHours;
        private BigDecimal avgRopFtHr;
        private Integer runsCount;
        private BigDecimal nptHours;
    }
}
