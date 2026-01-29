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
package org.thingsboard.rule.engine.nexus.rv;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * Configuration for RV Water Saturation (Sw) Calculation using Archie equation.
 */
@Data
public class RvSwCalculationNodeConfiguration implements NodeConfiguration<RvSwCalculationNodeConfiguration> {

    // Input field names
    private String porosityField;
    private String rwField;          // Formation water resistivity
    private String rtField;          // True formation resistivity

    // Archie parameters (with defaults)
    private Double tortuosityFactor;     // 'a' - typically 1.0
    private Double cementationExponent;  // 'm' - typically 2.0
    private Double saturationExponent;   // 'n' - typically 2.0

    // Optional: read parameters from message
    private String tortuosityFactorField;
    private String cementationExponentField;
    private String saturationExponentField;

    // Output configuration
    private String outputField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;
    private int resultPrecision;

    @Override
    public RvSwCalculationNodeConfiguration defaultConfiguration() {
        RvSwCalculationNodeConfiguration config = new RvSwCalculationNodeConfiguration();
        config.setPorosityField("porosity");
        config.setRwField("rw");
        config.setRtField("rt");
        config.setTortuosityFactor(1.0);
        config.setCementationExponent(2.0);
        config.setSaturationExponent(2.0);
        config.setTortuosityFactorField(null);
        config.setCementationExponentField(null);
        config.setSaturationExponentField(null);
        config.setOutputField("sw");
        config.setAddToMetadata(false);
        config.setSaveAsAttribute(true);
        config.setSaveAsTelemetry(false);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(4);
        return config;
    }
}
