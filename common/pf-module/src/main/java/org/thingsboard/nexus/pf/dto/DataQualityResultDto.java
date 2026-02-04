/*
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
package org.thingsboard.nexus.pf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Result of data quality validation for a telemetry message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityResultDto {

    /**
     * Entity ID that was validated
     */
    private UUID entityId;

    /**
     * Overall quality score (0.0 = invalid, 1.0 = perfect)
     */
    private double overallScore;

    /**
     * Quality classification based on score
     */
    private QualityLevel qualityLevel;

    /**
     * Whether the data should be accepted
     */
    private boolean accepted;

    /**
     * Detailed validation results per variable
     */
    @Builder.Default
    private List<VariableQualityResult> variableResults = new ArrayList<>();

    /**
     * List of validation issues found
     */
    @Builder.Default
    private List<ValidationIssue> issues = new ArrayList<>();

    /**
     * Timestamp validation result
     */
    private TimestampValidation timestampValidation;

    /**
     * Completeness score (ratio of expected vs received variables)
     */
    private double completenessScore;

    /**
     * Quality level classification
     */
    public enum QualityLevel {
        EXCELLENT(0.95, 1.0, "Excellent data quality"),
        GOOD(0.85, 0.95, "Good data quality"),
        ACCEPTABLE(0.70, 0.85, "Acceptable data quality"),
        SUSPICIOUS(0.50, 0.70, "Suspicious data quality - review recommended"),
        POOR(0.0, 0.50, "Poor data quality - likely invalid");

        private final double minScore;
        private final double maxScore;
        private final String description;

        QualityLevel(double minScore, double maxScore, String description) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.description = description;
        }

        public static QualityLevel fromScore(double score) {
            for (QualityLevel level : values()) {
                if (score >= level.minScore && score < level.maxScore) {
                    return level;
                }
            }
            return score >= 1.0 ? EXCELLENT : POOR;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Validation result for a single variable
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableQualityResult {
        private String variableKey;
        private Object value;
        private double qualityScore;
        private boolean rangeValid;
        private boolean rateOfChangeValid;
        private boolean outlierValid;
        private String issueDescription;
    }

    /**
     * Validation issue details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationIssue {
        private IssueType type;
        private IssueSeverity severity;
        private String variableKey;
        private String message;
        private Object actualValue;
        private Object expectedValue;
        private double scorePenalty;

        public enum IssueType {
            OUT_OF_PHYSICAL_RANGE,
            OUT_OF_EXPECTED_RANGE,
            EXCESSIVE_RATE_OF_CHANGE,
            STATISTICAL_OUTLIER,
            MISSING_REQUIRED_VARIABLE,
            INVALID_VALUE,
            STALE_TIMESTAMP,
            FUTURE_TIMESTAMP,
            MISSING_TIMESTAMP,
            NAN_VALUE,
            INFINITE_VALUE
        }

        public enum IssueSeverity {
            CRITICAL,   // Data should be rejected
            HIGH,       // Significant quality concern
            MEDIUM,     // Notable issue but data usable
            LOW         // Minor issue
        }
    }

    /**
     * Timestamp validation details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimestampValidation {
        private Long timestamp;
        private boolean valid;
        private long ageMillis;
        private String issue;
    }

    /**
     * Helper to add an issue
     */
    public void addIssue(ValidationIssue issue) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        issues.add(issue);
    }

    /**
     * Helper to add variable result
     */
    public void addVariableResult(VariableQualityResult result) {
        if (variableResults == null) {
            variableResults = new ArrayList<>();
        }
        variableResults.add(result);
    }
}
