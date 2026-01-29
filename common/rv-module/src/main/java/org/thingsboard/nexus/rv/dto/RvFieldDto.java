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
 * DTO representing a Field (Campo) entity.
 * Maps to a ThingsBoard Asset of type "rv_field" with SERVER_SCOPE attributes.
 *
 * Hierarchy: Basin -> Field -> Reservoir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvFieldDto {

    // Asset identity
    private UUID assetId;
    private UUID tenantId;
    private String name;
    private String label;

    // Hierarchy
    private UUID basinAssetId;

    // Field-specific attributes
    private String code;
    private String operationalStatus;    // PRODUCING, DEVELOPMENT, EXPLORATION, ABANDONED
    private String operatorCompany;
    private String fieldType;            // ONSHORE, OFFSHORE, TRANSITION
    private Integer discoveryYear;
    private Integer productionStartYear;

    // Geographic data
    private BigDecimal totalAreaKm2;
    private JsonNode geojsonBoundary;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private BigDecimal waterDepthM;      // For offshore fields

    // Reservoir characteristics (aggregated)
    private String predominantFluidType; // OIL, GAS, CONDENSATE, HEAVY_OIL, EXTRA_HEAVY_OIL
    private BigDecimal averageApiGravity;
    private BigDecimal oilInPlaceMmbbl;
    private BigDecimal gasInPlaceBcf;
    private BigDecimal recoverableReservesMmbbl;

    // Production statistics (telemetry aggregates)
    private BigDecimal currentOilRateBopd;
    private BigDecimal currentGasRateMscfd;
    private BigDecimal currentWaterRateBwpd;
    private BigDecimal cumulativeOilMmbbl;
    private BigDecimal cumulativeGasBcf;
    private BigDecimal cumulativeWaterMmbbl;

    // Well counts
    private Integer activeProducerCount;
    private Integer activeInjectorCount;
    private Integer shutInWellCount;
    private Integer abandonedWellCount;

    // Venezuela-specific
    private String fajaSector;           // BOYACA, JUNIN, AYACUCHO, CARABOBO (if applicable)
    private Boolean requiresDiluent;
    private BigDecimal diluentRatio;

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "rv_field";
    public static final String ATTR_CODE = "code";
    public static final String ATTR_OPERATIONAL_STATUS = "operational_status";
    public static final String ATTR_OPERATOR_COMPANY = "operator_company";
    public static final String ATTR_FIELD_TYPE = "field_type";
    public static final String ATTR_DISCOVERY_YEAR = "discovery_year";
    public static final String ATTR_PRODUCTION_START_YEAR = "production_start_year";
    public static final String ATTR_TOTAL_AREA_KM2 = "total_area_km2";
    public static final String ATTR_PREDOMINANT_FLUID_TYPE = "predominant_fluid_type";
    public static final String ATTR_AVERAGE_API_GRAVITY = "average_api_gravity";
    public static final String ATTR_OIL_IN_PLACE_MMBBL = "oil_in_place_mmbbl";
    public static final String ATTR_FAJA_SECTOR = "faja_sector";
    public static final String ATTR_REQUIRES_DILUENT = "requires_diluent";
}
