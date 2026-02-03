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
 * DTO representing a Decline Curve Analysis.
 * Maps to a ThingsBoard Asset of type "rv_decline_analysis" with SERVER_SCOPE attributes.
 *
 * Stores the parameters and results of production decline analysis (Arps).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvDeclineAnalysisDto {

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

    private UUID reservoirAssetId;

    // Analysis identification
    @Size(max = 50, message = "El código de análisis no debe exceder 50 caracteres")
    private String analysisCode;

    private Long analysisDate;
    private Long dataStartDate;
    private Long dataEndDate;

    // Decline type
    @Size(max = 50, message = "El tipo de declinación no debe exceder 50 caracteres")
    private String declineType;          // EXPONENTIAL, HYPERBOLIC, HARMONIC

    @Size(max = 50, message = "La fase de declinación no debe exceder 50 caracteres")
    private String declinePhase;         // PRIMARY, SECONDARY, TERTIARY

    // Arps parameters
    @Positive(message = "La tasa inicial (qi) debe ser > 0")
    private BigDecimal qiBopd;           // Initial rate

    @Positive(message = "La tasa de declinación anual (di) debe ser > 0")
    private BigDecimal diPerYear;        // Initial decline rate (1/year)

    @Positive(message = "La tasa de declinación mensual (di) debe ser > 0")
    private BigDecimal diPerMonth;       // Initial decline rate (1/month)

    @DecimalMin(value = "0.0", message = "El exponente b debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El exponente b debe ser <= 1")
    private BigDecimal bExponent;        // Decline exponent (0=exp, 0<b<1=hyp, 1=harm)

    // For modified hyperbolic (transition to exponential)
    @PositiveOrZero(message = "La tasa de declinación mínima debe ser >= 0")
    private BigDecimal dMinPerYear;      // Minimum decline rate

    @PositiveOrZero(message = "El tiempo de transición debe ser >= 0")
    private BigDecimal transitionTime;   // Time when D reaches Dmin

    // Calculated metrics
    @PositiveOrZero(message = "El EUR debe ser >= 0")
    private BigDecimal eurBbl;           // Estimated Ultimate Recovery

    @PositiveOrZero(message = "Las reservas remanentes deben ser >= 0")
    private BigDecimal remainingReservesBbl;

    @PositiveOrZero(message = "La producción acumulada debe ser >= 0")
    private BigDecimal cumulativeProductionBbl;

    @DecimalMin(value = "0.0", message = "El factor de recuperación debe ser >= 0")
    @DecimalMax(value = "100.0", message = "El factor de recuperación debe ser <= 100")
    private BigDecimal recoveryFactorPercent;

    // Forecast parameters
    private Long forecastEndDate;

    @PositiveOrZero(message = "El límite económico debe ser >= 0")
    private BigDecimal economicLimitBopd;

    @PositiveOrZero(message = "El EUR pronosticado debe ser >= 0")
    private BigDecimal forecastEurBbl;

    @PositiveOrZero(message = "La vida remanente debe ser >= 0")
    private Integer remainingLifeMonths;

    // Rate at specific times
    @PositiveOrZero(message = "La tasa a 1 año debe ser >= 0")
    private BigDecimal rate1YearBopd;

    @PositiveOrZero(message = "La tasa a 3 años debe ser >= 0")
    private BigDecimal rate3YearsBopd;

    @PositiveOrZero(message = "La tasa a 5 años debe ser >= 0")
    private BigDecimal rate5YearsBopd;

    @PositiveOrZero(message = "La tasa a 10 años debe ser >= 0")
    private BigDecimal rate10YearsBopd;

    // Cumulative at specific times
    @PositiveOrZero(message = "El acumulado a 1 año debe ser >= 0")
    private BigDecimal cumulative1YearBbl;

    @PositiveOrZero(message = "El acumulado a 3 años debe ser >= 0")
    private BigDecimal cumulative3YearsBbl;

    @PositiveOrZero(message = "El acumulado a 5 años debe ser >= 0")
    private BigDecimal cumulative5YearsBbl;

    // Quality metrics
    @DecimalMin(value = "0.0", message = "El coeficiente R² debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El coeficiente R² debe ser <= 1")
    private BigDecimal r2Coefficient;    // R² of fit

    @PositiveOrZero(message = "El error estándar debe ser >= 0")
    private BigDecimal standardError;

    @Size(max = 50, message = "La calidad del ajuste no debe exceder 50 caracteres")
    private String fitQuality;           // EXCELLENT, GOOD, FAIR, POOR

    @PositiveOrZero(message = "Los puntos de datos usados deben ser >= 0")
    private Integer dataPointsUsed;

    // Historical data (JSON array)
    private JsonNode historicalData;     // [{date, rate, cumulative}, ...]

    // Forecast data (JSON array)
    private JsonNode forecastData;       // [{date, rate, cumulative}, ...]

    // Comparison with other methods
    private BigDecimal eurExponentialBbl;
    private BigDecimal eurHyperbolicBbl;
    private BigDecimal eurHarmonicBbl;

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "RV_DECLINE_ANALYSIS";
    public static final String ATTR_ANALYSIS_CODE = "analysis_code";
    public static final String ATTR_ANALYSIS_DATE = "analysis_date";
    public static final String ATTR_DECLINE_TYPE = "decline_type";
    public static final String ATTR_QI_BOPD = "qi_bopd";
    public static final String ATTR_DI_PER_YEAR = "di_per_year";
    public static final String ATTR_B_EXPONENT = "b_exponent";
    public static final String ATTR_EUR_BBL = "eur_bbl";
    public static final String ATTR_R2_COEFFICIENT = "r2_coefficient";
    public static final String ATTR_ECONOMIC_LIMIT_BOPD = "economic_limit_bopd";
}
