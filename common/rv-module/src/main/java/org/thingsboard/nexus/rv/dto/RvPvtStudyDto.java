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
 * DTO representing a PVT Study entity.
 * Maps to a ThingsBoard Asset of type "rv_pvt_study" with SERVER_SCOPE attributes.
 *
 * Contains fluid properties from laboratory analysis or correlations.
 * Links to Reservoir via "CharacterizedBy" relation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvPvtStudyDto {

    // Asset identity
    private UUID assetId;
    private UUID tenantId;
    private String name;
    private String label;

    // Related entities
    private UUID reservoirAssetId;
    private UUID wellAssetId;           // Source well for sample

    // Study identification
    private String studyCode;
    private Long sampleDate;
    private String laboratoryName;
    private String sampleType;          // BOTTOMHOLE, RECOMBINED, SEPARATOR

    // Sample conditions
    private BigDecimal sampleDepthM;
    private BigDecimal samplePressurePsi;
    private BigDecimal sampleTemperatureF;

    // Stock tank oil properties
    private BigDecimal apiGravity;
    private BigDecimal specificGravityOil;    // γo
    private BigDecimal stockTankOilViscosityCp;

    // Reservoir fluid properties at bubble point
    private BigDecimal bubblePointPressurePsi;      // Pb
    private BigDecimal solutionGorAtPbScfStb;       // Rs at Pb
    private BigDecimal oilFvfAtPbRbStb;             // Bo at Pb
    private BigDecimal oilViscosityAtPbCp;          // μo at Pb
    private BigDecimal oilDensityAtPbLbFt3;         // ρo at Pb
    private BigDecimal oilCompressibilityAtPb1Psi;  // co at Pb

    // Gas properties
    private BigDecimal gasSpecificGravity;          // γg
    private BigDecimal gasZFactorAtPb;              // Z at Pb
    private BigDecimal gasFvfAtPbRcfScf;            // Bg at Pb
    private BigDecimal gasViscosityAtPbCp;          // μg at Pb

    // Water properties
    private BigDecimal waterSalinity;               // ppm NaCl equivalent
    private BigDecimal waterFvfAtReservoirRbStb;    // Bw
    private BigDecimal waterViscosityAtReservoirCp; // μw
    private BigDecimal waterCompressibility1Psi;    // cw

    // Composition (mole fractions)
    private BigDecimal moleFracN2;
    private BigDecimal moleFracCO2;
    private BigDecimal moleFracH2S;
    private BigDecimal moleFracC1;
    private BigDecimal moleFracC2;
    private BigDecimal moleFracC3;
    private BigDecimal moleFracIC4;
    private BigDecimal moleFracNC4;
    private BigDecimal moleFracIC5;
    private BigDecimal moleFracNC5;
    private BigDecimal moleFracC6;
    private BigDecimal moleFracC7Plus;
    private BigDecimal c7PlusMolecularWeight;
    private BigDecimal c7PlusSpecificGravity;

    // Correlation flags (if not lab data)
    private Boolean usesCorrelations;
    private String pbCorrelation;        // STANDING, VASQUEZ_BEGGS, GLASO
    private String rsCorrelation;        // STANDING, VASQUEZ_BEGGS, GLASO
    private String boCorrelation;        // STANDING, VASQUEZ_BEGGS, GLASO
    private String viscosityCorrelation; // BEGGS_ROBINSON, VASQUEZ_BEGGS

    // Venezuela-specific
    private Boolean hasFoamyBehavior;
    private BigDecimal foamCriticalGasSaturation;
    private BigDecimal pseudoBubblePointPsi;        // For foamy oil

    // PVT tables (stored as JSON arrays for pressure-dependent values)
    private JsonNode differentialLiberationData;    // P, Rs, Bo, Bg, μo
    private JsonNode separatorTestData;
    private JsonNode viscosityData;

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "rv_pvt_study";
    public static final String ATTR_STUDY_CODE = "study_code";
    public static final String ATTR_SAMPLE_DATE = "sample_date";
    public static final String ATTR_LABORATORY = "laboratory_name";
    public static final String ATTR_API_GRAVITY = "api_gravity";
    public static final String ATTR_BUBBLE_POINT_PSI = "bubble_point_pressure_psi";
    public static final String ATTR_SOLUTION_GOR = "solution_gor_at_pb_scf_stb";
    public static final String ATTR_OIL_FVF_AT_PB = "oil_fvf_at_pb_rb_stb";
    public static final String ATTR_GAS_SPECIFIC_GRAVITY = "gas_specific_gravity";
    public static final String ATTR_USES_CORRELATIONS = "uses_correlations";
    public static final String ATTR_HAS_FOAMY_BEHAVIOR = "has_foamy_behavior";
}
