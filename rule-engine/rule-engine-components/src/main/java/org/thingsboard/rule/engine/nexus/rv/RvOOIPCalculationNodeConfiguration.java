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
 * Configuration for RV OOIP Calculation Rule Node.
 * Allows specifying input sources (message body, attributes, telemetry) for OOIP parameters.
 */
@Data
public class RvOOIPCalculationNodeConfiguration implements NodeConfiguration<RvOOIPCalculationNodeConfiguration> {

    // Input field names (from message body or attributes)
    private String areaAcresField;
    private String thicknessMField;
    private String porosityField;
    private String waterSaturationField;
    private String boField;

    // Output configuration
    private String outputField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;

    // Precision
    private int resultPrecision;

    @Override
    public RvOOIPCalculationNodeConfiguration defaultConfiguration() {
        RvOOIPCalculationNodeConfiguration config = new RvOOIPCalculationNodeConfiguration();
        config.setAreaAcresField("areaAcres");
        config.setThicknessMField("thicknessM");
        config.setPorosityField("porosity");
        config.setWaterSaturationField("waterSaturation");
        config.setBoField("bo");
        config.setOutputField("ooip");
        config.setAddToMetadata(false);
        config.setSaveAsAttribute(true);
        config.setSaveAsTelemetry(false);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(0);
        return config;
    }
}
