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
import org.thingsboard.nexus.dr.model.enums.BhaType;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing a Bottom Hole Assembly (BHA).
 * The BHA is represented as an Asset (digital twin) in ThingsBoard.
 * This table stores references to the asset_id and operational data.
 */
@Entity
@Table(name = "dr_bhas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrBha {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "bha_number", nullable = false, length = 50)
    private String bhaNumber;

    /**
     * Reference to the Asset (digital twin) representing this BHA in ThingsBoard
     */
    @Column(name = "asset_id", nullable = false, unique = true)
    private UUID assetId;

    // --- BHA Type and Configuration ---

    @Enumerated(EnumType.STRING)
    @Column(name = "bha_type", length = 50)
    private BhaType bhaType;

    @Column(name = "is_directional")
    private Boolean isDirectional;

    // --- Bit Information ---

    @Column(name = "bit_serial", length = 100)
    private String bitSerial;

    @Column(name = "bit_type", length = 50)
    private String bitType;

    @Column(name = "bit_size_in", precision = 5, scale = 3)
    private BigDecimal bitSizeIn;

    @Column(name = "bit_iadc_code", length = 20)
    private String bitIadcCode;

    @Column(name = "bit_manufacturer", length = 100)
    private String bitManufacturer;

    @Column(name = "bit_model", length = 100)
    private String bitModel;

    @Column(name = "bit_tfa_sq_in", precision = 6, scale = 3)
    private BigDecimal bitTfaSqIn;

    @Column(name = "bit_nozzles", length = 100)
    private String bitNozzles;

    // --- BHA Dimensions ---

    @Column(name = "total_length_ft", precision = 8, scale = 2)
    private BigDecimal totalLengthFt;

    @Column(name = "total_weight_lbs", precision = 10, scale = 2)
    private BigDecimal totalWeightLbs;

    // --- Motor Information (if applicable) ---

    @Column(name = "motor_manufacturer", length = 100)
    private String motorManufacturer;

    @Column(name = "motor_model", length = 100)
    private String motorModel;

    @Column(name = "motor_od_in", precision = 5, scale = 3)
    private BigDecimal motorOdIn;

    @Column(name = "motor_bend_angle_deg", precision = 4, scale = 2)
    private BigDecimal motorBendAngleDeg;

    @Column(name = "motor_lobe_configuration", length = 20)
    private String motorLobeConfiguration;

    // --- RSS Information (if applicable) ---

    @Column(name = "rss_manufacturer", length = 100)
    private String rssManufacturer;

    @Column(name = "rss_model", length = 100)
    private String rssModel;

    @Column(name = "rss_type", length = 50)
    private String rssType;

    // --- Status and Tracking ---

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "total_footage_drilled", precision = 10, scale = 2)
    private BigDecimal totalFootageDrilled;

    @Column(name = "total_hours_on_bottom", precision = 10, scale = 2)
    private BigDecimal totalHoursOnBottom;

    @Column(name = "total_runs")
    private Integer totalRuns;

    // --- Components (JSON detailed list) ---

    @Type(JsonBinaryType.class)
    @Column(name = "components_json", columnDefinition = "jsonb")
    private JsonNode componentsJson;

    // --- Dull Grading (after pulling) ---

    @Column(name = "bit_dull_inner", length = 10)
    private String bitDullInner;

    @Column(name = "bit_dull_outer", length = 10)
    private String bitDullOuter;

    @Column(name = "bit_dull_char", length = 50)
    private String bitDullChar;

    @Column(name = "bit_dull_location", length = 10)
    private String bitDullLocation;

    @Column(name = "bit_bearing_condition", length = 10)
    private String bitBearingCondition;

    @Column(name = "bit_gauge_condition", length = 10)
    private String bitGaugeCondition;

    @Column(name = "bit_reason_pulled", length = 100)
    private String bitReasonPulled;

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
            status = "AVAILABLE";
        }
        if (isDirectional == null) {
            isDirectional = false;
        }
        if (totalFootageDrilled == null) {
            totalFootageDrilled = BigDecimal.ZERO;
        }
        if (totalHoursOnBottom == null) {
            totalHoursOnBottom = BigDecimal.ZERO;
        }
        if (totalRuns == null) {
            totalRuns = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
