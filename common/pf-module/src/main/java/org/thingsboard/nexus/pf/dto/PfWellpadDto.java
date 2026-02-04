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
import jakarta.validation.constraints.Positive;
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
 * Data Transfer Object for Wellpad (Macolla/Cluster)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfWellpadDto {

    // Asset type constant for ThingsBoard
    public static final String ASSET_TYPE = "pf_wellpad";

    // Attribute key constants
    public static final String ATTR_CODE = "code";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";
    public static final String ATTR_CAPACITY_WELLS = "capacity_wells";
    public static final String ATTR_CURRENT_WELL_COUNT = "current_well_count";
    public static final String ATTR_TOTAL_PRODUCTION_BPD = "total_production_bpd";
    public static final String ATTR_COMMISSIONING_DATE = "commissioning_date";
    public static final String ATTR_OPERATIONAL_STATUS = "operational_status";

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
     * Wellpad name
     */
    @NotBlank(message = "Wellpad name is required")
    @Size(min = 2, max = 255, message = "Wellpad name must be between 2 and 255 characters")
    private String name;

    /**
     * Unique code for the wellpad
     */
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    /**
     * Parent flow station ID
     */
    private UUID flowStationId;

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
     * Maximum number of wells the wellpad can support
     */
    @Positive(message = "Capacity must be > 0")
    private Integer capacityWells;

    /**
     * Current number of wells in the wellpad
     */
    @PositiveOrZero(message = "Current well count must be >= 0")
    private Integer currentWellCount;

    /**
     * Total production from all wells in barrels per day
     */
    @PositiveOrZero(message = "Total production must be >= 0")
    private BigDecimal totalProductionBpd;

    /**
     * Date when the wellpad was commissioned
     */
    private LocalDate commissioningDate;

    /**
     * Current operational status
     */
    private OperationalStatus operationalStatus;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Last update timestamp
     */
    private Long updatedTime;
}
