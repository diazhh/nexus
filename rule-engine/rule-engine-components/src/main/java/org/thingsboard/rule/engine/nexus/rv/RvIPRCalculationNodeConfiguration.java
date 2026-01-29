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
 * Configuration for RV IPR (Inflow Performance Relationship) Calculation.
 */
@Data
public class RvIPRCalculationNodeConfiguration implements NodeConfiguration<RvIPRCalculationNodeConfiguration> {

    // Method selection
    private IprMethod method;

    // Input field names for all methods
    private String reservoirPressureField;
    private String flowingPressureField;

    // For Vogel method
    private String qmaxField;

    // For Darcy/Linear method
    private String productivityIndexField;

    // For Fetkovich method
    private String cField;  // C coefficient
    private String nField;  // n exponent (typically 0.5 to 1.0)

    // For Jones method (quadratic)
    private String aCoeffField;  // linear term
    private String bCoeffField;  // quadratic term

    // Output configuration
    private String outputField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;
    private int resultPrecision;

    public enum IprMethod {
        VOGEL,      // q = qmax * (1 - 0.2*(Pwf/Pr) - 0.8*(Pwf/Pr)^2)
        DARCY,      // q = J * (Pr - Pwf) for undersaturated
        FETKOVICH,  // q = C * (Pr^2 - Pwf^2)^n
        JONES       // q = (-a + sqrt(a^2 + 4*b*(Pr-Pwf))) / (2*b) for turbulence
    }

    @Override
    public RvIPRCalculationNodeConfiguration defaultConfiguration() {
        RvIPRCalculationNodeConfiguration config = new RvIPRCalculationNodeConfiguration();
        config.setMethod(IprMethod.VOGEL);
        config.setReservoirPressureField("reservoirPressure");
        config.setFlowingPressureField("flowingPressure");
        config.setQmaxField("qmax");
        config.setProductivityIndexField("productivityIndex");
        config.setCField("cCoeff");
        config.setNField("nExponent");
        config.setACoeffField("aCoeff");
        config.setBCoeffField("bCoeff");
        config.setOutputField("oilRate");
        config.setAddToMetadata(false);
        config.setSaveAsAttribute(false);
        config.setSaveAsTelemetry(true);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(2);
        return config;
    }
}
