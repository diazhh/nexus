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
 * Type of drilling rig
 */
public enum RigType {
    /**
     * Land-based drilling rig
     */
    LAND,

    /**
     * Jack-up offshore rig
     */
    JACKUP,

    /**
     * Semi-submersible offshore rig
     */
    SEMI_SUBMERSIBLE,

    /**
     * Drillship
     */
    DRILLSHIP,

    /**
     * Platform rig (fixed platform)
     */
    PLATFORM,

    /**
     * Tender-assisted drilling
     */
    TENDER,

    /**
     * Barge-mounted rig
     */
    BARGE,

    /**
     * Workover rig
     */
    WORKOVER,

    /**
     * Coiled tubing drilling rig
     */
    CTD,

    /**
     * Hydraulic workover unit
     */
    HWU
}
