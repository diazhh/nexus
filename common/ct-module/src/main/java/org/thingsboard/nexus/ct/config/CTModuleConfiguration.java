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
package org.thingsboard.nexus.ct.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración del módulo Coiled Tubing
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "ct.module")
@Data
public class CTModuleConfiguration {

    /**
     * URL del backend para llamadas REST desde nodos
     */
    private String backendUrl = "http://localhost:8080";

    /**
     * Habilitar cálculo automático de fatiga
     */
    private boolean fatigueCalculationEnabled = true;

    /**
     * Umbral de fatiga para alarma crítica (%)
     */
    private double criticalFatigueThreshold = 95.0;

    /**
     * Umbral de fatiga para alarma alta (%)
     */
    private double highFatigueThreshold = 80.0;

    /**
     * Habilitar simulación de trabajos
     */
    private boolean jobSimulationEnabled = true;

    /**
     * Número de pasos para análisis de simulación
     */
    private int simulationSteps = 100;

    /**
     * Timeout para llamadas REST (ms)
     */
    private int restTimeout = 5000;

    @Bean
    public RestTemplate ctRestTemplate() {
        return new RestTemplate();
    }
}
