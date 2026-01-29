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
package org.thingsboard.nexus.rv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración del Módulo de Yacimientos (Reservoir Module)
 * Permite configurar umbrales, cálculos y comportamiento del módulo.
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "rv.module")
@Data
public class RvModuleConfiguration {

    /**
     * URL del backend para llamadas REST desde nodos de Rule Engine
     */
    private String backendUrl = "http://localhost:8080";

    /**
     * Habilitar cálculos automáticos de OOIP
     */
    private boolean ooipCalculationEnabled = true;

    /**
     * Habilitar cálculos de IPR (Inflow Performance Relationship)
     */
    private boolean iprCalculationEnabled = true;

    /**
     * Habilitar análisis de declinación
     */
    private boolean declineAnalysisEnabled = true;

    /**
     * Habilitar cálculos de Balance de Materiales
     */
    private boolean materialBalanceEnabled = true;

    /**
     * Umbral de presión para alarma de yacimiento (psi)
     */
    private double reservoirPressureAlarmThreshold = 500.0;

    /**
     * Umbral de saturación de agua para alarma (fracción)
     */
    private double waterSaturationAlarmThreshold = 0.80;

    /**
     * Exponente de cementación por defecto (Archie m)
     */
    private double defaultCementationExponent = 2.0;

    /**
     * Exponente de saturación por defecto (Archie n)
     */
    private double defaultSaturationExponent = 2.0;

    /**
     * Factor de tortuosidad por defecto (Archie a)
     */
    private double defaultTortuosityFactor = 1.0;

    /**
     * Timeout para llamadas REST (ms)
     */
    private int restTimeout = 5000;

    /**
     * Habilitar modelo de Foamy Oil (Venezuela - Faja del Orinoco)
     */
    private boolean foamyOilModelEnabled = true;

    /**
     * Habilitar cálculos de diluyente (Nafta)
     */
    private boolean diluentCalculationEnabled = true;

    @Bean
    public RestTemplate rvRestTemplate() {
        return new RestTemplate();
    }
}
