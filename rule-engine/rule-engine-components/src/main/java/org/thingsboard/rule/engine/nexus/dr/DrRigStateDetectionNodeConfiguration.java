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
 * Configuration for DR Rig State Detection Rule Node.
 * Detects drilling activity state from real-time sensor data.
 */
@Data
public class DrRigStateDetectionNodeConfiguration implements NodeConfiguration<DrRigStateDetectionNodeConfiguration> {

    // Input field names
    private String bitDepthFtField;
    private String holeDepthFtField;
    private String blockPositionFtField;
    private String hookLoadKlbsField;
    private String wobKlbsField;
    private String rpmField;
    private String sppPsiField;
    private String flowRateGpmField;
    private String torqueFtLbsField;
    private String ropFtHrField;

    // Thresholds for state detection
    private double onBottomThresholdFt;
    private double minRpmForRotating;
    private double minFlowForCirculating;
    private double minWobForDrilling;
    private double tripSpeedThresholdFtMin;
    private double slipPositionThresholdFt;
    private double pipeMovementThresholdFt;

    // Output configuration
    private String outputRigStateField;
    private String outputRigStateCodeField;
    private String outputActivityField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;

    // State change tracking
    private boolean trackStateChanges;
    private String stateChangeTimeField;

    @Override
    public DrRigStateDetectionNodeConfiguration defaultConfiguration() {
        DrRigStateDetectionNodeConfiguration config = new DrRigStateDetectionNodeConfiguration();
        config.setBitDepthFtField("bitDepth");
        config.setHoleDepthFtField("holeDepth");
        config.setBlockPositionFtField("blockPosition");
        config.setHookLoadKlbsField("hookLoad");
        config.setWobKlbsField("wob");
        config.setRpmField("rpm");
        config.setSppPsiField("spp");
        config.setFlowRateGpmField("flowRate");
        config.setTorqueFtLbsField("torque");
        config.setRopFtHrField("rop");

        config.setOnBottomThresholdFt(5.0);
        config.setMinRpmForRotating(5.0);
        config.setMinFlowForCirculating(50.0);
        config.setMinWobForDrilling(2.0);
        config.setTripSpeedThresholdFtMin(10.0);
        config.setSlipPositionThresholdFt(3.0);
        config.setPipeMovementThresholdFt(0.5);

        config.setOutputRigStateField("rigState");
        config.setOutputRigStateCodeField("rigStateCode");
        config.setOutputActivityField("drillingActivity");
        config.setAddToMetadata(true);
        config.setSaveAsAttribute(true);
        config.setSaveAsTelemetry(true);
        config.setAttributeScope("SERVER_SCOPE");

        config.setTrackStateChanges(true);
        config.setStateChangeTimeField("stateChangeTime");
        return config;
    }
}
