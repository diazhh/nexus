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

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 255, message = "El nombre no debe exceder 255 caracteres")
    private String name;

    private String label;

    // Related entities
    @NotNull(message = "El pozo (well) es requerido")
    private UUID wellAssetId;

    private UUID completionAssetId;

    // Model identification
    @Size(max = 50, message = "El código del modelo no debe exceder 50 caracteres")
    private String modelCode;

    private Long analysisDate;

    @Size(max = 50, message = "El método IPR no debe exceder 50 caracteres")
    private String iprMethod;            // VOGEL, DARCY, FETKOVICH, JONES, COMPOSITE

    // Reservoir conditions
    @PositiveOrZero(message = "La presión del yacimiento debe ser >= 0")
    private BigDecimal reservoirPressurePsi;    // Pr (current)

    @PositiveOrZero(message = "La presión de burbuja debe ser >= 0")
    private BigDecimal bubblePointPressurePsi;  // Pb

    @Positive(message = "La temperatura del yacimiento debe ser > 0")
    private BigDecimal reservoirTemperatureF;

    private Boolean isBelowBubblePoint;

    // Test data used
    @PositiveOrZero(message = "La tasa de prueba debe ser >= 0")
    private BigDecimal testRateBopd;

    @PositiveOrZero(message = "La presión de fondo (PWF) de prueba debe ser >= 0")
    private BigDecimal testPwfPsi;

    private Long testDate;

    @Size(max = 50, message = "El tipo de prueba no debe exceder 50 caracteres")
    private String testType;             // PRODUCTION_TEST, BUILDUP, DST

    // Calculated parameters - Productivity Index
    @PositiveOrZero(message = "El índice de productividad debe ser >= 0")
    private BigDecimal productivityIndexBpdPsi; // J (for undersaturated)

    @PositiveOrZero(message = "El índice de productividad sobre Pb debe ser >= 0")
    private BigDecimal productivityIndexAbovePb;

    @PositiveOrZero(message = "El índice de productividad bajo Pb debe ser >= 0")
    private BigDecimal productivityIndexBelowPb;

    // Calculated parameters - Vogel
    @PositiveOrZero(message = "El qmax debe ser >= 0")
    private BigDecimal qmaxBopd;         // AOF (Absolute Open Flow)

    @PositiveOrZero(message = "El coeficiente de Vogel debe ser >= 0")
    private BigDecimal vogelCoefficient; // Usually 0.8

    // Calculated parameters - Fetkovich
    @PositiveOrZero(message = "El coeficiente C de Fetkovich debe ser >= 0")
    private BigDecimal fetkovichC;       // Coefficient

    @DecimalMin(value = "0.5", message = "El exponente N de Fetkovich debe ser >= 0.5")
    @DecimalMax(value = "1.0", message = "El exponente N de Fetkovich debe ser <= 1.0")
    private BigDecimal fetkovichN;       // Exponent (0.5-1.0)

    // Calculated parameters - Jones (Turbulence)
    @PositiveOrZero(message = "El coeficiente laminar A de Jones debe ser >= 0")
    private BigDecimal jonesA;           // Laminar coefficient

    @PositiveOrZero(message = "El coeficiente turbulento B de Jones debe ser >= 0")
    private BigDecimal jonesB;           // Turbulent coefficient

    // IPR curve data points (JSON array for chart)
    private JsonNode iprCurveData;       // [{pwf, rate}, ...]

    // Operating point
    @PositiveOrZero(message = "La presión de fondo actual debe ser >= 0")
    private BigDecimal currentPwfPsi;

    @PositiveOrZero(message = "La tasa actual debe ser >= 0")
    private BigDecimal currentRateBopd;

    @DecimalMin(value = "0.0", message = "La eficiencia operativa debe ser >= 0")
    @DecimalMax(value = "100.0", message = "La eficiencia operativa debe ser <= 100")
    private BigDecimal operatingEfficiencyPercent;

    // Skin and damage
    private BigDecimal skinFactor;

    @PositiveOrZero(message = "La relación de daño debe ser >= 0")
    private BigDecimal damageRatio;

    @DecimalMin(value = "0.0", message = "La eficiencia de flujo debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La eficiencia de flujo debe ser <= 1")
    private BigDecimal flowEfficiency;

    @PositiveOrZero(message = "El qmax ideal debe ser >= 0")
    private BigDecimal idealQmaxBopd;    // qmax without skin

    // For gas wells
    @PositiveOrZero(message = "El AOF de gas debe ser >= 0")
    private BigDecimal aofMscfd;         // Gas AOF

    @PositiveOrZero(message = "El coeficiente C de backpressure debe ser >= 0")
    private BigDecimal backpressureC;

    @PositiveOrZero(message = "El exponente N de backpressure debe ser >= 0")
    private BigDecimal backpressureN;

    // Quality indicators
    @DecimalMin(value = "0.0", message = "El coeficiente R² debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El coeficiente R² debe ser <= 1")
    private BigDecimal r2Coefficient;    // Correlation coefficient

    @Size(max = 50, message = "La calidad del modelo no debe exceder 50 caracteres")
    private String modelQuality;         // EXCELLENT, GOOD, FAIR, POOR

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "RV_IPR_MODEL";
    public static final String ATTR_MODEL_CODE = "model_code";
    public static final String ATTR_ANALYSIS_DATE = "analysis_date";
    public static final String ATTR_IPR_METHOD = "ipr_method";
    public static final String ATTR_RESERVOIR_PRESSURE_PSI = "reservoir_pressure_psi";
    public static final String ATTR_PRODUCTIVITY_INDEX = "productivity_index_bpd_psi";
    public static final String ATTR_QMAX_BOPD = "qmax_bopd";
    public static final String ATTR_SKIN_FACTOR = "skin_factor";
    public static final String ATTR_FLOW_EFFICIENCY = "flow_efficiency";
}
