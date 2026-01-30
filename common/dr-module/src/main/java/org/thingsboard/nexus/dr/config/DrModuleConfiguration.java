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
package org.thingsboard.nexus.dr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the Drilling Module
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "dr.module")
@Data
public class DrModuleConfiguration {

    /**
     * URL of the backend for REST calls from rule nodes
     */
    private String backendUrl = "http://localhost:8080";

    /**
     * Enable automatic MSE (Mechanical Specific Energy) calculation
     */
    private boolean mseCalculationEnabled = true;

    /**
     * Enable automatic ECD (Equivalent Circulating Density) calculation
     */
    private boolean ecdCalculationEnabled = true;

    /**
     * Enable automatic kick detection
     */
    private boolean kickDetectionEnabled = true;

    /**
     * Pit gain threshold in barrels for kick warning
     */
    private double kickPitGainThresholdBbl = 5.0;

    /**
     * Flow out increase percentage threshold for kick detection
     */
    private double kickFlowIncreasePercent = 10.0;

    /**
     * Total gas threshold in units for high gas alarm
     */
    private double highGasThresholdUnits = 500.0;

    /**
     * H2S threshold in ppm for critical alarm
     */
    private double h2sThresholdPpm = 10.0;

    /**
     * MSE foundering factor (MSE > factor × estimated rock strength)
     */
    private double mseFounderingFactor = 3.0;

    /**
     * ECD safety margin below fracture gradient (ppg)
     */
    private double ecdFracSafetyMarginPpg = 0.3;

    /**
     * ECD safety margin above pore pressure (ppg)
     */
    private double ecdPoreSafetyMarginPpg = 0.3;

    /**
     * BOP test overdue threshold in days
     */
    private int bopTestOverdueDays = 14;

    /**
     * Connection lost threshold in minutes
     */
    private int connectionLostThresholdMinutes = 5;

    /**
     * Enable automatic rig state detection
     */
    private boolean rigStateDetectionEnabled = true;

    /**
     * Timeout for REST calls in milliseconds
     */
    private int restTimeout = 5000;

    @Bean
    public RestTemplate drRestTemplate() {
        return new RestTemplate();
    }
}
