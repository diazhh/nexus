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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object for Operational Limits configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalLimitDto {

    /**
     * Limit configuration ID
     */
    private UUID id;

    /**
     * Entity ID that this limit applies to
     */
    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    /**
     * Entity type (pf_well, pf_esp_system, etc.)
     */
    @NotBlank(message = "Entity type is required")
    private String entityType;

    /**
     * Variable key (e.g., temperature_motor_f, frequency_hz)
     */
    @NotBlank(message = "Variable key is required")
    @Size(max = 255, message = "Variable key cannot exceed 255 characters")
    private String variableKey;

    /**
     * High-High limit (critical high)
     */
    private BigDecimal highHighLimit;

    /**
     * High limit (warning high)
     */
    private BigDecimal highLimit;

    /**
     * Low limit (warning low)
     */
    private BigDecimal lowLimit;

    /**
     * Low-Low limit (critical low)
     */
    private BigDecimal lowLowLimit;

    /**
     * Rate of change limit (per minute)
     */
    @PositiveOrZero(message = "Rate of change limit must be >= 0")
    private BigDecimal rateOfChangeLimit;

    /**
     * Deadband to prevent alarm flapping (percentage)
     */
    @PositiveOrZero(message = "Deadband must be >= 0")
    @Builder.Default
    private BigDecimal deadband = BigDecimal.valueOf(0.5);

    /**
     * Whether this limit is enabled
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
