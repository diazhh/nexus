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

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    private String name;

    private String label;

    // Hierarchy
    @NotNull(message = "El yacimiento (reservoir) es requerido")
    private UUID reservoirAssetId;

    private UUID zoneAssetId;
    private UUID fieldAssetId;          // Denormalized for quick access

    // Well identification
    @Size(max = 50, message = "El código del pozo no debe exceder 50 caracteres")
    private String wellCode;            // API/UWI number

    @Size(max = 100, message = "El nombre del pozo no debe exceder 100 caracteres")
    private String wellName;

    @Size(max = 100, message = "El alias del pozo no debe exceder 100 caracteres")
    private String wellAlias;

    @Size(max = 50, message = "El número de licencia no debe exceder 50 caracteres")
    private String licenseNumber;

    // Well classification
    @Size(max = 50, message = "El tipo de pozo no debe exceder 50 caracteres")
    private String wellType;            // PRODUCER, INJECTOR, OBSERVATION, DISPOSAL

    @Size(max = 50, message = "El estado del pozo no debe exceder 50 caracteres")
    private String wellStatus;          // ACTIVE, SHUT_IN, ABANDONED, DRILLING, COMPLETING

    @Size(max = 50, message = "El propósito del pozo no debe exceder 50 caracteres")
    private String wellPurpose;         // OIL, GAS, WATER_INJECTION, GAS_INJECTION, WAG

    @Size(max = 50, message = "La configuración del pozo no debe exceder 50 caracteres")
    private String wellConfiguration;   // VERTICAL, DEVIATED, HORIZONTAL, MULTILATERAL

    @Size(max = 50, message = "El método de levantamiento no debe exceder 50 caracteres")
    private String liftMethod;          // NATURAL_FLOW, ESP, SRP, GAS_LIFT, PCP, JET_PUMP

    // Location (surface)
    @DecimalMin(value = "-90.0", message = "La latitud debe ser >= -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser <= 90")
    private BigDecimal surfaceLatitude;

    @DecimalMin(value = "-180.0", message = "La longitud debe ser >= -180")
    @DecimalMax(value = "180.0", message = "La longitud debe ser <= 180")
    private BigDecimal surfaceLongitude;

    private BigDecimal surfaceElevationM;

    @Size(max = 100, message = "La ubicación superficial no debe exceder 100 caracteres")
    private String surfaceLocation;     // Pad name, slot number

    // Location (bottomhole)
    @DecimalMin(value = "-90.0", message = "La latitud del fondo debe ser >= -90")
    @DecimalMax(value = "90.0", message = "La latitud del fondo debe ser <= 90")
    private BigDecimal bottomholeLatitude;

    @DecimalMin(value = "-180.0", message = "La longitud del fondo debe ser >= -180")
    @DecimalMax(value = "180.0", message = "La longitud del fondo debe ser <= 180")
    private BigDecimal bottomholeLongitude;

    // Depths
    @PositiveOrZero(message = "La profundidad total MD debe ser >= 0")
    private BigDecimal totalDepthMdM;   // TD measured depth

    @PositiveOrZero(message = "La profundidad total TVD debe ser >= 0")
    private BigDecimal totalDepthTvdM;  // TD true vertical depth

    @PositiveOrZero(message = "El punto de arranque debe ser >= 0")
    private BigDecimal kickoffPointM;   // KOP for deviated wells

    private BigDecimal datumElevationM;

    // Drilling dates (integration with Drilling Module)
    private Long spudDate;
    private Long tdReachedDate;
    private Long completionDate;
    private Long firstProductionDate;

    // Casing program (summary)
    @PositiveOrZero(message = "La profundidad del casing superficial debe ser >= 0")
    private BigDecimal surfaceCasingDepthM;

    @Positive(message = "El diámetro del casing superficial debe ser > 0")
    private BigDecimal surfaceCasingDiameterIn;

    @PositiveOrZero(message = "La profundidad del casing intermedio debe ser >= 0")
    private BigDecimal intermediateCasingDepthM;

    @Positive(message = "El diámetro del casing intermedio debe ser > 0")
    private BigDecimal intermediateCasingDiameterIn;

    @PositiveOrZero(message = "La profundidad del casing de producción debe ser >= 0")
    private BigDecimal productionCasingDepthM;

    @Positive(message = "El diámetro del casing de producción debe ser > 0")
    private BigDecimal productionCasingDiameterIn;

    @PositiveOrZero(message = "La profundidad del liner debe ser >= 0")
    private BigDecimal linerDepthM;

    @Positive(message = "El diámetro del liner debe ser > 0")
    private BigDecimal linerDiameterIn;

    // Production data (current - telemetry references)
    @PositiveOrZero(message = "El rate de petróleo debe ser >= 0")
    private BigDecimal currentOilRateBopd;

    @PositiveOrZero(message = "El rate de gas debe ser >= 0")
    private BigDecimal currentGasRateMscfd;

    @PositiveOrZero(message = "El rate de agua debe ser >= 0")
    private BigDecimal currentWaterRateBwpd;

    @DecimalMin(value = "0.0", message = "El water cut debe ser >= 0")
    @DecimalMax(value = "100.0", message = "El water cut debe ser <= 100")
    private BigDecimal currentWaterCutPercent;

    @PositiveOrZero(message = "El GOR debe ser >= 0")
    private BigDecimal currentGorScfStb;

    @PositiveOrZero(message = "La presión de cabeza debe ser >= 0")
    private BigDecimal currentThpPsi;       // Tubing head pressure

    @PositiveOrZero(message = "El tamaño del choke debe ser >= 0")
    private BigDecimal currentChokeSizeIn;

    // Cumulative production
    @PositiveOrZero(message = "La producción acumulada de petróleo debe ser >= 0")
    private BigDecimal cumulativeOilBbl;

    @PositiveOrZero(message = "La producción acumulada de gas debe ser >= 0")
    private BigDecimal cumulativeGasMscf;

    @PositiveOrZero(message = "La producción acumulada de agua debe ser >= 0")
    private BigDecimal cumulativeWaterBbl;

    @PositiveOrZero(message = "Las horas acumuladas deben ser >= 0")
    private BigDecimal cumulativeRunHours;

    // For injectors
    @PositiveOrZero(message = "El rate de inyección debe ser >= 0")
    private BigDecimal injectionRateBpd;

    @PositiveOrZero(message = "La presión de inyección debe ser >= 0")
    private BigDecimal injectionPressurePsi;

    @PositiveOrZero(message = "La inyección acumulada debe ser >= 0")
    private BigDecimal cumulativeInjectionBbl;

    // Productivity indices (from tests/IPR)
    @PositiveOrZero(message = "El índice de productividad debe ser >= 0")
    private BigDecimal productivityIndexBpdPsi;    // PI (J)

    private BigDecimal skinFactor;

    @DecimalMin(value = "0.0", message = "La eficiencia de flujo debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La eficiencia de flujo debe ser <= 1")
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
