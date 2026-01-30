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
 * Configuration for DR ECD (Equivalent Circulating Density) Calculation Rule Node.
 */
@Data
public class DrEcdCalculationNodeConfiguration implements NodeConfiguration<DrEcdCalculationNodeConfiguration> {

    // Input field names
    private String mudWeightPpgField;
    private String annularPressureLossPsiField;
    private String tvdFtField;

    // Output configuration
    private String outputField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;

    // Precision
    private int resultPrecision;

    // Threshold for ECD alerts
    private boolean enableEcdHighAlert;
    private double ecdHighThresholdPpg;
    private boolean enableEcdLowAlert;
    private double ecdLowThresholdPpg;

    @Override
    public DrEcdCalculationNodeConfiguration defaultConfiguration() {
        DrEcdCalculationNodeConfiguration config = new DrEcdCalculationNodeConfiguration();
        config.setMudWeightPpgField("mudWeight");
        config.setAnnularPressureLossPsiField("annularPressureLoss");
        config.setTvdFtField("tvd");
        config.setOutputField("ecd");
        config.setAddToMetadata(false);
        config.setSaveAsAttribute(false);
        config.setSaveAsTelemetry(true);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(2);
        config.setEnableEcdHighAlert(true);
        config.setEcdHighThresholdPpg(15.0);
        config.setEnableEcdLowAlert(true);
        config.setEcdLowThresholdPpg(9.0);
        return config;
    }
}
