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
 * Request DTO for Swab/Surge pressure calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwabSurgeCalculationRequest {

    /**
     * Mud weight (ppg)
     */
    private BigDecimal mudWeightPpg;

    /**
     * True Vertical Depth (ft)
     */
    private BigDecimal tvdFt;

    /**
     * Trip speed (ft/min)
     */
    private BigDecimal tripSpeedFtMin;

    /**
     * Hole inside diameter (inches)
     */
    private BigDecimal holeIdIn;

    /**
     * Pipe outside diameter (inches)
     */
    private BigDecimal pipeOdIn;

    /**
     * Closed end outside diameter (inches) - for BHA
     */
    private BigDecimal closedEndOdIn;

    /**
     * Is pipe open ended (allows flow through)
     */
    private Boolean isOpenEnded;

    /**
     * Plastic viscosity (cp)
     */
    private BigDecimal plasticViscosityCp;

    /**
     * Yield point (lbf/100sq ft)
     */
    private BigDecimal yieldPointLbf100sqft;

    /**
     * Pore pressure gradient (ppg) - for swab check
     */
    private BigDecimal porePressurePpg;

    /**
     * Fracture gradient (ppg) - for surge check
     */
    private BigDecimal fracGradientPpg;
}
