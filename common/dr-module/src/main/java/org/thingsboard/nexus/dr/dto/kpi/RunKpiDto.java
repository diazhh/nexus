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
import java.util.UUID;

/**
 * KPI Data Transfer Object for a drilling run.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunKpiDto {

    /**
     * Run identifier
     */
    private UUID runId;

    /**
     * Run number
     */
    private Integer runNumber;

    /**
     * Well identifier
     */
    private UUID wellId;

    /**
     * Rig identifier
     */
    private UUID rigId;

    // --- Depth KPIs ---

    /**
     * Starting measured depth (ft)
     */
    private BigDecimal startDepthFt;

    /**
     * Ending measured depth (ft)
     */
    private BigDecimal endDepthFt;

    /**
     * Total footage drilled (ft)
     */
    private BigDecimal footageDrilledFt;

    /**
     * True Vertical Depth at end (ft)
     */
    private BigDecimal tvdFt;

    // --- Time KPIs ---

    /**
     * Run start time (epoch ms)
     */
    private Long startTime;

    /**
     * Run end time (epoch ms)
     */
    private Long endTime;

    /**
     * Total run duration (hours)
     */
    private BigDecimal totalHours;

    /**
     * Rotating hours
     */
    private BigDecimal rotatingHours;

    /**
     * Sliding hours
     */
    private BigDecimal slidingHours;

    /**
     * Circulating hours
     */
    private BigDecimal circulatingHours;

    /**
     * Trip in hours
     */
    private BigDecimal tripInHours;

    /**
     * Trip out hours
     */
    private BigDecimal tripOutHours;

    /**
     * Connection time (hours)
     */
    private BigDecimal connectionHours;

    /**
     * Non-Productive Time (hours)
     */
    private BigDecimal nptHours;

    // --- ROP KPIs ---

    /**
     * Average Rate of Penetration (ft/hr)
     */
    private BigDecimal avgRopFtHr;

    /**
     * Maximum ROP (ft/hr)
     */
    private BigDecimal maxRopFtHr;

    /**
     * Minimum ROP (ft/hr)
     */
    private BigDecimal minRopFtHr;

    /**
     * Rotating ROP (ft/hr)
     */
    private BigDecimal rotatingRopFtHr;

    /**
     * Sliding ROP (ft/hr)
     */
    private BigDecimal slidingRopFtHr;

    // --- Drilling Parameters KPIs ---

    /**
     * Average Weight on Bit (klbs)
     */
    private BigDecimal avgWobKlbs;

    /**
     * Average RPM
     */
    private BigDecimal avgRpm;

    /**
     * Average Torque (ft-lbs)
     */
    private BigDecimal avgTorqueFtLbs;

    /**
     * Average Differential Pressure (psi)
     */
    private BigDecimal avgDiffPressurePsi;

    /**
     * Average Standpipe Pressure (psi)
     */
    private BigDecimal avgSppPsi;

    /**
     * Average Flow Rate (gpm)
     */
    private BigDecimal avgFlowRateGpm;

    // --- MSE KPIs ---

    /**
     * Average Mechanical Specific Energy (psi)
     */
    private BigDecimal avgMsePsi;

    /**
     * Minimum MSE (psi)
     */
    private BigDecimal minMsePsi;

    // --- Connection KPIs ---

    /**
     * Number of connections made
     */
    private Integer connectionCount;

    /**
     * Average connection time (minutes)
     */
    private BigDecimal avgConnectionTimeMin;

    /**
     * Best connection time (minutes)
     */
    private BigDecimal bestConnectionTimeMin;

    // --- BHA KPIs ---

    /**
     * BHA identifier used
     */
    private UUID bhaId;

    /**
     * Bit serial number
     */
    private String bitSerialNumber;

    /**
     * Bit size (inches)
     */
    private BigDecimal bitSizeIn;

    /**
     * Bit grade in (IADC)
     */
    private String bitGradeIn;

    /**
     * Bit grade out (IADC)
     */
    private String bitGradeOut;

    // --- Directional KPIs ---

    /**
     * Build rate achieved (deg/100ft)
     */
    private BigDecimal buildRateDegPer100ft;

    /**
     * Turn rate achieved (deg/100ft)
     */
    private BigDecimal turnRateDegPer100ft;

    /**
     * Maximum Dog Leg Severity (deg/100ft)
     */
    private BigDecimal maxDlsDegPer100ft;

    /**
     * Average inclination (degrees)
     */
    private BigDecimal avgInclinationDeg;

    /**
     * Percentage of footage drilled while sliding
     */
    private BigDecimal slidingPercentage;

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
     * Invisible Lost Time percentage
     */
    private BigDecimal iltPercent;

    /**
     * Cost per foot ($/ft)
     */
    private BigDecimal costPerFoot;

    // --- Survey KPIs ---

    /**
     * Number of surveys taken
     */
    private Integer surveyCount;

    /**
     * Average survey interval (ft)
     */
    private BigDecimal avgSurveyIntervalFt;
}
