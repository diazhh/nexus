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
 * Type of hole section being drilled
 */
public enum HoleSection {
    /**
     * Conductor section (typically 30" or 36")
     */
    CONDUCTOR,

    /**
     * Surface casing section (typically 20" or 13-3/8")
     */
    SURFACE,

    /**
     * Intermediate casing section
     */
    INTERMEDIATE,

    /**
     * Production casing section
     */
    PRODUCTION,

    /**
     * Liner section
     */
    LINER,

    /**
     * Sidetrack section
     */
    SIDETRACK,

    /**
     * Horizontal section
     */
    HORIZONTAL,

    /**
     * Rat hole / mouse hole
     */
    RAT_HOLE
}
