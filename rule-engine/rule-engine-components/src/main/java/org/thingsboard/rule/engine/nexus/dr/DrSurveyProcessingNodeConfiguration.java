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
 * Configuration for DR Survey Processing Rule Node.
 * Processes directional survey data using Minimum Curvature Method.
 */
@Data
public class DrSurveyProcessingNodeConfiguration implements NodeConfiguration<DrSurveyProcessingNodeConfiguration> {

    // Input field names for current survey
    private String mdFtField;
    private String inclinationDegField;
    private String azimuthDegField;

    // Previous survey fields (from attributes or message)
    private boolean usePreviousSurveyFromAttributes;
    private String prevMdFtField;
    private String prevInclinationDegField;
    private String prevAzimuthDegField;
    private String prevTvdFtField;
    private String prevNorthingFtField;
    private String prevEastingFtField;

    // Output field names
    private String outputTvdFtField;
    private String outputNorthingFtField;
    private String outputEastingFtField;
    private String outputDlsDegPer100ftField;
    private String outputVerticalSectionFtField;
    private String outputClosureDistanceFtField;
    private String outputClosureDirectionDegField;

    // Vertical section calculation
    private double verticalSectionAzimuthDeg;

    // Output configuration
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;
    private int resultPrecision;

    // DLS threshold for alerts
    private boolean enableDlsAlert;
    private double dlsThresholdDegPer100ft;

    @Override
    public DrSurveyProcessingNodeConfiguration defaultConfiguration() {
        DrSurveyProcessingNodeConfiguration config = new DrSurveyProcessingNodeConfiguration();
        config.setMdFtField("mdFt");
        config.setInclinationDegField("inclination");
        config.setAzimuthDegField("azimuth");

        config.setUsePreviousSurveyFromAttributes(true);
        config.setPrevMdFtField("lastSurveyMdFt");
        config.setPrevInclinationDegField("lastSurveyInclination");
        config.setPrevAzimuthDegField("lastSurveyAzimuth");
        config.setPrevTvdFtField("lastSurveyTvdFt");
        config.setPrevNorthingFtField("lastSurveyNorthingFt");
        config.setPrevEastingFtField("lastSurveyEastingFt");

        config.setOutputTvdFtField("tvdFt");
        config.setOutputNorthingFtField("northingFt");
        config.setOutputEastingFtField("eastingFt");
        config.setOutputDlsDegPer100ftField("dlsDegPer100ft");
        config.setOutputVerticalSectionFtField("verticalSectionFt");
        config.setOutputClosureDistanceFtField("closureDistanceFt");
        config.setOutputClosureDirectionDegField("closureDirectionDeg");

        config.setVerticalSectionAzimuthDeg(0.0);

        config.setAddToMetadata(true);
        config.setSaveAsAttribute(true);
        config.setSaveAsTelemetry(true);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(2);

        config.setEnableDlsAlert(true);
        config.setDlsThresholdDegPer100ft(6.0);
        return config;
    }
}
