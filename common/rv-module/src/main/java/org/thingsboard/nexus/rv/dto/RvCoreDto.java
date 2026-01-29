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
 * DTO for Core assets in the Reservoir Module.
 * Represents a core sample taken from a well with associated analysis data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvCoreDto {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;

    // Association
    private UUID wellId;            // Parent well
    private UUID reservoirId;       // Reservoir interval

    // Core Information
    private Integer coreNumber;     // Core sequence number in well
    private String coreDate;        // Date core was cut
    private String coreType;        // CONVENTIONAL, SIDEWALL, ROTARY_SIDEWALL

    // Depth Information (in meters)
    private BigDecimal topDepthMd;      // Top depth (MD)
    private BigDecimal bottomDepthMd;   // Bottom depth (MD)
    private BigDecimal topDepthTvd;     // Top depth (TVD)
    private BigDecimal bottomDepthTvd;  // Bottom depth (TVD)
    private BigDecimal cutLength;       // Total cut length (m)
    private BigDecimal recoveredLength; // Recovered length (m)
    private BigDecimal recovery;        // Recovery percentage

    // Formation
    private String formationName;
    private String lithologyPrimary;    // Main lithology
    private String lithologySecondary;  // Secondary lithology
    private String grainSize;           // FINE, MEDIUM, COARSE, etc.
    private String sorting;             // WELL_SORTED, MODERATELY_SORTED, POORLY_SORTED
    private String cementation;         // UNCONSOLIDATED, WEAKLY, MODERATELY, STRONGLY

    // Visual Description
    private String oilShow;             // NONE, STAIN, BLEEDING, SATURATED
    private String fluorescence;        // NONE, SPOTTY, PATCHY, UNIFORM
    private String oilOdor;             // NONE, SLIGHT, MODERATE, STRONG
    private String oilColor;

    // Routine Core Analysis (RCA) Results
    private Boolean rcaCompleted;
    private String rcaLaboratory;
    private String rcaDate;
    private BigDecimal avgPorosity;         // Average porosity (fraction)
    private BigDecimal avgPermeabilityH;    // Horizontal permeability (mD)
    private BigDecimal avgPermeabilityV;    // Vertical permeability (mD)
    private BigDecimal avgGrainDensity;     // Grain density (g/cc)
    private BigDecimal avgWaterSat;         // Residual water saturation

    // Special Core Analysis (SCAL) Results
    private Boolean scalCompleted;
    private String scalLaboratory;
    private String scalDate;
    private BigDecimal capillaryPressureEntry;  // Psi
    private BigDecimal irreducibleWaterSat;     // Swirr (fraction)
    private BigDecimal residualOilSat;          // Sor (fraction)
    private BigDecimal relPermEndpointWater;    // krw at Sor
    private BigDecimal relPermEndpointOil;      // kro at Swirr
    private BigDecimal wettability;             // Amott-Harvey index
    private String wettabilityClass;            // WATER_WET, OIL_WET, MIXED_WET

    // Geomechanical Properties
    private Boolean geomechCompleted;
    private BigDecimal youngModulus;        // GPa
    private BigDecimal poissonRatio;
    private BigDecimal uniaxialStrength;    // MPa
    private BigDecimal frictionAngle;       // degrees

    // Storage Location
    private String storageLocation;
    private String boxNumbers;              // Core box numbers
    private Boolean photographed;
    private Boolean ctScanned;

    // Venezuela-specific
    private Boolean heavyOilCore;
    private BigDecimal sandContent;         // For CHOPS evaluation
    private Boolean foamyOilTest;
    private BigDecimal foamyOilFactor;

    // File References
    private String corePhotosPath;
    private String coreReportPath;
    private String rcaReportPath;
    private String scalReportPath;

    // Extended metadata
    private JsonNode metadata;

    // Timestamps
    private Long createdTime;
    private Long updatedTime;

    // Core Type Constants
    public static final String CORE_CONVENTIONAL = "CONVENTIONAL";
    public static final String CORE_SIDEWALL = "SIDEWALL";
    public static final String CORE_ROTARY_SIDEWALL = "ROTARY_SIDEWALL";
    public static final String CORE_ORIENTED = "ORIENTED";

    // Oil Show Constants
    public static final String SHOW_NONE = "NONE";
    public static final String SHOW_STAIN = "STAIN";
    public static final String SHOW_BLEEDING = "BLEEDING";
    public static final String SHOW_SATURATED = "SATURATED";
}
