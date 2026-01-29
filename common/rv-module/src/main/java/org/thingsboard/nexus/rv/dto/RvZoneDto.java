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
    private UUID tenantId;
    private String name;
    private String label;

    // Hierarchy
    private UUID reservoirAssetId;

    // Zone identification
    private String code;
    private String zoneName;
    private Integer zoneNumber;          // Sequential number within reservoir

    // Depth intervals
    private BigDecimal topDepthMdM;      // Top measured depth
    private BigDecimal bottomDepthMdM;   // Bottom measured depth
    private BigDecimal topDepthTvdssM;   // Top true vertical depth subsea
    private BigDecimal bottomDepthTvdssM;
    private BigDecimal grossThicknessM;
    private BigDecimal netPayThicknessM;
    private BigDecimal netToGrossRatio;

    // Petrophysical properties (zone average)
    private BigDecimal porosityFrac;
    private BigDecimal permeabilityMd;
    private BigDecimal waterSaturationFrac;
    private BigDecimal shaleVolumeFrac;

    // Rock properties
    private String lithology;            // SANDSTONE, LIMESTONE, DOLOMITE
    private BigDecimal grainDensityGcc;
    private BigDecimal clayContent;

    // Flow properties
    private BigDecimal kh;               // Permeability-thickness (md-ft)
    private BigDecimal skinFactor;
    private BigDecimal flowCapacityPercent; // Contribution to total flow

    // Saturation contacts (if applicable to this zone)
    private BigDecimal gocDepthM;        // Gas-oil contact
    private BigDecimal owcDepthM;        // Oil-water contact
    private BigDecimal fwlDepthM;        // Free water level

    // Production allocation
    private BigDecimal allocationFactorPercent;
    private BigDecimal cumulativeProductionBbl;

    // Status
    private String zoneStatus;           // PRODUCING, BEHIND_PIPE, WATERED_OUT, DEPLETED
    private Boolean isPerforated;
    private Boolean isIsolated;          // Mechanically isolated

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "rv_zone";
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
