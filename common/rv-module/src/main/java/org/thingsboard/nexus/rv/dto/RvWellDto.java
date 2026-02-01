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
package org.thingsboard.nexus.rv.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO representing a Well (Pozo) entity.
 * Maps to a ThingsBoard Asset of type "rv_well" with SERVER_SCOPE attributes.
 *
 * IMPORTANT: This is the RESERVOIR module's view of a well (geological focus).
 * Integration with Drilling Module (Taladros) provides operational drilling data.
 *
 * Hierarchy: Reservoir -> Zone -> Well -> Completion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvWellDto {

    // Asset identity
    private UUID assetId;
    private UUID tenantId;
    private String name;
    private String label;

    // Hierarchy
    private UUID reservoirAssetId;
    private UUID zoneAssetId;
    private UUID fieldAssetId;          // Denormalized for quick access

    // Well identification
    private String wellCode;            // API/UWI number
    private String wellName;
    private String wellAlias;
    private String licenseNumber;

    // Well classification
    private String wellType;            // PRODUCER, INJECTOR, OBSERVATION, DISPOSAL
    private String wellStatus;          // ACTIVE, SHUT_IN, ABANDONED, DRILLING, COMPLETING
    private String wellPurpose;         // OIL, GAS, WATER_INJECTION, GAS_INJECTION, WAG
    private String wellConfiguration;   // VERTICAL, DEVIATED, HORIZONTAL, MULTILATERAL
    private String liftMethod;          // NATURAL_FLOW, ESP, SRP, GAS_LIFT, PCP, JET_PUMP

    // Location (surface)
    private BigDecimal surfaceLatitude;
    private BigDecimal surfaceLongitude;
    private BigDecimal surfaceElevationM;
    private String surfaceLocation;     // Pad name, slot number

    // Location (bottomhole)
    private BigDecimal bottomholeLatitude;
    private BigDecimal bottomholeLongitude;

    // Depths
    private BigDecimal totalDepthMdM;   // TD measured depth
    private BigDecimal totalDepthTvdM;  // TD true vertical depth
    private BigDecimal kickoffPointM;   // KOP for deviated wells
    private BigDecimal datumElevationM;

    // Drilling dates (integration with Drilling Module)
    private Long spudDate;
    private Long tdReachedDate;
    private Long completionDate;
    private Long firstProductionDate;

    // Casing program (summary)
    private BigDecimal surfaceCasingDepthM;
    private BigDecimal surfaceCasingDiameterIn;
    private BigDecimal intermediateCasingDepthM;
    private BigDecimal intermediateCasingDiameterIn;
    private BigDecimal productionCasingDepthM;
    private BigDecimal productionCasingDiameterIn;
    private BigDecimal linerDepthM;
    private BigDecimal linerDiameterIn;

    // Production data (current - telemetry references)
    private BigDecimal currentOilRateBopd;
    private BigDecimal currentGasRateMscfd;
    private BigDecimal currentWaterRateBwpd;
    private BigDecimal currentWaterCutPercent;
    private BigDecimal currentGorScfStb;
    private BigDecimal currentThpPsi;       // Tubing head pressure
    private BigDecimal currentChokeSizeIn;

    // Cumulative production
    private BigDecimal cumulativeOilBbl;
    private BigDecimal cumulativeGasMscf;
    private BigDecimal cumulativeWaterBbl;
    private BigDecimal cumulativeRunHours;

    // For injectors
    private BigDecimal injectionRateBpd;
    private BigDecimal injectionPressurePsi;
    private BigDecimal cumulativeInjectionBbl;

    // Productivity indices (from tests/IPR)
    private BigDecimal productivityIndexBpdPsi;    // PI (J)
    private BigDecimal skinFactor;
    private BigDecimal flowEfficiency;

    // Venezuela-specific (CHOPS/Foamy)
    private Boolean isColdProduction;              // CHOPS well
    private BigDecimal sandProductionPercent;
    private Boolean hasFoamyBehavior;

    // Integration references
    private UUID drillingJobAssetId;               // Link to Drilling Module
    private UUID productionUnitAssetId;            // Link to Production Module

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "RV_WELL";
    public static final String ATTR_WELL_CODE = "well_code";
    public static final String ATTR_WELL_TYPE = "well_type";
    public static final String ATTR_WELL_STATUS = "well_status";
    public static final String ATTR_WELL_PURPOSE = "well_purpose";
    public static final String ATTR_WELL_CONFIGURATION = "well_configuration";
    public static final String ATTR_LIFT_METHOD = "lift_method";
    public static final String ATTR_SURFACE_LATITUDE = "surface_latitude";
    public static final String ATTR_SURFACE_LONGITUDE = "surface_longitude";
    public static final String ATTR_TOTAL_DEPTH_MD_M = "total_depth_md_m";
    public static final String ATTR_TOTAL_DEPTH_TVD_M = "total_depth_tvd_m";
    public static final String ATTR_SPUD_DATE = "spud_date";
    public static final String ATTR_COMPLETION_DATE = "completion_date";
    public static final String ATTR_FIRST_PRODUCTION_DATE = "first_production_date";
    public static final String ATTR_PRODUCTIVITY_INDEX = "productivity_index_bpd_psi";
    public static final String ATTR_IS_COLD_PRODUCTION = "is_cold_production";
}
