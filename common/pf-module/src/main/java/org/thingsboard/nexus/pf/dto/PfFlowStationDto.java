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

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for Flow Station
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfFlowStationDto {

    // Asset type constant for ThingsBoard
    public static final String ASSET_TYPE = "pf_flow_station";

    // Attribute key constants
    public static final String ATTR_CODE = "code";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";
    public static final String ATTR_DESIGN_CAPACITY_BPD = "design_capacity_bpd";
    public static final String ATTR_CURRENT_THROUGHPUT_BPD = "current_throughput_bpd";
    public static final String ATTR_COMMISSIONING_DATE = "commissioning_date";
    public static final String ATTR_OPERATIONAL_STATUS = "operational_status";
    public static final String ATTR_SEPARATOR_COUNT = "separator_count";
    public static final String ATTR_TANK_COUNT = "tank_count";

    /**
     * Asset ID in ThingsBoard
     */
    private UUID assetId;

    /**
     * Tenant ID
     */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    /**
     * Flow station name
     */
    @NotBlank(message = "Flow station name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    /**
     * Unique code for the flow station
     */
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    /**
     * Geographic latitude
     */
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    /**
     * Geographic longitude
     */
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    /**
     * Design capacity in barrels per day
     */
    @PositiveOrZero(message = "Design capacity must be >= 0")
    private BigDecimal designCapacityBpd;

    /**
     * Current throughput in barrels per day
     */
    @PositiveOrZero(message = "Current throughput must be >= 0")
    private BigDecimal currentThroughputBpd;

    /**
     * Date when the flow station was commissioned
     */
    private LocalDate commissioningDate;

    /**
     * Current operational status
     */
    private OperationalStatus operationalStatus;

    /**
     * Number of separators in the flow station
     */
    @PositiveOrZero(message = "Separator count must be >= 0")
    private Integer separatorCount;

    /**
     * Number of tanks in the flow station
     */
    @PositiveOrZero(message = "Tank count must be >= 0")
    private Integer tankCount;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Last update timestamp
     */
    private Long updatedTime;
}
