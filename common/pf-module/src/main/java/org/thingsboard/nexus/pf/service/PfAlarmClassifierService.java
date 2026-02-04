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
package org.thingsboard.nexus.pf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.pf.dto.AlarmSeverity;
import org.thingsboard.nexus.pf.dto.LiftSystemType;
import org.thingsboard.nexus.pf.dto.PfAlarmDto;

import java.util.*;

/**
 * Service for classifying alarms and generating recommended actions.
 * Uses domain knowledge about oil & gas production equipment to provide
 * contextual recommendations for operators.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfAlarmClassifierService {

    /**
     * Classifies an alarm and adds tags, priority, and recommended actions.
     */
    public ClassificationResult classify(PfAlarmDto alarm) {
        ClassificationResult result = new ClassificationResult();

        // Extract alarm context
        String alarmType = alarm.getAlarmType();
        AlarmSeverity severity = alarm.getSeverity();
        String entityType = alarm.getEntityType();

        // Add tags based on alarm type and entity
        result.setTags(generateTags(alarmType, entityType));

        // Calculate priority score
        result.setPriorityScore(calculatePriority(alarm));

        // Generate recommended actions
        result.setRecommendedActions(generateRecommendations(alarmType, entityType, severity));

        // Add root cause hints
        result.setPossibleCauses(identifyPossibleCauses(alarmType, entityType));

        // Add related alarms that should be checked
        result.setRelatedAlarmTypes(identifyRelatedAlarms(alarmType, entityType));

        log.debug("Classified alarm {}: priority={}, tags={}, recommendations={}",
                alarm.getId(), result.getPriorityScore(), result.getTags(),
                result.getRecommendedActions().size());

        return result;
    }

    /**
     * Generates tags for categorization and filtering.
     */
    private Set<String> generateTags(String alarmType, String entityType) {
        Set<String> tags = new HashSet<>();

        // Entity type tags
        if (entityType != null) {
            tags.add(entityType);

            if (entityType.contains("esp")) {
                tags.add("ARTIFICIAL_LIFT");
                tags.add("ESP");
            } else if (entityType.contains("pcp")) {
                tags.add("ARTIFICIAL_LIFT");
                tags.add("PCP");
            } else if (entityType.contains("gas_lift")) {
                tags.add("ARTIFICIAL_LIFT");
                tags.add("GAS_LIFT");
            } else if (entityType.contains("rod_pump")) {
                tags.add("ARTIFICIAL_LIFT");
                tags.add("ROD_PUMP");
            } else if (entityType.contains("well")) {
                tags.add("WELL");
            }
        }

        // Alarm type tags
        if (alarmType != null) {
            alarmType = alarmType.toUpperCase();

            if (alarmType.contains("TEMPERATURE")) {
                tags.add("THERMAL");
            }
            if (alarmType.contains("VIBRATION")) {
                tags.add("MECHANICAL");
            }
            if (alarmType.contains("CURRENT") || alarmType.contains("VOLTAGE")) {
                tags.add("ELECTRICAL");
            }
            if (alarmType.contains("PRESSURE")) {
                tags.add("HYDRAULIC");
            }
            if (alarmType.contains("COMMUNICATION") || alarmType.contains("DATA_QUALITY")) {
                tags.add("INSTRUMENTATION");
            }
            if (alarmType.contains("FREQUENCY") || alarmType.contains("RPM")) {
                tags.add("OPERATIONAL");
            }
        }

        return tags;
    }

    /**
     * Calculates priority score (0-100).
     */
    private int calculatePriority(PfAlarmDto alarm) {
        int basePriority = switch (alarm.getSeverity()) {
            case CRITICAL -> 80;
            case HIGH -> 60;
            case MEDIUM -> 40;
            case LOW -> 20;
            case INFO -> 10;
        };

        // Adjust based on alarm type criticality
        String alarmType = alarm.getAlarmType().toUpperCase();

        // Equipment protection alarms get higher priority
        if (alarmType.contains("TEMPERATURE_MOTOR") ||
                alarmType.contains("VIBRATION") ||
                alarmType.contains("OVERLOAD")) {
            basePriority += 10;
        }

        // Safety-related alarms
        if (alarmType.contains("GAS_DETECTION") ||
                alarmType.contains("LEAK") ||
                alarmType.contains("PRESSURE_HIGH_HIGH")) {
            basePriority += 15;
        }

        // Production-impacting alarms
        if (alarmType.contains("PUMP_OFF") ||
                alarmType.contains("SHUTDOWN") ||
                alarmType.contains("TRIP")) {
            basePriority += 10;
        }

        return Math.min(100, basePriority);
    }

    /**
     * Generates recommended actions based on alarm type.
     */
    private List<String> generateRecommendations(String alarmType, String entityType,
                                                  AlarmSeverity severity) {
        List<String> recommendations = new ArrayList<>();
        String type = alarmType.toUpperCase();

        // ESP-specific recommendations
        if (entityType != null && entityType.contains("esp")) {
            if (type.contains("TEMPERATURE_MOTOR")) {
                recommendations.add("Reduce operating frequency by 5-10 Hz");
                recommendations.add("Check cooling fluid flow rate");
                recommendations.add("Verify motor is not in sand face-up position");
                if (severity == AlarmSeverity.CRITICAL) {
                    recommendations.add("Consider immediate shutdown to prevent motor burnout");
                    recommendations.add("Schedule pulling job if condition persists");
                }
            }

            if (type.contains("CURRENT_HIGH")) {
                recommendations.add("Check for sand production or scale buildup");
                recommendations.add("Verify pump is not gas-locked");
                recommendations.add("Review well inflow vs pump capacity");
            }

            if (type.contains("CURRENT_LOW")) {
                recommendations.add("Check for gas interference");
                recommendations.add("Verify pump intake is not plugged");
                recommendations.add("Review fluid level - possible pump-off condition");
            }

            if (type.contains("VIBRATION")) {
                recommendations.add("Check for worn pump stages");
                recommendations.add("Verify tubing/casing integrity");
                recommendations.add("Review for sand ingress");
                if (severity == AlarmSeverity.CRITICAL) {
                    recommendations.add("Reduce frequency to minimize mechanical stress");
                }
            }

            if (type.contains("PIP") || type.contains("INTAKE_PRESSURE")) {
                recommendations.add("Review submergence calculations");
                recommendations.add("Check for fluid influx changes");
                recommendations.add("Verify pump setting depth is appropriate");
            }
        }

        // PCP-specific recommendations
        if (entityType != null && entityType.contains("pcp")) {
            if (type.contains("TORQUE_HIGH")) {
                recommendations.add("Check for stator swelling or damage");
                recommendations.add("Verify fluid viscosity hasn't changed significantly");
                recommendations.add("Review for sand production");
            }

            if (type.contains("RPM") || type.contains("SPEED")) {
                recommendations.add("Check drive system (VFD/gearbox)");
                recommendations.add("Verify rod string integrity");
            }

            if (type.contains("TEMPERATURE_STATOR")) {
                recommendations.add("Reduce RPM to allow cooling");
                recommendations.add("Check for dry running condition");
                recommendations.add("Verify proper fluid lubrication");
            }
        }

        // Gas Lift recommendations
        if (entityType != null && entityType.contains("gas_lift")) {
            if (type.contains("INJECTION_PRESSURE")) {
                recommendations.add("Check valve operability");
                recommendations.add("Review gas supply pressure");
                recommendations.add("Verify orifice sizing");
            }

            if (type.contains("INJECTION_RATE")) {
                recommendations.add("Check flowmeter calibration");
                recommendations.add("Review compressor output");
                recommendations.add("Verify allocation calculations");
            }
        }

        // Rod Pump recommendations
        if (entityType != null && entityType.contains("rod_pump")) {
            if (type.contains("LOAD_HIGH")) {
                recommendations.add("Check for fluid pound");
                recommendations.add("Verify counterbalance settings");
                recommendations.add("Review rod string design");
            }

            if (type.contains("FILLAGE")) {
                recommendations.add("Review inflow performance");
                recommendations.add("Consider adjusting SPM");
                recommendations.add("Check pump efficiency from dynacard");
            }
        }

        // Generic recommendations
        if (type.contains("COMMUNICATION_LOST")) {
            recommendations.add("Check RTU/PLC connectivity");
            recommendations.add("Verify network infrastructure");
            recommendations.add("Dispatch field technician if issue persists");
        }

        if (type.contains("DATA_QUALITY")) {
            recommendations.add("Check sensor calibration");
            recommendations.add("Verify instrument wiring");
            recommendations.add("Review data transmission settings");
        }

        // Add general recommendations based on severity
        if (severity == AlarmSeverity.CRITICAL) {
            recommendations.add("Notify operations supervisor immediately");
            recommendations.add("Document all actions taken");
        }

        return recommendations;
    }

    /**
     * Identifies possible root causes.
     */
    private List<String> identifyPossibleCauses(String alarmType, String entityType) {
        List<String> causes = new ArrayList<>();
        String type = alarmType.toUpperCase();

        if (type.contains("TEMPERATURE")) {
            causes.add("Insufficient cooling");
            causes.add("Overloading");
            causes.add("Bearing failure");
            causes.add("Electrical insulation breakdown");
        }

        if (type.contains("VIBRATION")) {
            causes.add("Worn bearings or seals");
            causes.add("Pump/motor imbalance");
            causes.add("Loose mounting");
            causes.add("Sand erosion");
        }

        if (type.contains("CURRENT") && type.contains("HIGH")) {
            causes.add("Pump overload");
            causes.add("Mechanical binding");
            causes.add("Sand/scale buildup");
            causes.add("Incorrect sizing");
        }

        if (type.contains("PRESSURE") && type.contains("LOW")) {
            causes.add("Pump wear");
            causes.add("Gas interference");
            causes.add("Tubing leak");
            causes.add("Reservoir depletion");
        }

        return causes;
    }

    /**
     * Identifies related alarm types that should be monitored.
     */
    private List<String> identifyRelatedAlarms(String alarmType, String entityType) {
        List<String> relatedAlarms = new ArrayList<>();
        String type = alarmType.toUpperCase();

        // Temperature often correlates with current and vibration
        if (type.contains("TEMPERATURE")) {
            relatedAlarms.add("CURRENT_HIGH");
            relatedAlarms.add("VIBRATION_HIGH");
            relatedAlarms.add("LOAD_HIGH");
        }

        // Vibration often precedes or accompanies temperature rise
        if (type.contains("VIBRATION")) {
            relatedAlarms.add("TEMPERATURE_MOTOR");
            relatedAlarms.add("CURRENT_HIGH");
        }

        // Low pressure can indicate multiple issues
        if (type.contains("PRESSURE") && type.contains("LOW")) {
            relatedAlarms.add("CURRENT_LOW");
            relatedAlarms.add("PRODUCTION_LOW");
        }

        return relatedAlarms;
    }

    // Result class

    @lombok.Data
    public static class ClassificationResult {
        private Set<String> tags = new HashSet<>();
        private int priorityScore;
        private List<String> recommendedActions = new ArrayList<>();
        private List<String> possibleCauses = new ArrayList<>();
        private List<String> relatedAlarmTypes = new ArrayList<>();
    }
}
