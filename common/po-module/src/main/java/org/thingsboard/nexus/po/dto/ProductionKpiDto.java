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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for production KPIs and metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionKpiDto {

    /**
     * Tenant ID
     */
    private UUID tenantId;

    /**
     * Scope: FIELD, WELLPAD, or WELL
     */
    private String scope;

    /**
     * Asset ID (field, wellpad, or well)
     */
    private UUID assetId;

    /**
     * Asset name
     */
    private String assetName;

    /**
     * Period start timestamp
     */
    private Long periodStart;

    /**
     * Period end timestamp
     */
    private Long periodEnd;

    // Production metrics
    /**
     * Total oil production (BBL)
     */
    private BigDecimal totalOilProduction;

    /**
     * Average daily oil production (BPD)
     */
    private BigDecimal avgDailyOilProduction;

    /**
     * Total gas production (MSCF)
     */
    private BigDecimal totalGasProduction;

    /**
     * Total water production (BBL)
     */
    private BigDecimal totalWaterProduction;

    /**
     * Average water cut (%)
     */
    private BigDecimal avgWaterCut;

    /**
     * Average GOR (SCF/BBL)
     */
    private BigDecimal avgGor;

    // Efficiency metrics
    /**
     * Production efficiency (%)
     */
    private BigDecimal productionEfficiency;

    /**
     * Uptime percentage (%)
     */
    private BigDecimal uptimePercent;

    /**
     * Average pump efficiency (%)
     */
    private BigDecimal avgPumpEfficiency;

    /**
     * Average energy consumption (kWh/BBL)
     */
    private BigDecimal avgEnergyPerBarrel;

    // Optimization metrics
    /**
     * Number of optimizations performed
     */
    private Integer optimizationsPerformed;

    /**
     * Number of recommendations generated
     */
    private Integer recommendationsGenerated;

    /**
     * Number of recommendations accepted
     */
    private Integer recommendationsAccepted;

    /**
     * Recommendation acceptance rate (%)
     */
    private BigDecimal recommendationAcceptanceRate;

    /**
     * Production increase from optimization (BPD)
     */
    private BigDecimal productionIncreaseFromOptimization;

    /**
     * Cost savings from optimization (USD)
     */
    private BigDecimal costSavingsFromOptimization;

    // Health metrics
    /**
     * Number of wells by health level
     */
    private Map<HealthLevel, Integer> wellsByHealthLevel;

    /**
     * Average health score
     */
    private Double avgHealthScore;

    /**
     * Number of predicted failures avoided
     */
    private Integer failuresAvoided;

    // Alarm metrics
    /**
     * Number of alarms raised
     */
    private Integer alarmsRaised;

    /**
     * Number of critical alarms
     */
    private Integer criticalAlarms;

    /**
     * Average alarm response time (minutes)
     */
    private BigDecimal avgAlarmResponseTime;

    // Comparison
    /**
     * Previous period production for comparison
     */
    private BigDecimal previousPeriodProduction;

    /**
     * Production change vs previous period (%)
     */
    private BigDecimal productionChangePercent;

    /**
     * Target production for period
     */
    private BigDecimal targetProduction;

    /**
     * Production vs target (%)
     */
    private BigDecimal productionVsTargetPercent;
}
