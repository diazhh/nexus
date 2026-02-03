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
 * DTO representing a Basin (Cuenca) entity.
 * Maps to a ThingsBoard Asset of type "rv_basin" with SERVER_SCOPE attributes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvBasinDto {

    // Asset identity
    private UUID assetId;

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    private String name;

    private String label;

    // Basin-specific attributes (stored as SERVER_SCOPE attributes in TB)
    @Size(max = 50, message = "El código no debe exceder 50 caracteres")
    private String code;

    @Size(max = 50, message = "El tipo de cuenca no debe exceder 50 caracteres")
    private String basinType;        // SEDIMENTARY, FORELAND, RIFT, INTRACRATONIC

    @Size(max = 100, message = "El país no debe exceder 100 caracteres")
    private String country;

    @Size(max = 100, message = "La región no debe exceder 100 caracteres")
    private String region;

    @PositiveOrZero(message = "El área total debe ser >= 0")
    private BigDecimal totalAreaKm2;

    @PositiveOrZero(message = "Las reservas estimadas deben ser >= 0")
    private BigDecimal estimatedReservesMmbbl;

    // Geographic data
    private JsonNode geojsonBoundary;  // GeoJSON polygon for basin boundary

    @DecimalMin(value = "-90.0", message = "La latitud debe ser >= -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser <= 90")
    private BigDecimal centerLatitude;

    @DecimalMin(value = "-180.0", message = "La longitud debe ser >= -180")
    @DecimalMax(value = "180.0", message = "La longitud debe ser <= 180")
    private BigDecimal centerLongitude;

    // Statistics (can be computed or stored as telemetry)
    private Integer fieldCount;
    private Integer wellCount;
    private BigDecimal totalProductionMmbbl;

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "RV_BASIN";
    public static final String ATTR_CODE = "code";
    public static final String ATTR_BASIN_TYPE = "basin_type";
    public static final String ATTR_COUNTRY = "country";
    public static final String ATTR_REGION = "region";
    public static final String ATTR_TOTAL_AREA_KM2 = "total_area_km2";
    public static final String ATTR_ESTIMATED_RESERVES_MMBBL = "estimated_reserves_mmbbl";
    public static final String ATTR_GEOJSON_BOUNDARY = "geojson_boundary";
    public static final String ATTR_CENTER_LATITUDE = "center_latitude";
    public static final String ATTR_CENTER_LONGITUDE = "center_longitude";
}
