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
import java.util.UUID;

/**
 * DTO for Rod Pump (Sucker Rod Pump / Beam Pump) optimization.
 *
 * Rod Pump optimization focuses on:
 * - Stroke length adjustment
 * - Strokes per minute (SPM) optimization
 * - Pump fillage analysis
 * - Counterbalance optimization
 * - Rod stress management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RodPumpOptimizationDto {

    /**
     * Well asset ID
     */
    private UUID wellAssetId;

    /**
     * Rod pump system asset ID
     */
    private UUID rodPumpAssetId;

    /**
     * Well name
     */
    private String wellName;

    // Current operating conditions

    /**
     * Current strokes per minute (SPM)
     */
    private BigDecimal currentSpm;

    /**
     * Current stroke length (inches)
     */
    private BigDecimal currentStrokeLength;

    /**
     * Current pump fillage (%)
     */
    private BigDecimal currentFillage;

    /**
     * Current peak polished rod load (lbs)
     */
    private BigDecimal currentPeakLoad;

    /**
     * Current minimum polished rod load (lbs)
     */
    private BigDecimal currentMinLoad;

    /**
     * Current counterbalance effect (%)
     */
    private BigDecimal currentCounterbalance;

    /**
     * Current production rate (BPD)
     */
    private BigDecimal currentProductionBpd;

    /**
     * Current power consumption (kW)
     */
    private BigDecimal currentPowerKw;

    /**
     * Current pump efficiency (%)
     */
    private BigDecimal currentPumpEfficiency;

    /**
     * Current rod stress (psi)
     */
    private BigDecimal currentRodStress;

    // Optimized values

    /**
     * Recommended SPM
     */
    private BigDecimal recommendedSpm;

    /**
     * SPM change from current
     */
    private BigDecimal spmChange;

    /**
     * Recommended stroke length (inches)
     */
    private BigDecimal recommendedStrokeLength;

    /**
     * Stroke length change (inches)
     */
    private BigDecimal strokeLengthChange;

    /**
     * Expected fillage at optimized settings (%)
     */
    private BigDecimal expectedFillage;

    /**
     * Expected peak load (lbs)
     */
    private BigDecimal expectedPeakLoad;

    /**
     * Expected production rate (BPD)
     */
    private BigDecimal expectedProductionBpd;

    /**
     * Expected production increase (BPD)
     */
    private BigDecimal expectedProductionIncrease;

    /**
     * Expected production increase (%)
     */
    private BigDecimal expectedProductionIncreasePercent;

    /**
     * Expected power consumption (kW)
     */
    private BigDecimal expectedPowerKw;

    /**
     * Expected pump efficiency (%)
     */
    private BigDecimal expectedPumpEfficiency;

    /**
     * Expected efficiency improvement (%)
     */
    private BigDecimal expectedEfficiencyImprovement;

    /**
     * Expected rod stress (psi)
     */
    private BigDecimal expectedRodStress;

    // Constraints

    /**
     * Maximum SPM
     */
    private BigDecimal maxSpm;

    /**
     * Minimum SPM
     */
    private BigDecimal minSpm;

    /**
     * Maximum stroke length (inches)
     */
    private BigDecimal maxStrokeLength;

    /**
     * Minimum stroke length (inches)
     */
    private BigDecimal minStrokeLength;

    /**
     * Maximum peak load (lbs)
     */
    private BigDecimal maxPeakLoad;

    /**
     * Maximum rod stress (psi)
     */
    private BigDecimal maxRodStress;

    /**
     * Minimum fillage threshold (%)
     */
    private BigDecimal minFillage;

    // Analysis details

    /**
     * Optimal fillage SPM (for current stroke)
     */
    private BigDecimal optimalFillageSpm;

    /**
     * Pump displacement (bbl/stroke)
     */
    private BigDecimal pumpDisplacement;

    /**
     * Theoretical production capacity (BPD)
     */
    private BigDecimal theoreticalCapacity;

    /**
     * Volumetric efficiency (%)
     */
    private BigDecimal volumetricEfficiency;

    /**
     * Counterbalance recommendation
     */
    private String counterbalanceRecommendation;

    /**
     * Primary optimization type (SPEED, STROKE, or BOTH)
     */
    private String optimizationType;

    /**
     * Constraint that limits optimization
     */
    private String limitingConstraint;

    /**
     * Confidence score (0.0 to 1.0)
     */
    private Double confidence;

    /**
     * Whether this is a significant recommendation
     */
    private Boolean isSignificant;

    /**
     * Reason if not significant
     */
    private String notSignificantReason;

    /**
     * Dynacard analysis summary
     */
    private String dynacardAnalysis;
}
