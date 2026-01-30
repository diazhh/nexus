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
 * Status of a drilling run (bit run)
 */
public enum RunStatus {
    /**
     * Run is planned but not started
     */
    PLANNED,

    /**
     * Run is in progress
     */
    IN_PROGRESS,

    /**
     * Run is on hold (waiting)
     */
    ON_HOLD,

    /**
     * Run completed successfully
     */
    COMPLETED,

    /**
     * Run was cancelled
     */
    CANCELLED,

    /**
     * Run failed (stuck pipe, tool failure, etc.)
     */
    FAILED
}
