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
 * DTO for ESP (Electrical Submersible Pump) frequency optimization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspOptimizationDto {

    /**
     * Well asset ID
     */
    private UUID wellAssetId;

    /**
     * ESP system asset ID
     */
    private UUID espAssetId;

    /**
     * Well name
     */
    private String wellName;

    // Current operating conditions
    /**
     * Current frequency (Hz)
     */
    private BigDecimal currentFrequency;

    /**
     * Current motor load (%)
     */
    private BigDecimal currentMotorLoad;

    /**
     * Current motor temperature (°F)
     */
    private BigDecimal currentMotorTemperature;

    /**
     * Current pump intake pressure (psi)
     */
    private BigDecimal currentPip;

    /**
     * Current discharge pressure (psi)
     */
    private BigDecimal currentDischargePressure;

    /**
     * Current production rate (BPD)
     */
    private BigDecimal currentProductionBpd;

    /**
     * Current power consumption (kW)
     */
    private BigDecimal currentPowerKw;

    // Optimized values
    /**
     * Recommended frequency (Hz)
     */
    private BigDecimal recommendedFrequency;

    /**
     * Frequency change from current (Hz)
     */
    private BigDecimal frequencyChange;

    /**
     * Expected motor load at new frequency (%)
     */
    private BigDecimal expectedMotorLoad;

    /**
     * Expected motor temperature at new frequency (°F)
     */
    private BigDecimal expectedMotorTemperature;

    /**
     * Expected production rate at new frequency (BPD)
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
     * Expected power consumption at new frequency (kW)
     */
    private BigDecimal expectedPowerKw;

    /**
     * Expected efficiency improvement (%)
     */
    private BigDecimal expectedEfficiencyImprovement;

    // Constraints
    /**
     * Maximum allowed frequency (Hz)
     */
    private BigDecimal maxFrequency;

    /**
     * Minimum allowed frequency (Hz)
     */
    private BigDecimal minFrequency;

    /**
     * Maximum motor temperature limit (°F)
     */
    private BigDecimal maxMotorTemperature;

    /**
     * Maximum motor load limit (%)
     */
    private BigDecimal maxMotorLoad;

    /**
     * Minimum PIP limit (psi)
     */
    private BigDecimal minPip;

    // Analysis details
    /**
     * Operating point efficiency (%)
     */
    private BigDecimal operatingEfficiency;

    /**
     * Best efficiency point frequency (Hz)
     */
    private BigDecimal bepFrequency;

    /**
     * Distance from BEP (%)
     */
    private BigDecimal distanceFromBep;

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
