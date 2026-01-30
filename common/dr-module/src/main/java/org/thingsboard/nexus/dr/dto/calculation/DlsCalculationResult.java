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
package org.thingsboard.nexus.dr.dto.calculation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result DTO for Dog Leg Severity calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlsCalculationResult {

    /**
     * Dog Leg Severity (degrees per 100 ft)
     */
    private BigDecimal dlsDegPer100ft;

    /**
     * Total dogleg angle (degrees)
     */
    private BigDecimal doglegAngleDeg;

    /**
     * Course length between surveys (ft)
     */
    private BigDecimal courseLengthFt;

    /**
     * Change in inclination (degrees)
     */
    private BigDecimal inclinationChangeDeg;

    /**
     * Change in azimuth (degrees)
     */
    private BigDecimal azimuthChangeDeg;

    /**
     * Build/Drop rate (degrees per 100 ft) - positive is building
     */
    private BigDecimal buildRateDegPer100ft;

    /**
     * Turn rate (degrees per 100 ft) - positive is right turn
     */
    private BigDecimal turnRateDegPer100ft;

    /**
     * Severity classification: LOW, MODERATE, HIGH, SEVERE
     */
    private String severity;
}
