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
 * Data Transfer Object for PCP (Progressing Cavity Pump) System.
 * PCP systems use a helical rotor inside a stator to create a series of sealed cavities
 * that move fluid from the intake to the discharge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfPcpSystemDto {

    // Asset type constant for ThingsBoard
    public static final String ASSET_TYPE = "pf_pcp_system";

    // Attribute key constants
    public static final String ATTR_PUMP_MODEL = "pump_model";
    public static final String ATTR_PUMP_SERIAL_NUMBER = "pump_serial_number";
    public static final String ATTR_ROTOR_TYPE = "rotor_type";
    public static final String ATTR_STATOR_ELASTOMER = "stator_elastomer";
    public static final String ATTR_STAGES = "stages";
    public static final String ATTR_NOMINAL_DISPLACEMENT_BPD_RPM = "nominal_displacement_bpd_rpm";
    public static final String ATTR_MAX_HEAD_FT = "max_head_ft";
    public static final String ATTR_DRIVE_TYPE = "drive_type";
    public static final String ATTR_MOTOR_HP = "motor_hp";
    public static final String ATTR_GEARBOX_RATIO = "gearbox_ratio";
    public static final String ATTR_ROD_STRING_TYPE = "rod_string_type";
    public static final String ATTR_SETTING_DEPTH_FT = "setting_depth_ft";
    public static final String ATTR_INSTALLATION_DATE = "installation_date";
    public static final String ATTR_LAST_PULLING_DATE = "last_pulling_date";
    public static final String ATTR_RUN_LIFE_DAYS = "run_life_days";
    public static final String ATTR_MIN_RPM = "min_rpm";
    public static final String ATTR_MAX_RPM = "max_rpm";
    public static final String ATTR_MAX_TORQUE_FT_LB = "max_torque_ft_lb";
    public static final String ATTR_MAX_ROD_LOAD_LB = "max_rod_load_lb";
    public static final String ATTR_MAX_FLUID_TEMP_F = "max_fluid_temp_f";

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
     * Rotor type (e.g., Chrome, Coated, Stainless)
     */
    @Size(max = 50, message = "Rotor type cannot exceed 50 characters")
    private String rotorType;

    /**
     * Stator elastomer material (e.g., NBR, HNBR, EPDM, FKM)
     */
    @Size(max = 50, message = "Stator elastomer cannot exceed 50 characters")
    private String statorElastomer;

    /**
     * Number of pump stages (lobes)
     */
    @Positive(message = "Stages must be > 0")
    private Integer stages;

    /**
     * Nominal displacement per revolution (BPD per RPM)
     */
    @PositiveOrZero(message = "Nominal displacement must be >= 0")
    private BigDecimal nominalDisplacementBpdRpm;

    /**
     * Maximum head capacity in feet
     */
    @PositiveOrZero(message = "Max head must be >= 0")
    private BigDecimal maxHeadFt;

    /**
     * Drive type (SURFACE_DRIVE or DOWNHOLE_DRIVE)
     */
    private PcpDriveType driveType;

    /**
     * Motor horsepower
     */
    @PositiveOrZero(message = "Motor HP must be >= 0")
    private BigDecimal motorHp;

    /**
     * Gearbox ratio (for surface drive)
     */
    @PositiveOrZero(message = "Gearbox ratio must be >= 0")
    private BigDecimal gearboxRatio;

    /**
     * Rod string type (e.g., Solid, Hollow, Continuous)
     */
    @Size(max = 50, message = "Rod string type cannot exceed 50 characters")
    private String rodStringType;

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
     * Last pulling date
     */
    private LocalDate lastPullingDate;

    /**
     * Run life in days since installation
     */
    @PositiveOrZero(message = "Run life must be >= 0")
    private Integer runLifeDays;

    // Operational Limits

    /**
     * Minimum operating RPM
     */
    @PositiveOrZero(message = "Min RPM must be >= 0")
    private BigDecimal minRpm;

    /**
     * Maximum operating RPM
     */
    @PositiveOrZero(message = "Max RPM must be >= 0")
    private BigDecimal maxRpm;

    /**
     * Maximum rod torque in ft-lb
     */
    @PositiveOrZero(message = "Max torque must be >= 0")
    private BigDecimal maxTorqueFtLb;

    /**
     * Maximum rod load in pounds
     */
    @PositiveOrZero(message = "Max rod load must be >= 0")
    private BigDecimal maxRodLoadLb;

    /**
     * Maximum fluid temperature in Fahrenheit
     */
    @PositiveOrZero(message = "Max fluid temp must be >= 0")
    private BigDecimal maxFluidTempF;

    /**
     * Creation timestamp
     */
    private Long createdTime;

    /**
     * Last update timestamp
     */
    private Long updatedTime;

    /**
     * PCP Drive Type enum
     */
    public enum PcpDriveType {
        SURFACE_DRIVE,      // Surface-mounted drive head with rod string
        DOWNHOLE_DRIVE      // Electric Submersible PCP (ESP-like downhole motor)
    }
}
