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
 * KPI Data Transfer Object for a rig.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RigKpiDto {

    /**
     * Rig identifier
     */
    private UUID rigId;

    /**
     * Rig name
     */
    private String rigName;

    /**
     * Contractor name
     */
    private String contractor;

    // --- Activity KPIs ---

    /**
     * Total wells drilled
     */
    private Integer totalWellsDrilled;

    /**
     * Total footage drilled (ft)
     */
    private BigDecimal totalFootageDrilledFt;

    /**
     * Total operating hours
     */
    private BigDecimal totalOperatingHours;

    /**
     * Total drilling hours
     */
    private BigDecimal totalDrillingHours;

    /**
     * Total NPT hours
     */
    private BigDecimal totalNptHours;

    // --- Performance KPIs ---

    /**
     * Average ROP across all wells (ft/hr)
     */
    private BigDecimal avgRopFtHr;

    /**
     * Best ROP achieved (ft/hr)
     */
    private BigDecimal bestRopFtHr;

    /**
     * Average connection time (minutes)
     */
    private BigDecimal avgConnectionTimeMin;

    /**
     * Best connection time achieved (minutes)
     */
    private BigDecimal bestConnectionTimeMin;

    /**
     * Average trip speed (ft/hr)
     */
    private BigDecimal avgTripSpeedFtHr;

    // --- Efficiency KPIs ---

    /**
     * Average drilling efficiency percentage
     */
    private BigDecimal avgDrillingEfficiencyPercent;

    /**
     * Average NPT percentage
     */
    private BigDecimal avgNptPercent;

    /**
     * Average productive time percentage
     */
    private BigDecimal avgProductiveTimePercent;

    /**
     * Rig utilization percentage
     */
    private BigDecimal rigUtilizationPercent;

    // --- Safety KPIs ---

    /**
     * Total Recordable Incident Rate (TRIR)
     */
    private BigDecimal trir;

    /**
     * Days since last recordable incident
     */
    private Integer daysSinceLastIncident;

    /**
     * Lost Time Injury Frequency (LTIF)
     */
    private BigDecimal ltif;

    // --- Cost KPIs ---

    /**
     * Average cost per foot ($/ft)
     */
    private BigDecimal avgCostPerFoot;

    /**
     * Average cost per day ($/day)
     */
    private BigDecimal avgCostPerDay;

    /**
     * Total operational cost ($)
     */
    private BigDecimal totalOperationalCost;

    // --- Equipment KPIs ---

    /**
     * Equipment uptime percentage
     */
    private BigDecimal equipmentUptimePercent;

    /**
     * Mean Time Between Failures (hours)
     */
    private BigDecimal mtbfHours;

    /**
     * Mean Time To Repair (hours)
     */
    private BigDecimal mttrHours;

    // --- Time Period ---

    /**
     * KPI calculation start time (epoch ms)
     */
    private Long periodStartTime;

    /**
     * KPI calculation end time (epoch ms)
     */
    private Long periodEndTime;

    /**
     * Number of runs in calculation period
     */
    private Integer runsInPeriod;

    // --- Benchmark Comparison ---

    /**
     * ROP vs benchmark percentage
     */
    private BigDecimal ropVsBenchmarkPercent;

    /**
     * Connection time vs benchmark percentage
     */
    private BigDecimal connectionTimeVsBenchmarkPercent;

    /**
     * Efficiency vs benchmark percentage
     */
    private BigDecimal efficiencyVsBenchmarkPercent;
}
