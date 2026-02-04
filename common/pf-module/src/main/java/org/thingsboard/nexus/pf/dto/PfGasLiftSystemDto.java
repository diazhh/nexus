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
package org.thingsboard.nexus.pf.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for Gas Lift System.
 * Gas lift systems inject compressed gas into the production tubing to reduce
 * the hydrostatic pressure and allow the well to flow to surface.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfGasLiftSystemDto {

    // Asset type constant for ThingsBoard
    public static final String ASSET_TYPE = "pf_gas_lift_system";

    // Attribute key constants
    public static final String ATTR_GAS_LIFT_TYPE = "gas_lift_type";
    public static final String ATTR_NUM_VALVES = "num_valves";
    public static final String ATTR_OPERATING_VALVE_DEPTH_FT = "operating_valve_depth_ft";
    public static final String ATTR_INJECTION_POINT_DEPTH_FT = "injection_point_depth_ft";
    public static final String ATTR_MANDREL_SIZE = "mandrel_size";
    public static final String ATTR_VALVE_TYPE = "valve_type";
    public static final String ATTR_DESIGN_INJECTION_RATE_MSCFD = "design_injection_rate_mscfd";
    public static final String ATTR_DESIGN_INJECTION_PRESSURE_PSI = "design_injection_pressure_psi";
    public static final String ATTR_GAS_SPECIFIC_GRAVITY = "gas_specific_gravity";
    public static final String ATTR_CASING_PRESSURE_PSI = "casing_pressure_psi";
    public static final String ATTR_TUBING_SIZE_IN = "tubing_size_in";
    public static final String ATTR_CASING_SIZE_IN = "casing_size_in";
    public static final String ATTR_INSTALLATION_DATE = "installation_date";
    public static final String ATTR_LAST_SURVEY_DATE = "last_survey_date";
    public static final String ATTR_MIN_INJECTION_RATE_MSCFD = "min_injection_rate_mscfd";
    public static final String ATTR_MAX_INJECTION_RATE_MSCFD = "max_injection_rate_mscfd";
    public static final String ATTR_MIN_INJECTION_PRESSURE_PSI = "min_injection_pressure_psi";
    public static final String ATTR_MAX_INJECTION_PRESSURE_PSI = "max_injection_pressure_psi";
    public static final String ATTR_MIN_CASING_PRESSURE_PSI = "min_casing_pressure_psi";
    public static final String ATTR_MAX_CASING_PRESSURE_PSI = "max_casing_pressure_psi";

    /**
     * Asset ID in ThingsBoard
     */
    private UUID assetId;

    /**
     * Tenant ID
     */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    /**
     * Associated well ID
     */
    @NotNull(message = "Well ID is required")
    private UUID wellId;

    /**
     * Gas lift type (CONTINUOUS or INTERMITTENT)
     */
    @NotNull(message = "Gas lift type is required")
    private GasLiftType gasLiftType;

    /**
     * Number of gas lift valves installed
     */
    @Positive(message = "Number of valves must be > 0")
    private Integer numValves;

    /**
     * Operating (bottom) valve depth in feet
     */
    @PositiveOrZero(message = "Operating valve depth must be >= 0")
    private BigDecimal operatingValveDepthFt;

    /**
     * Gas injection point depth in feet
     */
    @PositiveOrZero(message = "Injection point depth must be >= 0")
    private BigDecimal injectionPointDepthFt;

    /**
     * Mandrel size (e.g., "1.5 inch", "1.0 inch")
     */
    @Size(max = 50, message = "Mandrel size cannot exceed 50 characters")
    private String mandrelSize;

    /**
     * Valve type (e.g., IPO - Injection Pressure Operated, PPO - Production Pressure Operated)
     */
    @Size(max = 50, message = "Valve type cannot exceed 50 characters")
    private String valveType;

    /**
     * Design injection rate in MSCF/D (thousand standard cubic feet per day)
     */
    @PositiveOrZero(message = "Design injection rate must be >= 0")
    private BigDecimal designInjectionRateMscfd;

    /**
     * Design injection pressure in PSI
     */
    @PositiveOrZero(message = "Design injection pressure must be >= 0")
    private BigDecimal designInjectionPressurePsi;

    /**
     * Gas specific gravity (air = 1.0)
     */
    @PositiveOrZero(message = "Gas specific gravity must be >= 0")
    private BigDecimal gasSpecificGravity;

    /**
     * Casing pressure in PSI
     */
    @PositiveOrZero(message = "Casing pressure must be >= 0")
    private BigDecimal casingPressurePsi;

    /**
     * Tubing size in inches
     */
    @PositiveOrZero(message = "Tubing size must be >= 0")
    private BigDecimal tubingSizeIn;

    /**
     * Casing size in inches
     */
    @PositiveOrZero(message = "Casing size must be >= 0")
    private BigDecimal casingSizeIn;

    /**
     * Installation date
     */
    private LocalDate installationDate;

    /**
     * Last survey/optimization date
     */
    private LocalDate lastSurveyDate;

    // Operational Limits

    /**
     * Minimum injection rate in MSCF/D
     */
    @PositiveOrZero(message = "Min injection rate must be >= 0")
    private BigDecimal minInjectionRateMscfd;

    /**
     * Maximum injection rate in MSCF/D
     */
    @PositiveOrZero(message = "Max injection rate must be >= 0")
    private BigDecimal maxInjectionRateMscfd;

    /**
     * Minimum injection pressure in PSI
     */
    @PositiveOrZero(message = "Min injection pressure must be >= 0")
    private BigDecimal minInjectionPressurePsi;

    /**
     * Maximum injection pressure in PSI
     */
    @PositiveOrZero(message = "Max injection pressure must be >= 0")
    private BigDecimal maxInjectionPressurePsi;

    /**
     * Minimum casing pressure in PSI
     */
    @PositiveOrZero(message = "Min casing pressure must be >= 0")
    private BigDecimal minCasingPressurePsi;

    /**
     * Maximum casing pressure in PSI
     */
    @PositiveOrZero(message = "Max casing pressure must be >= 0")
    private BigDecimal maxCasingPressurePsi;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Last update timestamp
     */
    private Long updatedTime;

    /**
     * Gas Lift Type enum
     */
    public enum GasLiftType {
        CONTINUOUS,     // Continuous gas injection for steady-state production
        INTERMITTENT    // Periodic gas slugs for lower productivity wells
    }
}
