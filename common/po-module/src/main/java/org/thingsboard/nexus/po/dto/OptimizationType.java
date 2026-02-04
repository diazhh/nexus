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

/**
 * Types of optimization that can be performed.
 */
public enum OptimizationType {
    /**
     * ESP frequency optimization
     */
    ESP_FREQUENCY,

    /**
     * ESP stage optimization
     */
    ESP_STAGING,

    /**
     * Gas lift gas injection rate allocation
     */
    GAS_LIFT_ALLOCATION,

    /**
     * Gas lift valve optimization
     */
    GAS_LIFT_VALVE,

    /**
     * PCP speed (RPM) optimization
     */
    PCP_SPEED,

    /**
     * Rod pump stroke optimization
     */
    ROD_PUMP_STROKE,

    /**
     * Rod pump speed optimization
     */
    ROD_PUMP_SPEED,

    /**
     * General production rate optimization
     */
    PRODUCTION_RATE,

    /**
     * Water cut management
     */
    WATER_CUT,

    /**
     * Multi-well field optimization
     */
    FIELD_OPTIMIZATION
}
