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
package org.thingsboard.nexus.dr.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.thingsboard.nexus.dr.model.enums.SurveyType;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing a Directional Survey point.
 * Surveys are measurement points along the wellbore that define its trajectory.
 */
@Entity
@Table(name = "dr_directional_surveys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrDirectionalSurvey {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Reference to the drilling run
     */
    @Column(name = "run_id", nullable = false)
    private UUID runId;

    /**
     * Reference to the well (from rv-module)
     */
    @Column(name = "well_id", nullable = false)
    private UUID wellId;

    // --- Measured Data ---

    /**
     * Measured Depth in feet
     */
    @Column(name = "md_ft", nullable = false, precision = 10, scale = 2)
    private BigDecimal mdFt;

    /**
     * Inclination angle in degrees (0 = vertical, 90 = horizontal)
     */
    @Column(name = "inclination_deg", precision = 6, scale = 3)
    private BigDecimal inclinationDeg;

    /**
     * Azimuth (direction) in degrees from North (0-360)
     */
    @Column(name = "azimuth_deg", precision = 6, scale = 3)
    private BigDecimal azimuthDeg;

    /**
     * Toolface orientation in degrees
     */
    @Column(name = "toolface_deg", precision = 6, scale = 3)
    private BigDecimal toolfaceDeg;

    // --- Calculated Values ---

    /**
     * True Vertical Depth in feet (calculated)
     */
    @Column(name = "tvd_ft", precision = 10, scale = 2)
    private BigDecimal tvdFt;

    /**
     * North displacement from surface location in feet
     */
    @Column(name = "north_ft", precision = 12, scale = 2)
    private BigDecimal northFt;

    /**
     * East displacement from surface location in feet
     */
    @Column(name = "east_ft", precision = 12, scale = 2)
    private BigDecimal eastFt;

    /**
     * Vertical Section in feet (projection onto azimuth plane)
     */
    @Column(name = "vertical_section_ft", precision = 12, scale = 2)
    private BigDecimal verticalSectionFt;

    /**
     * Dog Leg Severity in degrees per 100 feet
     */
    @Column(name = "dls_deg_per_100ft", precision = 6, scale = 3)
    private BigDecimal dlsDegPer100ft;

    /**
     * Closure distance (horizontal displacement from surface)
     */
    @Column(name = "closure_distance_ft", precision = 12, scale = 2)
    private BigDecimal closureDistanceFt;

    /**
     * Closure azimuth (direction from surface to current position)
     */
    @Column(name = "closure_azimuth_deg", precision = 6, scale = 3)
    private BigDecimal closureAzimuthDeg;

    // --- Survey Metadata ---

    @Enumerated(EnumType.STRING)
    @Column(name = "survey_type", length = 50)
    private SurveyType surveyType;

    /**
     * Is this a definitive survey (vs preliminary)
     */
    @Column(name = "is_definitive")
    private Boolean isDefinitive;

    /**
     * Survey quality assessment
     */
    @Column(name = "survey_quality", length = 20)
    private String surveyQuality;

    /**
     * Timestamp when survey was taken
     */
    @Column(name = "survey_time", nullable = false)
    private Long surveyTime;

    // --- Magnetic Data (for MWD surveys) ---

    @Column(name = "magnetic_field_strength", precision = 8, scale = 4)
    private BigDecimal magneticFieldStrength;

    @Column(name = "magnetic_dip_angle_deg", precision = 6, scale = 3)
    private BigDecimal magneticDipAngleDeg;

    @Column(name = "gravity_field_strength", precision = 8, scale = 4)
    private BigDecimal gravityFieldStrength;

    // --- Temperature and Corrections ---

    @Column(name = "borehole_temp_f", precision = 6, scale = 2)
    private BigDecimal boreholeTempF;

    @Column(name = "sag_correction_applied")
    private Boolean sagCorrectionApplied;

    @Column(name = "magnetic_correction_applied")
    private Boolean magneticCorrectionApplied;

    // --- Error Model ---

    @Column(name = "north_uncertainty_ft", precision = 8, scale = 2)
    private BigDecimal northUncertaintyFt;

    @Column(name = "east_uncertainty_ft", precision = 8, scale = 2)
    private BigDecimal eastUncertaintyFt;

    @Column(name = "tvd_uncertainty_ft", precision = 8, scale = 2)
    private BigDecimal tvdUncertaintyFt;

    // --- Raw Data ---

    @Type(JsonBinaryType.class)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private JsonNode rawData;

    // --- Metadata ---

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdTime == null) {
            createdTime = System.currentTimeMillis();
        }
        if (surveyTime == null) {
            surveyTime = System.currentTimeMillis();
        }
        if (isDefinitive == null) {
            isDefinitive = false;
        }
        if (surveyQuality == null) {
            surveyQuality = "GOOD";
        }
    }
}
