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
 * DTO representing a Zone (Zona) entity.
 * Maps to a ThingsBoard Asset of type "rv_zone" with SERVER_SCOPE attributes.
 *
 * A Zone is a vertical subdivision within a Reservoir, typically representing
 * a distinct flow unit or layer with specific petrophysical properties.
 *
 * Hierarchy: Reservoir -> Zone -> Well (completion interval)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvZoneDto {

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

    // Zone identification
    @Size(max = 50, message = "El código no debe exceder 50 caracteres")
    private String code;

    @Size(max = 100, message = "El nombre de zona no debe exceder 100 caracteres")
    private String zoneName;

    @Positive(message = "El número de zona debe ser > 0")
    private Integer zoneNumber;          // Sequential number within reservoir

    // Depth intervals
    @PositiveOrZero(message = "La profundidad superior MD debe ser >= 0")
    private BigDecimal topDepthMdM;      // Top measured depth

    @PositiveOrZero(message = "La profundidad inferior MD debe ser >= 0")
    private BigDecimal bottomDepthMdM;   // Bottom measured depth

    private BigDecimal topDepthTvdssM;   // Top true vertical depth subsea
    private BigDecimal bottomDepthTvdssM;

    @PositiveOrZero(message = "El espesor bruto debe ser >= 0")
    private BigDecimal grossThicknessM;

    @PositiveOrZero(message = "El espesor neto debe ser >= 0")
    private BigDecimal netPayThicknessM;

    @DecimalMin(value = "0.0", message = "La relación net-to-gross debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La relación net-to-gross debe ser <= 1")
    private BigDecimal netToGrossRatio;

    // Petrophysical properties (zone average)
    @DecimalMin(value = "0.0", message = "La porosidad debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La porosidad debe ser <= 1")
    private BigDecimal porosityFrac;

    @PositiveOrZero(message = "La permeabilidad debe ser >= 0")
    private BigDecimal permeabilityMd;

    @DecimalMin(value = "0.0", message = "La saturación de agua debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación de agua debe ser <= 1")
    private BigDecimal waterSaturationFrac;

    @DecimalMin(value = "0.0", message = "El volumen de arcilla debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El volumen de arcilla debe ser <= 1")
    private BigDecimal shaleVolumeFrac;

    // Rock properties
    @Size(max = 50, message = "La litología no debe exceder 50 caracteres")
    private String lithology;            // SANDSTONE, LIMESTONE, DOLOMITE

    @Positive(message = "La densidad del grano debe ser > 0")
    private BigDecimal grainDensityGcc;

    @DecimalMin(value = "0.0", message = "El contenido de arcilla debe ser >= 0")
    @DecimalMax(value = "100.0", message = "El contenido de arcilla debe ser <= 100")
    private BigDecimal clayContent;

    // Flow properties
    @PositiveOrZero(message = "El kh debe ser >= 0")
    private BigDecimal kh;               // Permeability-thickness (md-ft)

    private BigDecimal skinFactor;

    @DecimalMin(value = "0.0", message = "La capacidad de flujo debe ser >= 0")
    @DecimalMax(value = "100.0", message = "La capacidad de flujo debe ser <= 100")
    private BigDecimal flowCapacityPercent; // Contribution to total flow

    // Saturation contacts (if applicable to this zone)
    @PositiveOrZero(message = "La profundidad del GOC debe ser >= 0")
    private BigDecimal gocDepthM;        // Gas-oil contact

    @PositiveOrZero(message = "La profundidad del OWC debe ser >= 0")
    private BigDecimal owcDepthM;        // Oil-water contact

    @PositiveOrZero(message = "La profundidad del FWL debe ser >= 0")
    private BigDecimal fwlDepthM;        // Free water level

    // Production allocation
    @DecimalMin(value = "0.0", message = "El factor de asignación debe ser >= 0")
    @DecimalMax(value = "100.0", message = "El factor de asignación debe ser <= 100")
    private BigDecimal allocationFactorPercent;

    @PositiveOrZero(message = "La producción acumulada debe ser >= 0")
    private BigDecimal cumulativeProductionBbl;

    // Status
    @Size(max = 50, message = "El estado de la zona no debe exceder 50 caracteres")
    private String zoneStatus;           // PRODUCING, BEHIND_PIPE, WATERED_OUT, DEPLETED

    private Boolean isPerforated;
    private Boolean isIsolated;          // Mechanically isolated

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "RV_ZONE";
    public static final String ATTR_CODE = "code";
    public static final String ATTR_ZONE_NUMBER = "zone_number";
    public static final String ATTR_TOP_DEPTH_MD_M = "top_depth_md_m";
    public static final String ATTR_BOTTOM_DEPTH_MD_M = "bottom_depth_md_m";
    public static final String ATTR_GROSS_THICKNESS_M = "gross_thickness_m";
    public static final String ATTR_NET_PAY_THICKNESS_M = "net_pay_thickness_m";
    public static final String ATTR_POROSITY_FRAC = "porosity_frac";
    public static final String ATTR_PERMEABILITY_MD = "permeability_md";
    public static final String ATTR_WATER_SATURATION_FRAC = "water_saturation_frac";
    public static final String ATTR_LITHOLOGY = "lithology";
    public static final String ATTR_ZONE_STATUS = "zone_status";
    public static final String ATTR_IS_PERFORATED = "is_perforated";
}
