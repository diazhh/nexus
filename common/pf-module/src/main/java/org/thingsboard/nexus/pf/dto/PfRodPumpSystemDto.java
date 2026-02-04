/*
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
 * Data Transfer Object for Rod Pump (Sucker Rod Pump / Beam Pump) System.
 * Rod pump systems use a surface pumping unit to drive a downhole plunger pump
 * via a string of sucker rods.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfRodPumpSystemDto {

    // Asset type constant for ThingsBoard
    public static final String ASSET_TYPE = "pf_rod_pump_system";

    // Attribute key constants
    public static final String ATTR_PUMPING_UNIT_TYPE = "pumping_unit_type";
    public static final String ATTR_PUMPING_UNIT_SIZE = "pumping_unit_size";
    public static final String ATTR_PUMP_TYPE = "pump_type";
    public static final String ATTR_PUMP_BORE_IN = "pump_bore_in";
    public static final String ATTR_PUMP_SERIAL_NUMBER = "pump_serial_number";
    public static final String ATTR_STROKE_LENGTH_IN = "stroke_length_in";
    public static final String ATTR_SPM = "spm";
    public static final String ATTR_MOTOR_HP = "motor_hp";
    public static final String ATTR_GEARBOX_RATING = "gearbox_rating";
    public static final String ATTR_ROD_STRING_DESIGN = "rod_string_design";
    public static final String ATTR_SETTING_DEPTH_FT = "setting_depth_ft";
    public static final String ATTR_TUBING_SIZE_IN = "tubing_size_in";
    public static final String ATTR_TUBING_ANCHOR = "tubing_anchor";
    public static final String ATTR_COUNTERBALANCE_TYPE = "counterbalance_type";
    public static final String ATTR_INSTALLATION_DATE = "installation_date";
    public static final String ATTR_LAST_DYNAMOMETER_DATE = "last_dynamometer_date";
    public static final String ATTR_RUN_LIFE_DAYS = "run_life_days";
    public static final String ATTR_MIN_SPM = "min_spm";
    public static final String ATTR_MAX_SPM = "max_spm";
    public static final String ATTR_MAX_PEAK_LOAD_LB = "max_peak_load_lb";
    public static final String ATTR_MIN_LOAD_LB = "min_load_lb";
    public static final String ATTR_MAX_TORQUE_IN_LB = "max_torque_in_lb";
    public static final String ATTR_MAX_MOTOR_AMPS = "max_motor_amps";

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
     * Pumping unit type (e.g., CONVENTIONAL, MARK_II, AIR_BALANCED, HYDRAULIC)
     */
    private PumpingUnitType pumpingUnitType;

    /**
     * Pumping unit size/designation (e.g., "C-228D-200-74", "M II-228D-213-86")
     */
    @Size(max = 100, message = "Pumping unit size cannot exceed 100 characters")
    private String pumpingUnitSize;

    /**
     * Pump type (e.g., TUBING, INSERT, ROD_INSERT)
     */
    @Size(max = 50, message = "Pump type cannot exceed 50 characters")
    private String pumpType;

    /**
     * Pump bore diameter in inches
     */
    @PositiveOrZero(message = "Pump bore must be >= 0")
    private BigDecimal pumpBoreIn;

    /**
     * Pump serial number
     */
    @Size(max = 100, message = "Serial number cannot exceed 100 characters")
    private String pumpSerialNumber;

    /**
     * Stroke length in inches
     */
    @Positive(message = "Stroke length must be > 0")
    private BigDecimal strokeLengthIn;

    /**
     * Strokes per minute (SPM)
     */
    @Positive(message = "SPM must be > 0")
    private BigDecimal spm;

    /**
     * Motor horsepower
     */
    @PositiveOrZero(message = "Motor HP must be >= 0")
    private BigDecimal motorHp;

    /**
     * Gearbox torque rating in in-lb
     */
    @PositiveOrZero(message = "Gearbox rating must be >= 0")
    private BigDecimal gearboxRating;

    /**
     * Rod string design description (e.g., "7/8\" 86 rods + 3/4\" 76 rods")
     */
    @Size(max = 255, message = "Rod string design cannot exceed 255 characters")
    private String rodStringDesign;

    /**
     * Pump setting depth in feet
     */
    @PositiveOrZero(message = "Setting depth must be >= 0")
    private BigDecimal settingDepthFt;

    /**
     * Tubing size in inches
     */
    @PositiveOrZero(message = "Tubing size must be >= 0")
    private BigDecimal tubingSizeIn;

    /**
     * Whether tubing anchor is installed
     */
    private Boolean tubingAnchor;

    /**
     * Counterbalance type (e.g., CRANK, BEAM, ROTAFLEX)
     */
    @Size(max = 50, message = "Counterbalance type cannot exceed 50 characters")
    private String counterbalanceType;

    /**
     * Installation date
     */
    private LocalDate installationDate;

    /**
     * Last dynamometer survey date
     */
    private LocalDate lastDynamometerDate;

    /**
     * Run life in days since installation
     */
    @PositiveOrZero(message = "Run life must be >= 0")
    private Integer runLifeDays;

    // Operational Limits

    /**
     * Minimum strokes per minute
     */
    @PositiveOrZero(message = "Min SPM must be >= 0")
    private BigDecimal minSpm;

    /**
     * Maximum strokes per minute
     */
    @PositiveOrZero(message = "Max SPM must be >= 0")
    private BigDecimal maxSpm;

    /**
     * Maximum polished rod peak load in pounds
     */
    @PositiveOrZero(message = "Max peak load must be >= 0")
    private BigDecimal maxPeakLoadLb;

    /**
     * Minimum polished rod load in pounds
     */
    @PositiveOrZero(message = "Min load must be >= 0")
    private BigDecimal minLoadLb;

    /**
     * Maximum gearbox torque in in-lb
     */
    @PositiveOrZero(message = "Max torque must be >= 0")
    private BigDecimal maxTorqueInLb;

    /**
     * Maximum motor current in Amps
     */
    @PositiveOrZero(message = "Max motor amps must be >= 0")
    private BigDecimal maxMotorAmps;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Last update timestamp
     */
    private Long updatedTime;

    /**
     * Pumping Unit Type enum
     */
    public enum PumpingUnitType {
        CONVENTIONAL,       // Conventional beam pumping unit
        MARK_II,            // Lufkin Mark II (torque balanced)
        AIR_BALANCED,       // Air-balanced pumping unit
        HYDRAULIC,          // Hydraulic pumping unit
        LONG_STROKE,        // Long stroke pumping units (e.g., Rotaflex)
        BEAM_BALANCED       // Beam-balanced pumping unit
    }
}
