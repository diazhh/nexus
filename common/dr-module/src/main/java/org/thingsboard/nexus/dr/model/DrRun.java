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
import org.thingsboard.nexus.dr.model.enums.HoleSection;
import org.thingsboard.nexus.dr.model.enums.RunStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing a Drilling Run (bit run).
 * A run represents a continuous drilling operation from start depth to end depth.
 */
@Entity
@Table(name = "dr_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrRun {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "run_number", nullable = false, length = 50)
    private String runNumber;

    // --- References ---

    /**
     * Reference to the drilling rig performing this run
     */
    @Column(name = "rig_id", nullable = false)
    private UUID rigId;

    /**
     * Reference to the well being drilled (from rv-module)
     */
    @Column(name = "well_id", nullable = false)
    private UUID wellId;

    /**
     * Reference to the BHA used in this run
     */
    @Column(name = "bha_id")
    private UUID bhaId;

    // --- MWD/LWD Tool Asset References (dynamic relations) ---

    @Column(name = "mwd_tool_asset_id")
    private UUID mwdToolAssetId;

    @Column(name = "lwd_tool_asset_id")
    private UUID lwdToolAssetId;

    // --- Hole Section Configuration ---

    @Enumerated(EnumType.STRING)
    @Column(name = "hole_section", length = 50)
    private HoleSection holeSection;

    @Column(name = "hole_size_in", precision = 5, scale = 3)
    private BigDecimal holeSizeIn;

    @Column(name = "casing_size_in", precision = 5, scale = 3)
    private BigDecimal casingSizeIn;

    @Column(name = "previous_casing_shoe_md_ft", precision = 10, scale = 2)
    private BigDecimal previousCasingShoeMdFt;

    // --- Planned Depths ---

    @Column(name = "planned_start_depth_md_ft", precision = 10, scale = 2)
    private BigDecimal plannedStartDepthMdFt;

    @Column(name = "planned_end_depth_md_ft", precision = 10, scale = 2)
    private BigDecimal plannedEndDepthMdFt;

    @Column(name = "planned_start_depth_tvd_ft", precision = 10, scale = 2)
    private BigDecimal plannedStartDepthTvdFt;

    @Column(name = "planned_end_depth_tvd_ft", precision = 10, scale = 2)
    private BigDecimal plannedEndDepthTvdFt;

    // --- Actual Depths ---

    @Column(name = "start_depth_md_ft", precision = 10, scale = 2)
    private BigDecimal startDepthMdFt;

    @Column(name = "end_depth_md_ft", precision = 10, scale = 2)
    private BigDecimal endDepthMdFt;

    @Column(name = "current_depth_md_ft", precision = 10, scale = 2)
    private BigDecimal currentDepthMdFt;

    @Column(name = "start_depth_tvd_ft", precision = 10, scale = 2)
    private BigDecimal startDepthTvdFt;

    @Column(name = "end_depth_tvd_ft", precision = 10, scale = 2)
    private BigDecimal endDepthTvdFt;

    @Column(name = "current_depth_tvd_ft", precision = 10, scale = 2)
    private BigDecimal currentDepthTvdFt;

    // --- Mud Properties ---

    @Column(name = "mud_type", length = 50)
    private String mudType;

    @Column(name = "mud_weight_ppg", precision = 5, scale = 2)
    private BigDecimal mudWeightPpg;

    @Column(name = "pore_pressure_ppg", precision = 5, scale = 2)
    private BigDecimal porePressurePpg;

    @Column(name = "frac_gradient_ppg", precision = 5, scale = 2)
    private BigDecimal fracGradientPpg;

    // --- Dates ---

    @Column(name = "spud_date")
    private Long spudDate;

    @Column(name = "start_date")
    private Long startDate;

    @Column(name = "end_date")
    private Long endDate;

    // --- Status ---

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RunStatus status;

    // --- KPIs (calculated/aggregated) ---

    @Column(name = "total_footage_ft", precision = 10, scale = 2)
    private BigDecimal totalFootageFt;

    @Column(name = "avg_rop_ft_hr", precision = 8, scale = 2)
    private BigDecimal avgRopFtHr;

    @Column(name = "max_rop_ft_hr", precision = 8, scale = 2)
    private BigDecimal maxRopFtHr;

    @Column(name = "total_rotating_hours", precision = 8, scale = 2)
    private BigDecimal totalRotatingHours;

    @Column(name = "total_sliding_hours", precision = 8, scale = 2)
    private BigDecimal totalSlidingHours;

    @Column(name = "total_circulating_hours", precision = 8, scale = 2)
    private BigDecimal totalCirculatingHours;

    @Column(name = "total_connection_time_hours", precision = 8, scale = 2)
    private BigDecimal totalConnectionTimeHours;

    @Column(name = "total_trip_time_hours", precision = 8, scale = 2)
    private BigDecimal totalTripTimeHours;

    @Column(name = "total_npt_hours", precision = 8, scale = 2)
    private BigDecimal totalNptHours;

    @Column(name = "drilling_efficiency_percent", precision = 5, scale = 2)
    private BigDecimal drillingEfficiencyPercent;

    // --- Connection Statistics ---

    @Column(name = "total_connections")
    private Integer totalConnections;

    @Column(name = "avg_connection_time_min", precision = 6, scale = 2)
    private BigDecimal avgConnectionTimeMin;

    // --- Survey Statistics ---

    @Column(name = "survey_count")
    private Integer surveyCount;

    @Column(name = "max_inclination_deg", precision = 6, scale = 3)
    private BigDecimal maxInclinationDeg;

    @Column(name = "max_dls_deg_per_100ft", precision = 6, scale = 3)
    private BigDecimal maxDlsDegPer100ft;

    // --- Vibration Statistics ---

    @Column(name = "avg_axial_vibration_g", precision = 6, scale = 3)
    private BigDecimal avgAxialVibrationG;

    @Column(name = "avg_lateral_vibration_g", precision = 6, scale = 3)
    private BigDecimal avgLateralVibrationG;

    @Column(name = "max_shock_g", precision = 6, scale = 3)
    private BigDecimal maxShockG;

    // --- End of Run Summary ---

    @Column(name = "reason_ended", length = 255)
    private String reasonEnded;

    @Column(name = "bit_condition_out", length = 100)
    private String bitConditionOut;

    // --- Metadata ---

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_time")
    private Long updatedTime;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdTime == null) {
            createdTime = System.currentTimeMillis();
        }
        if (status == null) {
            status = RunStatus.PLANNED;
        }
        if (totalFootageFt == null) {
            totalFootageFt = BigDecimal.ZERO;
        }
        if (totalRotatingHours == null) {
            totalRotatingHours = BigDecimal.ZERO;
        }
        if (totalSlidingHours == null) {
            totalSlidingHours = BigDecimal.ZERO;
        }
        if (totalCirculatingHours == null) {
            totalCirculatingHours = BigDecimal.ZERO;
        }
        if (totalConnectionTimeHours == null) {
            totalConnectionTimeHours = BigDecimal.ZERO;
        }
        if (totalTripTimeHours == null) {
            totalTripTimeHours = BigDecimal.ZERO;
        }
        if (totalNptHours == null) {
            totalNptHours = BigDecimal.ZERO;
        }
        if (totalConnections == null) {
            totalConnections = 0;
        }
        if (surveyCount == null) {
            surveyCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
