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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for Telemetry Data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryDataDto {

    /**
     * Entity ID that the telemetry belongs to
     */
    private UUID entityId;

    /**
     * Entity type (pf_well, pf_esp_system, etc.)
     */
    private String entityType;

    /**
     * Timestamp of the telemetry reading
     */
    private Long timestamp;

    /**
     * Key-value pairs of telemetry data
     */
    private Map<String, Object> values;

    /**
     * Data quality score (0.0 - 1.0)
     */
    private Double quality;

    /**
     * Get a specific value as Double
     */
    public Double getDoubleValue(String key) {
        Object value = values.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * Get a specific value as String
     */
    public String getStringValue(String key) {
        Object value = values.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Get a specific value as Boolean
     */
    public Boolean getBooleanValue(String key) {
        Object value = values.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
