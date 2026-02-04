/*
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
 * Health level classification for equipment.
 */
public enum HealthLevel {
    /**
     * Equipment is in excellent condition (score >= 0.9)
     */
    EXCELLENT,

    /**
     * Equipment is in good condition (score >= 0.75)
     */
    GOOD,

    /**
     * Equipment needs attention (score >= 0.5)
     */
    FAIR,

    /**
     * Equipment is in poor condition (score >= 0.25)
     */
    POOR,

    /**
     * Equipment is critical, intervention needed (score < 0.25)
     */
    CRITICAL;

    /**
     * Get health level from score.
     */
    public static HealthLevel fromScore(double score) {
        if (score >= 0.9) return EXCELLENT;
        if (score >= 0.75) return GOOD;
        if (score >= 0.5) return FAIR;
        if (score >= 0.25) return POOR;
        return CRITICAL;
    }
}
