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
 * Status of a drilling rig
 */
public enum RigStatus {
    /**
     * Rig is actively drilling
     */
    DRILLING,

    /**
     * Rig is tripping in (running pipe into hole)
     */
    TRIPPING_IN,

    /**
     * Rig is tripping out (pulling pipe out of hole)
     */
    TRIPPING_OUT,

    /**
     * Rig is circulating mud
     */
    CIRCULATING,

    /**
     * Rig is making a connection
     */
    CONNECTION,

    /**
     * Rig is running casing
     */
    CASING,

    /**
     * Rig is cementing
     */
    CEMENTING,

    /**
     * Rig is logging
     */
    LOGGING,

    /**
     * Rig is testing (BOP test, formation test, etc.)
     */
    TESTING,

    /**
     * Rig is rigging up
     */
    RIG_UP,

    /**
     * Rig is rigging down
     */
    RIG_DOWN,

    /**
     * Rig is moving between locations
     */
    MOVING,

    /**
     * Rig is on standby waiting for operations
     */
    STANDBY,

    /**
     * Rig is under maintenance
     */
    MAINTENANCE,

    /**
     * Rig is experiencing well control event
     */
    WELL_CONTROL,

    /**
     * Rig is stuck pipe situation
     */
    STUCK_PIPE,

    /**
     * Rig is fishing
     */
    FISHING,

    /**
     * Rig is offline/not operational
     */
    OFFLINE,

    /**
     * Rig has been decommissioned
     */
    DECOMMISSIONED
}
