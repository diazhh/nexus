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
package org.thingsboard.nexus.dr.model.enums;

/**
 * Type of well control event
 */
public enum WellControlEventType {
    /**
     * Kick - influx of formation fluid
     */
    KICK,

    /**
     * Lost circulation - mud loss to formation
     */
    LOSS,

    /**
     * Ballooning - wellbore breathing
     */
    BALLOONING,

    /**
     * Flow check performed
     */
    FLOW_CHECK,

    /**
     * Gas cut mud
     */
    GAS_CUT,

    /**
     * H2S detection
     */
    H2S_DETECTION,

    /**
     * Underground blowout
     */
    UNDERGROUND_BLOWOUT,

    /**
     * Surface blowout
     */
    SURFACE_BLOWOUT,

    /**
     * Near miss event
     */
    NEAR_MISS
}
