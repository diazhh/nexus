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
 * DTO representing an IPR (Inflow Performance Relationship) Model.
 * Maps to a ThingsBoard Asset of type "rv_ipr_model" with SERVER_SCOPE attributes.
 *
 * Stores the parameters and results of IPR analysis for a well/completion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvIprModelDto {

    // Asset identity
    private UUID assetId;
    private UUID tenantId;
    private String name;
    private String label;

    // Related entities
    private UUID wellAssetId;
    private UUID completionAssetId;

    // Model identification
    private String modelCode;
    private Long analysisDate;
    private String iprMethod;            // VOGEL, DARCY, FETKOVICH, JONES, COMPOSITE

    // Reservoir conditions
    private BigDecimal reservoirPressurePsi;    // Pr (current)
    private BigDecimal bubblePointPressurePsi;  // Pb
    private BigDecimal reservoirTemperatureF;
    private Boolean isBelowBubblePoint;

    // Test data used
    private BigDecimal testRateBopd;
    private BigDecimal testPwfPsi;
    private Long testDate;
    private String testType;             // PRODUCTION_TEST, BUILDUP, DST

    // Calculated parameters - Productivity Index
    private BigDecimal productivityIndexBpdPsi; // J (for undersaturated)
    private BigDecimal productivityIndexAbovePb;
    private BigDecimal productivityIndexBelowPb;

    // Calculated parameters - Vogel
    private BigDecimal qmaxBopd;         // AOF (Absolute Open Flow)
    private BigDecimal vogelCoefficient; // Usually 0.8

    // Calculated parameters - Fetkovich
    private BigDecimal fetkovichC;       // Coefficient
    private BigDecimal fetkovichN;       // Exponent (0.5-1.0)

    // Calculated parameters - Jones (Turbulence)
    private BigDecimal jonesA;           // Laminar coefficient
    private BigDecimal jonesB;           // Turbulent coefficient

    // IPR curve data points (JSON array for chart)
    private JsonNode iprCurveData;       // [{pwf, rate}, ...]

    // Operating point
    private BigDecimal currentPwfPsi;
    private BigDecimal currentRateBopd;
    private BigDecimal operatingEfficiencyPercent;

    // Skin and damage
    private BigDecimal skinFactor;
    private BigDecimal damageRatio;
    private BigDecimal flowEfficiency;
    private BigDecimal idealQmaxBopd;    // qmax without skin

    // For gas wells
    private BigDecimal aofMscfd;         // Gas AOF
    private BigDecimal backpressureC;
    private BigDecimal backpressureN;

    // Quality indicators
    private BigDecimal r2Coefficient;    // Correlation coefficient
    private String modelQuality;         // EXCELLENT, GOOD, FAIR, POOR

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "rv_ipr_model";
    public static final String ATTR_MODEL_CODE = "model_code";
    public static final String ATTR_ANALYSIS_DATE = "analysis_date";
    public static final String ATTR_IPR_METHOD = "ipr_method";
    public static final String ATTR_RESERVOIR_PRESSURE_PSI = "reservoir_pressure_psi";
    public static final String ATTR_PRODUCTIVITY_INDEX = "productivity_index_bpd_psi";
    public static final String ATTR_QMAX_BOPD = "qmax_bopd";
    public static final String ATTR_SKIN_FACTOR = "skin_factor";
    public static final String ATTR_FLOW_EFFICIENCY = "flow_efficiency";
}
