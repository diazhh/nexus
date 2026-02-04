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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Production Facility Alarm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfAlarmDto {

    /**
     * Alarm ID
     */
    private UUID id;

    /**
     * Tenant ID
     */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    /**
     * Entity ID (well, ESP, etc.) that triggered the alarm
     */
    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    /**
     * Entity type (pf_well, pf_esp_system, etc.)
     */
    @NotBlank(message = "Entity type is required")
    private String entityType;

    /**
     * Entity name for display
     */
    private String entityName;

    /**
     * Alarm type (e.g., LIMIT_VIOLATION_TEMPERATURE_MOTOR_F)
     */
    @NotBlank(message = "Alarm type is required")
    @Size(max = 100, message = "Alarm type cannot exceed 100 characters")
    private String alarmType;

    /**
     * Alarm severity
     */
    @NotNull(message = "Severity is required")
    private AlarmSeverity severity;

    /**
     * Current alarm status
     */
    @NotNull(message = "Status is required")
    private AlarmStatus status;

    /**
     * Human-readable alarm message
     */
    private String message;

    /**
     * Additional details in JSON format
     */
    private JsonNode details;

    /**
     * Timestamp when alarm started
     */
    private Long startTime;

    /**
     * Timestamp when alarm ended (condition cleared)
     */
    private Long endTime;

    /**
     * Timestamp when alarm was acknowledged
     */
    private Long acknowledgedTime;

    /**
     * User who acknowledged the alarm
     */
    private UUID acknowledgedBy;

    /**
     * Comment added when acknowledging
     */
    private String acknowledgeComment;

    /**
     * Timestamp when alarm was cleared
     */
    private Long clearedTime;

    /**
     * Recommended actions for this alarm
     */
    private List<String> recommendedActions;

    /**
     * Tags for categorization
     */
    private List<String> tags;
}
