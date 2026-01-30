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
 * Result DTO for Torque and Drag calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TorqueDragCalculationResult {

    /**
     * Hook load while rotating (lbs)
     */
    private BigDecimal rotatingHookLoadLbs;

    /**
     * Hook load while tripping in (lbs)
     */
    private BigDecimal trippingInHookLoadLbs;

    /**
     * Hook load while tripping out (lbs)
     */
    private BigDecimal trippingOutHookLoadLbs;

    /**
     * Hook load while sliding (lbs)
     */
    private BigDecimal slidingHookLoadLbs;

    /**
     * Surface torque (ft-lbs)
     */
    private BigDecimal surfaceTorqueFtLbs;

    /**
     * Total drag force (lbs)
     */
    private BigDecimal dragForceLbs;

    /**
     * Buoyancy factor used
     */
    private BigDecimal buoyancyFactor;

    /**
     * Total buoyed string weight (lbs)
     */
    private BigDecimal totalBuoyedStringWeightLbs;

    /**
     * Hook load for the specified operation (lbs)
     */
    private BigDecimal operationHookLoadLbs;

    /**
     * Operation type that was calculated
     */
    private String operation;
}
