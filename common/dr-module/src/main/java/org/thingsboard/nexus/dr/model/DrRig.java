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
import org.thingsboard.nexus.dr.model.enums.RigStatus;
import org.thingsboard.nexus.dr.model.enums.RigType;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing a Drilling Rig
 * The rig and its components are stored as Assets (digital twins) in ThingsBoard.
 * This table stores references to the asset_ids and operational data.
 */
@Entity
@Table(name = "dr_rigs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrRig {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rig_code", unique = true, nullable = false, length = 50)
    private String rigCode;

    @Column(name = "rig_name", nullable = false)
    private String rigName;

    /**
     * Reference to the root Asset (digital twin) in ThingsBoard
     */
    @Column(name = "asset_id", nullable = false, unique = true)
    private UUID assetId;

    // --- Child Asset References (created by template) ---

    @Column(name = "drawworks_asset_id")
    private UUID drawworksAssetId;

    @Column(name = "top_drive_asset_id")
    private UUID topDriveAssetId;

    @Column(name = "mud_pump_1_asset_id")
    private UUID mudPump1AssetId;

    @Column(name = "mud_pump_2_asset_id")
    private UUID mudPump2AssetId;

    @Column(name = "mud_pump_3_asset_id")
    private UUID mudPump3AssetId;

    @Column(name = "mud_system_asset_id")
    private UUID mudSystemAssetId;

    @Column(name = "bop_stack_asset_id")
    private UUID bopStackAssetId;

    @Column(name = "gas_detector_asset_id")
    private UUID gasDetectorAssetId;

    // --- Rig Type and Status ---

    @Enumerated(EnumType.STRING)
    @Column(name = "rig_type", length = 50)
    private RigType rigType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 50)
    private RigStatus operationalStatus;

    // --- Rig Specifications (static data stored here for reference) ---

    @Column(name = "contractor", length = 100)
    private String contractor;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "year_built")
    private Integer yearBuilt;

    @Column(name = "max_hookload_lbs")
    private Integer maxHookloadLbs;

    @Column(name = "max_rotary_torque_ft_lbs")
    private Integer maxRotaryTorqueFtLbs;

    @Column(name = "max_depth_capability_ft", precision = 10, scale = 2)
    private BigDecimal maxDepthCapabilityFt;

    // --- Current Operation References ---

    /**
     * Reference to current well being drilled (from rv-module)
     */
    @Column(name = "current_well_id")
    private UUID currentWellId;

    /**
     * Reference to current drilling run
     */
    @Column(name = "current_run_id")
    private UUID currentRunId;

    // --- Location ---

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    // --- Cumulative Statistics (updated by the module) ---

    @Column(name = "total_wells_drilled")
    private Integer totalWellsDrilled;

    @Column(name = "total_footage_drilled_ft", precision = 12, scale = 2)
    private BigDecimal totalFootageDrilledFt;

    @Column(name = "total_npt_hours", precision = 10, scale = 2)
    private BigDecimal totalNptHours;

    @Column(name = "total_operational_hours", precision = 12, scale = 2)
    private BigDecimal totalOperationalHours;

    // --- Maintenance and Certification ---

    @Column(name = "last_rig_inspection_date")
    private Long lastRigInspectionDate;

    @Column(name = "next_rig_inspection_due")
    private Long nextRigInspectionDue;

    @Column(name = "bop_test_date")
    private Long bopTestDate;

    @Column(name = "certification_expiry_date")
    private Long certificationExpiryDate;

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
        if (operationalStatus == null) {
            operationalStatus = RigStatus.STANDBY;
        }
        if (totalWellsDrilled == null) {
            totalWellsDrilled = 0;
        }
        if (totalFootageDrilledFt == null) {
            totalFootageDrilledFt = BigDecimal.ZERO;
        }
        if (totalNptHours == null) {
            totalNptHours = BigDecimal.ZERO;
        }
        if (totalOperationalHours == null) {
            totalOperationalHours = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
