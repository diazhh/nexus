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
 * Data Transfer Object for ESP (Electric Submersible Pump) System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfEspSystemDto {

    // Asset type constant for ThingsBoard
    public static final String ASSET_TYPE = "pf_esp_system";

    // Attribute key constants
    public static final String ATTR_PUMP_MODEL = "pump_model";
    public static final String ATTR_PUMP_SERIAL_NUMBER = "pump_serial_number";
    public static final String ATTR_STAGES = "stages";
    public static final String ATTR_RATED_HEAD_FT = "rated_head_ft";
    public static final String ATTR_RATED_FLOW_BPD = "rated_flow_bpd";
    public static final String ATTR_MOTOR_HP = "motor_hp";
    public static final String ATTR_MOTOR_VOLTAGE = "motor_voltage";
    public static final String ATTR_FREQUENCY_HZ = "frequency_hz";
    public static final String ATTR_SETTING_DEPTH_FT = "setting_depth_ft";
    public static final String ATTR_INSTALLATION_DATE = "installation_date";
    public static final String ATTR_LAST_PULLING_DATE = "last_pulling_date";
    public static final String ATTR_RUN_LIFE_DAYS = "run_life_days";
    public static final String ATTR_MIN_FREQUENCY_HZ = "min_frequency_hz";
    public static final String ATTR_MAX_FREQUENCY_HZ = "max_frequency_hz";
    public static final String ATTR_MIN_CURRENT_AMPS = "min_current_amps";
    public static final String ATTR_MAX_CURRENT_AMPS = "max_current_amps";
    public static final String ATTR_MAX_MOTOR_TEMP_F = "max_motor_temp_f";
    public static final String ATTR_MIN_PIP_PSI = "min_pip_psi";
    public static final String ATTR_MAX_VIBRATION_G = "max_vibration_g";

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
     * Pump model/brand
     */
    @Size(max = 100, message = "Pump model cannot exceed 100 characters")
    private String pumpModel;

    /**
     * Pump serial number
     */
    @Size(max = 100, message = "Serial number cannot exceed 100 characters")
    private String pumpSerialNumber;

    /**
     * Number of pump stages
     */
    @Positive(message = "Stages must be > 0")
    private Integer stages;

    /**
     * Rated head in feet
     */
    @PositiveOrZero(message = "Rated head must be >= 0")
    private BigDecimal ratedHeadFt;

    /**
     * Rated flow in barrels per day
     */
    @PositiveOrZero(message = "Rated flow must be >= 0")
    private BigDecimal ratedFlowBpd;

    /**
     * Motor horsepower
     */
    @PositiveOrZero(message = "Motor HP must be >= 0")
    private BigDecimal motorHp;

    /**
     * Motor voltage
     */
    @Positive(message = "Motor voltage must be > 0")
    private Integer motorVoltage;

    /**
     * Operating frequency in Hz
     */
    @PositiveOrZero(message = "Frequency must be >= 0")
    private BigDecimal frequencyHz;

    /**
     * Setting depth in feet
     */
    @PositiveOrZero(message = "Setting depth must be >= 0")
    private BigDecimal settingDepthFt;

    /**
     * Installation date
     */
    private LocalDate installationDate;

    /**
     * Last pulling date (when ESP was pulled for maintenance)
     */
    private LocalDate lastPullingDate;

    /**
     * Run life in days since installation
     */
    @PositiveOrZero(message = "Run life must be >= 0")
    private Integer runLifeDays;

    // Operational Limits

    /**
     * Minimum allowed frequency in Hz
     */
    @PositiveOrZero(message = "Min frequency must be >= 0")
    private BigDecimal minFrequencyHz;

    /**
     * Maximum allowed frequency in Hz
     */
    @PositiveOrZero(message = "Max frequency must be >= 0")
    private BigDecimal maxFrequencyHz;

    /**
     * Minimum expected current in Amps
     */
    @PositiveOrZero(message = "Min current must be >= 0")
    private BigDecimal minCurrentAmps;

    /**
     * Maximum allowed current in Amps
     */
    @PositiveOrZero(message = "Max current must be >= 0")
    private BigDecimal maxCurrentAmps;

    /**
     * Maximum allowed motor temperature in Fahrenheit
     */
    @PositiveOrZero(message = "Max motor temp must be >= 0")
    private BigDecimal maxMotorTempF;

    /**
     * Minimum pump intake pressure in PSI
     */
    @PositiveOrZero(message = "Min PIP must be >= 0")
    private BigDecimal minPipPsi;

    /**
     * Maximum allowed vibration in G
     */
    @PositiveOrZero(message = "Max vibration must be >= 0")
    private BigDecimal maxVibrationG;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Last update timestamp
     */
    private Long updatedTime;
}
