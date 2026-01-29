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
 * Configuration for RV Decline Analysis Calculation using Arps equations.
 */
@Data
public class RvDeclineAnalysisNodeConfiguration implements NodeConfiguration<RvDeclineAnalysisNodeConfiguration> {

    // Decline type selection
    private DeclineType declineType;

    // Input field names
    private String initialRateField;      // qi - initial rate (bpd)
    private String declineRateField;      // Di - decline rate (1/time)
    private String bExponentField;        // b - Arps exponent (for hyperbolic)
    private String timeField;             // t - time

    // Default b exponent if not in message
    private Double defaultBExponent;

    // Calculation type
    private CalculationType calculationType;

    // Output configuration
    private String outputField;
    private boolean addToMetadata;
    private boolean saveAsAttribute;
    private boolean saveAsTelemetry;
    private String attributeScope;
    private int resultPrecision;

    public enum DeclineType {
        EXPONENTIAL,  // b = 0: q(t) = qi * e^(-Di*t)
        HYPERBOLIC,   // 0 < b < 1: q(t) = qi / (1 + b*Di*t)^(1/b)
        HARMONIC      // b = 1: q(t) = qi / (1 + Di*t)
    }

    public enum CalculationType {
        RATE_AT_TIME,        // Calculate q(t)
        CUMULATIVE_AT_TIME,  // Calculate Np(t)
        TIME_TO_RATE,        // Calculate t for given q
        EUR                  // Calculate Estimated Ultimate Recovery
    }

    @Override
    public RvDeclineAnalysisNodeConfiguration defaultConfiguration() {
        RvDeclineAnalysisNodeConfiguration config = new RvDeclineAnalysisNodeConfiguration();
        config.setDeclineType(DeclineType.HYPERBOLIC);
        config.setInitialRateField("initialRate");
        config.setDeclineRateField("declineRate");
        config.setBExponentField("bExponent");
        config.setTimeField("time");
        config.setDefaultBExponent(0.5);
        config.setCalculationType(CalculationType.RATE_AT_TIME);
        config.setOutputField("forecastRate");
        config.setAddToMetadata(false);
        config.setSaveAsAttribute(false);
        config.setSaveAsTelemetry(true);
        config.setAttributeScope("SERVER_SCOPE");
        config.setResultPrecision(2);
        return config;
    }
}
