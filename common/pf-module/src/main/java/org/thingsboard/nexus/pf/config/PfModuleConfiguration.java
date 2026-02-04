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
package org.thingsboard.nexus.pf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the PF (Production Facilities) Module.
 * Properties are loaded from application-pf.yml
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "pf.module")
@Data
public class PfModuleConfiguration {

    /**
     * Backend URL for external service calls
     */
    private String backendUrl = "http://localhost:8080";

    /**
     * Enable/disable telemetry processing
     */
    private boolean telemetryProcessingEnabled = true;

    /**
     * Enable/disable alarm evaluation
     */
    private boolean alarmEvaluationEnabled = true;

    /**
     * Minimum data quality score to accept telemetry (0.0 - 1.0)
     */
    private double minDataQualityScore = 0.7;

    /**
     * REST client timeout in milliseconds
     */
    private int restTimeout = 5000;

    /**
     * Telemetry batch size for batch processing
     */
    private int telemetryBatchSize = 100;

    /**
     * Alarm deadband percentage to prevent alarm flapping
     */
    private double alarmDeadbandPercent = 2.0;

    /**
     * Maximum rate of change per minute (percentage) before flagging data quality
     */
    private double maxRateOfChangePercentPerMinute = 10.0;

    @Bean
    public RestTemplate pfRestTemplate() {
        return new RestTemplate();
    }
}
