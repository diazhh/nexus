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
 * Result DTO for Bit Hydraulics calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitHydraulicsCalculationResult {

    /**
     * Jet velocity (ft/sec)
     */
    private BigDecimal jetVelocityFtSec;

    /**
     * Pressure drop across bit (psi)
     */
    private BigDecimal bitPressureDropPsi;

    /**
     * Bit Hydraulic Horsepower (HHP)
     */
    private BigDecimal bitHydraulicHorsePower;

    /**
     * HSI - Horsepower per Square Inch
     */
    private BigDecimal hsiHpPerSqIn;

    /**
     * Impact force at bit (lbs)
     */
    private BigDecimal impactForceLbs;

    /**
     * Percentage of total pressure drop at bit
     */
    private BigDecimal percentPressureAtBit;
}
