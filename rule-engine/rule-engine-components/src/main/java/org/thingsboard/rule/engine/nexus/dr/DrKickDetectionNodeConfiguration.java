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
 * Configuration for DR Kick Detection Rule Node.
 * Monitors multiple drilling parameters to detect potential kick events.
 */
@Data
public class DrKickDetectionNodeConfiguration implements NodeConfiguration<DrKickDetectionNodeConfiguration> {

    // Input field names - Primary indicators
    private String pitVolumeField;
    private String flowInGpmField;
    private String flowOutGpmField;
    private String standpipePressureField;
    private String mudWeightOutField;

    // Optional secondary indicators
    private String mudWeightInField;
    private String gasUnitsField;
    private String pumpStrokesField;
    private String hookLoadField;

    // Baseline values for comparison (optional - can be from attributes)
    private boolean useAttributeBaselines;
    private String baselinePitVolumeField;
    private String baselineFlowDifferentialField;

    // Threshold configuration
    private double pitGainThresholdBbl;
    private double flowDifferentialThresholdGpm;
    private double pressureDropThresholdPsi;
    private double mudWeightDropThresholdPpg;
    private double gasIncreaseThresholdPercent;

    // Output configuration
    private String outputKickIndicatorField;
    private String outputKickSeverityField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;

    // Alarm generation
    private boolean generateAlarm;
    private String alarmType;

    @Override
    public DrKickDetectionNodeConfiguration defaultConfiguration() {
        DrKickDetectionNodeConfiguration config = new DrKickDetectionNodeConfiguration();
        config.setPitVolumeField("pitVolume");
        config.setFlowInGpmField("flowIn");
        config.setFlowOutGpmField("flowOut");
        config.setStandpipePressureField("spp");
        config.setMudWeightOutField("mudWeightOut");
        config.setMudWeightInField("mudWeightIn");
        config.setGasUnitsField("totalGas");
        config.setPumpStrokesField("pumpStrokes");
        config.setHookLoadField("hookLoad");

        config.setUseAttributeBaselines(true);
        config.setBaselinePitVolumeField("baselinePitVolume");
        config.setBaselineFlowDifferentialField("baselineFlowDiff");

        config.setPitGainThresholdBbl(5.0);
        config.setFlowDifferentialThresholdGpm(50.0);
        config.setPressureDropThresholdPsi(100.0);
        config.setMudWeightDropThresholdPpg(0.3);
        config.setGasIncreaseThresholdPercent(50.0);

        config.setOutputKickIndicatorField("kickIndicator");
        config.setOutputKickSeverityField("kickSeverity");
        config.setAddToMetadata(true);
        config.setSaveAsAttribute(false);
        config.setSaveAsTelemetry(true);
        config.setAttributeScope("SERVER_SCOPE");

        config.setGenerateAlarm(true);
        config.setAlarmType("KICK_DETECTED");
        return config;
    }
}
