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
 * Configuration for RV Shale Volume (Vsh) Calculation using Larionov equation.
 */
@Data
public class RvVshCalculationNodeConfiguration implements NodeConfiguration<RvVshCalculationNodeConfiguration> {

    // Input field names
    private String grLogField;
    private String grCleanField;
    private String grShaleField;

    // Method selection
    private VshMethod method;

    // Output configuration
    private String outputField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;
    private int resultPrecision;

    public enum VshMethod {
        LINEAR,           // Vsh = IGR (simple linear)
        LARIONOV_TERTIARY, // Vsh = 0.083 * (2^(3.7*IGR) - 1) for Tertiary rocks
        LARIONOV_OLDER,    // Vsh = 0.33 * (2^(2*IGR) - 1) for older rocks
        STEIBER,          // Vsh = IGR / (3 - 2*IGR)
        CLAVIER           // Vsh = 1.7 - sqrt(3.38 - (IGR + 0.7)^2)
    }

    @Override
    public RvVshCalculationNodeConfiguration defaultConfiguration() {
        RvVshCalculationNodeConfiguration config = new RvVshCalculationNodeConfiguration();
        config.setGrLogField("grLog");
        config.setGrCleanField("grClean");
        config.setGrShaleField("grShale");
        config.setMethod(VshMethod.LARIONOV_TERTIARY);
        config.setOutputField("vsh");
        config.setAddToMetadata(false);
        config.setSaveAsAttribute(true);
        config.setSaveAsTelemetry(false);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(4);
        return config;
    }
}
