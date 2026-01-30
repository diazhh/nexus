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
 * Request DTO for Torque and Drag calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TorqueDragCalculationRequest {

    /**
     * Operation type: ROTATING, TRIPPING_IN, TRIPPING_OUT, SLIDING
     */
    private String operation;

    /**
     * Friction coefficient (typical 0.15-0.35)
     */
    private BigDecimal frictionCoefficient;

    /**
     * Mud weight (ppg)
     */
    private BigDecimal mudWeightPpg;

    /**
     * String weight (lbs/ft)
     */
    private BigDecimal stringWeightLbsPerFt;

    /**
     * Total measured depth (ft)
     */
    private BigDecimal totalDepthFt;

    /**
     * Average inclination along hole (degrees)
     */
    private BigDecimal avgInclinationDeg;

    /**
     * Pipe outside diameter (inches)
     */
    private BigDecimal pipeOdIn;
}
