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
 * Result DTO for ECD (Equivalent Circulating Density) calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcdCalculationResult {

    /**
     * Equivalent Circulating Density (ppg)
     */
    private BigDecimal ecdPpg;

    /**
     * ECD increment due to annular pressure loss (ppg)
     */
    private BigDecimal ecdIncrementPpg;

    /**
     * Annular pressure loss (psi)
     */
    private BigDecimal annularPressureLossPsi;

    /**
     * Hydrostatic pressure at TVD (psi)
     */
    private BigDecimal hydrostaticPressurePsi;

    /**
     * Bottom hole circulating pressure (psi)
     */
    private BigDecimal bottomHolePressurePsi;

    /**
     * Is ECD above fracture gradient
     */
    private Boolean isAboveFracGradient;

    /**
     * Is ECD below pore pressure
     */
    private Boolean isBelowPorePressure;

    /**
     * Margin to fracture gradient (ppg)
     */
    private BigDecimal marginToFracPpg;

    /**
     * Margin above pore pressure (ppg)
     */
    private BigDecimal marginToPorePpg;
}
