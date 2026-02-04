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
package org.thingsboard.nexus.pf.dto;

/**
 * Operational status of a production well
 */
public enum WellStatus {
    /**
     * Well is producing normally
     */
    PRODUCING,

    /**
     * Well is temporarily shut-in
     */
    SHUT_IN,

    /**
     * Well is under workover/maintenance
     */
    UNDER_WORKOVER,

    /**
     * Well is abandoned
     */
    ABANDONED,

    /**
     * Well is suspended
     */
    SUSPENDED,

    /**
     * Well is inactive
     */
    INACTIVE
}
