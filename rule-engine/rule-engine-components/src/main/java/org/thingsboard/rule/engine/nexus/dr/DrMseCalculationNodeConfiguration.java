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
package org.thingsboard.rule.engine.nexus.dr;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * Configuration for DR MSE (Mechanical Specific Energy) Calculation Rule Node.
 */
@Data
public class DrMseCalculationNodeConfiguration implements NodeConfiguration<DrMseCalculationNodeConfiguration> {

    // Input field names
    private String wobKlbsField;
    private String rpmField;
    private String torqueFtLbsField;
    private String ropFtHrField;
    private String bitDiameterInField;

    // Output configuration
    private String outputField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;

    // Precision
    private int resultPrecision;

    // Threshold for MSE alerts
    private boolean enableMseAlert;
    private double mseThresholdPsi;

    @Override
    public DrMseCalculationNodeConfiguration defaultConfiguration() {
        DrMseCalculationNodeConfiguration config = new DrMseCalculationNodeConfiguration();
        config.setWobKlbsField("wob");
        config.setRpmField("rpm");
        config.setTorqueFtLbsField("torque");
        config.setRopFtHrField("rop");
        config.setBitDiameterInField("bitDiameter");
        config.setOutputField("mse");
        config.setAddToMetadata(false);
        config.setSaveAsAttribute(false);
        config.setSaveAsTelemetry(true);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(2);
        config.setEnableMseAlert(true);
        config.setMseThresholdPsi(100000.0);
        return config;
    }
}
