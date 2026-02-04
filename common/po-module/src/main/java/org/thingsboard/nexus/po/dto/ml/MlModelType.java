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
package org.thingsboard.nexus.po.dto.ml;

/**
 * Types of ML models supported by the system.
 */
public enum MlModelType {
    /**
     * LSTM-based failure prediction model.
     * Predicts probability of equipment failure within a time horizon.
     */
    FAILURE_PREDICTION,

    /**
     * Isolation Forest-based anomaly detection.
     * Identifies unusual patterns in telemetry data.
     */
    ANOMALY_DETECTION,

    /**
     * Composite health score calculation.
     * Combines multiple factors into a 0-100 health index.
     */
    HEALTH_SCORE
}
