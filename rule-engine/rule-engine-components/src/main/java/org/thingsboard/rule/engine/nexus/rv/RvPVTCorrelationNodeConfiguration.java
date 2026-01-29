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
 * Configuration for RV PVT Correlation Calculations.
 * Supports multiple correlation methods for bubble point, Bo, and viscosity.
 */
@Data
public class RvPVTCorrelationNodeConfiguration implements NodeConfiguration<RvPVTCorrelationNodeConfiguration> {

    // Property to calculate
    private PvtProperty propertyToCalculate;

    // Correlation method
    private PvtCorrelation correlation;

    // Input field names
    private String rsField;           // Solution GOR (scf/stb)
    private String gasGravityField;   // Gas specific gravity (air = 1)
    private String oilGravityField;   // Oil specific gravity
    private String apiGravityField;   // API gravity
    private String temperatureField;  // Temperature (°F)
    private String pressureField;     // Pressure (psia)

    // Output configuration
    private String outputField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;
    private int resultPrecision;

    public enum PvtProperty {
        BUBBLE_POINT,      // Pb - bubble point pressure (psia)
        OIL_FVF,           // Bo - oil formation volume factor (rb/stb)
        GAS_FVF,           // Bg - gas formation volume factor (rcf/scf)
        DEAD_OIL_VISCOSITY,// μod - dead oil viscosity (cp)
        LIVE_OIL_VISCOSITY,// μo - live oil viscosity (cp)
        SOLUTION_GOR,      // Rs - solution gas-oil ratio (scf/stb)
        OIL_COMPRESSIBILITY// co - oil compressibility (1/psi)
    }

    public enum PvtCorrelation {
        // Bubble Point correlations
        STANDING_PB,       // Standing correlation for Pb
        VAZQUEZ_BEGGS_PB,  // Vazquez-Beggs correlation for Pb
        GLASO_PB,          // Glasø correlation for Pb

        // Oil FVF correlations
        STANDING_BO,       // Standing correlation for Bo
        VAZQUEZ_BEGGS_BO,  // Vazquez-Beggs correlation for Bo
        GLASO_BO,          // Glasø correlation for Bo

        // Viscosity correlations
        BEGGS_ROBINSON,    // Beggs-Robinson for dead oil viscosity
        EGBOGAH,           // Egbogah for dead oil viscosity
        CHEW_CONNALLY,     // Chew-Connally for live oil viscosity

        // GOR correlations
        STANDING_RS,       // Standing correlation for Rs
        VAZQUEZ_BEGGS_RS   // Vazquez-Beggs correlation for Rs
    }

    @Override
    public RvPVTCorrelationNodeConfiguration defaultConfiguration() {
        RvPVTCorrelationNodeConfiguration config = new RvPVTCorrelationNodeConfiguration();
        config.setPropertyToCalculate(PvtProperty.BUBBLE_POINT);
        config.setCorrelation(PvtCorrelation.STANDING_PB);
        config.setRsField("rs");
        config.setGasGravityField("gasGravity");
        config.setOilGravityField("oilGravity");
        config.setApiGravityField("apiGravity");
        config.setTemperatureField("temperature");
        config.setPressureField("pressure");
        config.setOutputField("bubblePoint");
        config.setAddToMetadata(false);
        config.setSaveAsAttribute(true);
        config.setSaveAsTelemetry(false);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(2);
        return config;
    }
}
