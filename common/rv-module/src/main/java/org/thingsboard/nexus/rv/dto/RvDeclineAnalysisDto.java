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
    private UUID tenantId;
    private String name;
    private String label;

    // Related entities
    private UUID wellAssetId;
    private UUID reservoirAssetId;

    // Analysis identification
    private String analysisCode;
    private Long analysisDate;
    private Long dataStartDate;
    private Long dataEndDate;

    // Decline type
    private String declineType;          // EXPONENTIAL, HYPERBOLIC, HARMONIC
    private String declinePhase;         // PRIMARY, SECONDARY, TERTIARY

    // Arps parameters
    private BigDecimal qiBopd;           // Initial rate
    private BigDecimal diPerYear;        // Initial decline rate (1/year)
    private BigDecimal diPerMonth;       // Initial decline rate (1/month)
    private BigDecimal bExponent;        // Decline exponent (0=exp, 0<b<1=hyp, 1=harm)

    // For modified hyperbolic (transition to exponential)
    private BigDecimal dMinPerYear;      // Minimum decline rate
    private BigDecimal transitionTime;   // Time when D reaches Dmin

    // Calculated metrics
    private BigDecimal eurBbl;           // Estimated Ultimate Recovery
    private BigDecimal remainingReservesBbl;
    private BigDecimal cumulativeProductionBbl;
    private BigDecimal recoveryFactorPercent;

    // Forecast parameters
    private Long forecastEndDate;
    private BigDecimal economicLimitBopd;
    private BigDecimal forecastEurBbl;
    private Integer remainingLifeMonths;

    // Rate at specific times
    private BigDecimal rate1YearBopd;
    private BigDecimal rate3YearsBopd;
    private BigDecimal rate5YearsBopd;
    private BigDecimal rate10YearsBopd;

    // Cumulative at specific times
    private BigDecimal cumulative1YearBbl;
    private BigDecimal cumulative3YearsBbl;
    private BigDecimal cumulative5YearsBbl;

    // Quality metrics
    private BigDecimal r2Coefficient;    // R² of fit
    private BigDecimal standardError;
    private String fitQuality;           // EXCELLENT, GOOD, FAIR, POOR
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
