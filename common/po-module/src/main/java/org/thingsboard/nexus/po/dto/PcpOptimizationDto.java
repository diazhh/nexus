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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for PCP (Progressive Cavity Pump) speed optimization.
 *
 * PCP optimization focuses on RPM adjustment considering:
 * - Pump efficiency curve
 * - Rod/stator wear at different speeds
 * - Fluid viscosity effects
 * - Power consumption
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcpOptimizationDto {

    /**
     * Well asset ID
     */
    private UUID wellAssetId;

    /**
     * PCP system asset ID
     */
    private UUID pcpAssetId;

    /**
     * Well name
     */
    private String wellName;

    // Current operating conditions

    /**
     * Current pump speed (RPM)
     */
    private BigDecimal currentRpm;

    /**
     * Current motor torque (%)
     */
    private BigDecimal currentTorque;

    /**
     * Current drive load (%)
     */
    private BigDecimal currentDriveLoad;

    /**
     * Current rod string load (lbs)
     */
    private BigDecimal currentRodLoad;

    /**
     * Current pump intake pressure (psi)
     */
    private BigDecimal currentPip;

    /**
     * Current production rate (BPD)
     */
    private BigDecimal currentProductionBpd;

    /**
     * Current power consumption (kW)
     */
    private BigDecimal currentPowerKw;

    /**
     * Current fluid viscosity (cP)
     */
    private BigDecimal currentViscosity;

    /**
     * Current pump efficiency (%)
     */
    private BigDecimal currentPumpEfficiency;

    // Optimized values

    /**
     * Recommended pump speed (RPM)
     */
    private BigDecimal recommendedRpm;

    /**
     * Speed change from current (RPM)
     */
    private BigDecimal rpmChange;

    /**
     * Expected torque at new speed (%)
     */
    private BigDecimal expectedTorque;

    /**
     * Expected drive load at new speed (%)
     */
    private BigDecimal expectedDriveLoad;

    /**
     * Expected production rate at new speed (BPD)
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
     * Expected power consumption at new speed (kW)
     */
    private BigDecimal expectedPowerKw;

    /**
     * Expected pump efficiency at new speed (%)
     */
    private BigDecimal expectedPumpEfficiency;

    /**
     * Expected efficiency improvement (%)
     */
    private BigDecimal expectedEfficiencyImprovement;

    // Constraints

    /**
     * Maximum allowed RPM
     */
    private BigDecimal maxRpm;

    /**
     * Minimum allowed RPM
     */
    private BigDecimal minRpm;

    /**
     * Maximum torque limit (%)
     */
    private BigDecimal maxTorque;

    /**
     * Maximum rod load limit (lbs)
     */
    private BigDecimal maxRodLoad;

    /**
     * Minimum PIP limit (psi)
     */
    private BigDecimal minPip;

    // Analysis details

    /**
     * Optimal efficiency point RPM
     */
    private BigDecimal oepRpm;

    /**
     * Distance from optimal efficiency point (%)
     */
    private BigDecimal distanceFromOep;

    /**
     * Estimated rod wear factor (0-1, higher = more wear)
     */
    private BigDecimal rodWearFactor;

    /**
     * Estimated stator wear factor (0-1, higher = more wear)
     */
    private BigDecimal statorWearFactor;

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
}
