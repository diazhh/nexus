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
 * Request DTO for Dog Leg Severity calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlsCalculationRequest {

    /**
     * Measured Depth at survey 1 (ft)
     */
    private BigDecimal md1Ft;

    /**
     * Measured Depth at survey 2 (ft)
     */
    private BigDecimal md2Ft;

    /**
     * Inclination at survey 1 (degrees)
     */
    private BigDecimal inclination1Deg;

    /**
     * Inclination at survey 2 (degrees)
     */
    private BigDecimal inclination2Deg;

    /**
     * Azimuth at survey 1 (degrees)
     */
    private BigDecimal azimuth1Deg;

    /**
     * Azimuth at survey 2 (degrees)
     */
    private BigDecimal azimuth2Deg;
}
