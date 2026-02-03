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

import jakarta.validation.constraints.*;
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

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    private String name;

    private String label;

    // Hierarchy
    @NotNull(message = "La cuenca (basin) es requerida")
    private UUID basinAssetId;

    // Field-specific attributes
    @Size(max = 50, message = "El código no debe exceder 50 caracteres")
    private String code;

    @Size(max = 50, message = "El estado operacional no debe exceder 50 caracteres")
    private String operationalStatus;    // PRODUCING, DEVELOPMENT, EXPLORATION, ABANDONED

    @Size(max = 100, message = "La compañía operadora no debe exceder 100 caracteres")
    private String operatorCompany;

    @Size(max = 50, message = "El tipo de campo no debe exceder 50 caracteres")
    private String fieldType;            // ONSHORE, OFFSHORE, TRANSITION

    @Min(value = 1800, message = "El año de descubrimiento debe ser >= 1800")
    @Max(value = 2100, message = "El año de descubrimiento debe ser <= 2100")
    private Integer discoveryYear;

    @Min(value = 1800, message = "El año de inicio de producción debe ser >= 1800")
    @Max(value = 2100, message = "El año de inicio de producción debe ser <= 2100")
    private Integer productionStartYear;

    // Geographic data
    @PositiveOrZero(message = "El área total debe ser >= 0")
    private BigDecimal totalAreaKm2;

    private JsonNode geojsonBoundary;

    @DecimalMin(value = "-90.0", message = "La latitud debe ser >= -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser <= 90")
    private BigDecimal centerLatitude;

    @DecimalMin(value = "-180.0", message = "La longitud debe ser >= -180")
    @DecimalMax(value = "180.0", message = "La longitud debe ser <= 180")
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
    public static final String ASSET_TYPE = "RV_FIELD";
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
