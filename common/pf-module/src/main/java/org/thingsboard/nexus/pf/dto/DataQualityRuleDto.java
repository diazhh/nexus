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
package org.thingsboard.nexus.pf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Quality Rule configuration for telemetry validation.
 * Defines physical ranges, rate of change limits, and outlier detection parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityRuleDto {

    /**
     * Rule ID
     */
    private UUID id;

    /**
     * Variable key (e.g., "frequency_hz", "temperature_motor_f")
     */
    @NotBlank(message = "Variable key is required")
    private String variableKey;

    /**
     * Entity type this rule applies to (null = all entities)
     */
    private String entityType;

    /**
     * Specific entity ID (null = all entities of the type)
     */
    private UUID entityId;

    /**
     * Physical minimum value (below this is physically impossible)
     */
    private BigDecimal physicalMin;

    /**
     * Physical maximum value (above this is physically impossible)
     */
    private BigDecimal physicalMax;

    /**
     * Expected minimum value under normal operation
     */
    private BigDecimal expectedMin;

    /**
     * Expected maximum value under normal operation
     */
    private BigDecimal expectedMax;

    /**
     * Maximum allowed rate of change per minute (percentage)
     * Example: 10.0 means max 10% change per minute
     */
    private BigDecimal maxRateOfChangePercentPerMinute;

    /**
     * Maximum allowed absolute change per minute
     */
    private BigDecimal maxAbsoluteChangePerMinute;

    /**
     * Number of standard deviations for outlier detection (default: 3.0)
     */
    @Builder.Default
    private BigDecimal outlierSigmaThreshold = BigDecimal.valueOf(3.0);

    /**
     * Minimum sample count for statistical calculations
     */
    @Builder.Default
    private Integer minSamplesForStatistics = 30;

    /**
     * Unit of measurement for display
     */
    private String unit;

    /**
     * Human-readable description
     */
    private String description;

    /**
     * Whether this rule is enabled
     */
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Last update timestamp
     */
    private Long updatedTime;
}
