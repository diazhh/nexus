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
package org.thingsboard.nexus.po.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Production Optimization (PO) module.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "nexus.po")
public class PoModuleConfiguration {

    /**
     * Enable/disable the PO module
     */
    private boolean enabled = true;

    /**
     * Enable/disable automatic optimization scheduling
     */
    private boolean autoOptimizationEnabled = false;

    /**
     * Interval in minutes for automatic optimization runs
     */
    private int optimizationIntervalMinutes = 60;

    /**
     * Enable/disable health score calculation
     */
    private boolean healthScoreEnabled = true;

    /**
     * Interval in minutes for health score recalculation
     */
    private int healthScoreIntervalMinutes = 15;

    /**
     * Minimum data quality score required for optimization
     */
    private double minDataQualityForOptimization = 0.8;

    /**
     * Number of days of historical data to use for optimization
     */
    private int historicalDataDays = 30;

    /**
     * Enable/disable recommendation auto-expiry
     */
    private boolean recommendationAutoExpiry = true;

    /**
     * Hours after which pending recommendations expire
     */
    private int recommendationExpiryHours = 24;

    /**
     * ESP Optimizer specific configuration
     */
    private EspOptimizerConfig esp = new EspOptimizerConfig();

    /**
     * Gas Lift Allocator specific configuration
     */
    private GasLiftAllocatorConfig gasLift = new GasLiftAllocatorConfig();

    /**
     * PCP Optimizer specific configuration
     */
    private PcpOptimizerConfig pcp = new PcpOptimizerConfig();

    /**
     * Rod Pump Optimizer specific configuration
     */
    private RodPumpOptimizerConfig rodPump = new RodPumpOptimizerConfig();

    @Data
    public static class EspOptimizerConfig {
        /**
         * Minimum frequency change to recommend (Hz)
         */
        private double minFrequencyChange = 0.5;

        /**
         * Maximum frequency (Hz)
         */
        private double maxFrequency = 60.0;

        /**
         * Minimum frequency (Hz)
         */
        private double minFrequency = 30.0;

        /**
         * Target motor load percentage
         */
        private double targetMotorLoad = 75.0;

        /**
         * Maximum motor temperature (°F)
         */
        private double maxMotorTemperature = 280.0;
    }

    @Data
    public static class GasLiftAllocatorConfig {
        /**
         * Enable/disable gas lift optimization
         */
        private boolean enabled = true;

        /**
         * Maximum total gas injection rate (MSCF/day)
         */
        private double maxTotalGasRate = 10000.0;

        /**
         * Minimum gas injection per well (MSCF/day)
         */
        private double minGasPerWell = 50.0;

        /**
         * Maximum gas injection per well (MSCF/day)
         */
        private double maxGasPerWell = 2000.0;
    }

    @Data
    public static class PcpOptimizerConfig {
        /**
         * Enable/disable PCP optimization
         */
        private boolean enabled = true;

        /**
         * Minimum RPM change to recommend
         */
        private double minRpmChange = 5.0;

        /**
         * Maximum RPM
         */
        private double maxRpm = 500.0;

        /**
         * Minimum RPM
         */
        private double minRpm = 50.0;

        /**
         * Target torque percentage
         */
        private double targetTorque = 70.0;

        /**
         * Maximum torque (%)
         */
        private double maxTorque = 90.0;

        /**
         * Maximum rod load (lbs)
         */
        private double maxRodLoad = 15000.0;

        /**
         * Wear factor threshold for recommendations
         */
        private double wearFactorThreshold = 0.7;
    }

    @Data
    public static class RodPumpOptimizerConfig {
        /**
         * Enable/disable rod pump optimization
         */
        private boolean enabled = true;

        /**
         * Minimum SPM change to recommend
         */
        private double minSpmChange = 0.5;

        /**
         * Maximum SPM
         */
        private double maxSpm = 15.0;

        /**
         * Minimum SPM
         */
        private double minSpm = 3.0;

        /**
         * Maximum stroke length (inches)
         */
        private double maxStrokeLength = 144.0;

        /**
         * Minimum stroke length (inches)
         */
        private double minStrokeLength = 24.0;

        /**
         * Target fillage percentage
         */
        private double targetFillage = 85.0;

        /**
         * Minimum acceptable fillage (%)
         */
        private double minFillage = 50.0;

        /**
         * Maximum peak polished rod load (lbs)
         */
        private double maxPeakLoad = 25000.0;

        /**
         * Maximum rod stress (psi)
         */
        private double maxRodStress = 30000.0;

        /**
         * Optimal counterbalance range lower bound (%)
         */
        private double counterbalanceLow = 45.0;

        /**
         * Optimal counterbalance range upper bound (%)
         */
        private double counterbalanceHigh = 55.0;
    }
}
