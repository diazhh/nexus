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
 * Data Transfer Object for Production Well
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfWellDto {

    // Asset type constant for ThingsBoard
    public static final String ASSET_TYPE = "pf_well";

    // Attribute key constants
    public static final String ATTR_API_NUMBER = "api_number";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_LIFT_SYSTEM_TYPE = "lift_system_type";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";
    public static final String ATTR_MEASURED_DEPTH_FT = "measured_depth_ft";
    public static final String ATTR_TRUE_VERTICAL_DEPTH_FT = "true_vertical_depth_ft";
    public static final String ATTR_SPUD_DATE = "spud_date";
    public static final String ATTR_FIRST_PRODUCTION_DATE = "first_production_date";
    public static final String ATTR_CURRENT_PRODUCTION_BPD = "current_production_bpd";
    public static final String ATTR_RV_WELL_ID = "rv_well_id";

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
     * Well name
     */
    @NotBlank(message = "Well name is required")
    @Size(min = 2, max = 255, message = "Well name must be between 2 and 255 characters")
    private String name;

    /**
     * API Well Number (unique industry identifier)
     */
    @Size(max = 50, message = "API number cannot exceed 50 characters")
    private String apiNumber;

    /**
     * Parent wellpad ID
     */
    private UUID wellpadId;

    /**
     * Reference to RV module well (for reservoir characterization data)
     */
    private UUID rvWellId;

    /**
     * Current well status
     */
    @NotNull(message = "Well status is required")
    private WellStatus status;

    /**
     * Type of artificial lift system
     */
    private LiftSystemType liftSystemType;

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
     * Measured depth in feet
     */
    @PositiveOrZero(message = "Measured depth must be >= 0")
    private BigDecimal measuredDepthFt;

    /**
     * True vertical depth in feet
     */
    @PositiveOrZero(message = "True vertical depth must be >= 0")
    private BigDecimal trueVerticalDepthFt;

    /**
     * Date when drilling started
     */
    private LocalDate spudDate;

    /**
     * Date of first production
     */
    private LocalDate firstProductionDate;

    /**
     * Current production rate in barrels per day
     */
    @PositiveOrZero(message = "Production rate must be >= 0")
    private BigDecimal currentProductionBpd;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Last update timestamp
     */
    private Long updatedTime;
}
