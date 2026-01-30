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
package org.thingsboard.nexus.dr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.nexus.dr.model.DrRig;
import org.thingsboard.nexus.dr.model.enums.RigStatus;
import org.thingsboard.nexus.dr.model.enums.RigType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Drilling Rig
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrRigDto {

    private UUID id;
    private UUID tenantId;
    private String rigCode;
    private String rigName;
    private UUID assetId;

    // Child asset references
    private UUID drawworksAssetId;
    private UUID topDriveAssetId;
    private UUID mudPump1AssetId;
    private UUID mudPump2AssetId;
    private UUID mudPump3AssetId;
    private UUID mudSystemAssetId;
    private UUID bopStackAssetId;
    private UUID gasDetectorAssetId;

    // Rig type and status
    private RigType rigType;
    private RigStatus operationalStatus;

    // Rig specifications
    private String contractor;
    private String manufacturer;
    private String model;
    private Integer yearBuilt;
    private Integer maxHookloadLbs;
    private Integer maxRotaryTorqueFtLbs;
    private BigDecimal maxDepthCapabilityFt;

    // Current operation
    private UUID currentWellId;
    private String currentWellName;  // enriched field
    private UUID currentRunId;
    private String currentRunNumber; // enriched field

    // Location
    private String currentLocation;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Statistics
    private Integer totalWellsDrilled;
    private BigDecimal totalFootageDrilledFt;
    private BigDecimal totalNptHours;
    private BigDecimal totalOperationalHours;

    // Maintenance
    private Long lastRigInspectionDate;
    private Long nextRigInspectionDue;
    private Long bopTestDate;
    private Long certificationExpiryDate;
    private Boolean bopTestOverdue;  // calculated field

    // Metadata
    private String notes;
    private JsonNode metadata;

    private Long createdTime;
    private Long updatedTime;

    /**
     * Create DTO from entity
     */
    public static DrRigDto fromEntity(DrRig entity) {
        if (entity == null) {
            return null;
        }

        DrRigDto dto = new DrRigDto();
        dto.id = entity.getId();
        dto.tenantId = entity.getTenantId();
        dto.rigCode = entity.getRigCode();
        dto.rigName = entity.getRigName();
        dto.assetId = entity.getAssetId();

        // Child assets
        dto.drawworksAssetId = entity.getDrawworksAssetId();
        dto.topDriveAssetId = entity.getTopDriveAssetId();
        dto.mudPump1AssetId = entity.getMudPump1AssetId();
        dto.mudPump2AssetId = entity.getMudPump2AssetId();
        dto.mudPump3AssetId = entity.getMudPump3AssetId();
        dto.mudSystemAssetId = entity.getMudSystemAssetId();
        dto.bopStackAssetId = entity.getBopStackAssetId();
        dto.gasDetectorAssetId = entity.getGasDetectorAssetId();

        // Type and status
        dto.rigType = entity.getRigType();
        dto.operationalStatus = entity.getOperationalStatus();

        // Specifications
        dto.contractor = entity.getContractor();
        dto.manufacturer = entity.getManufacturer();
        dto.model = entity.getModel();
        dto.yearBuilt = entity.getYearBuilt();
        dto.maxHookloadLbs = entity.getMaxHookloadLbs();
        dto.maxRotaryTorqueFtLbs = entity.getMaxRotaryTorqueFtLbs();
        dto.maxDepthCapabilityFt = entity.getMaxDepthCapabilityFt();

        // Current operation
        dto.currentWellId = entity.getCurrentWellId();
        dto.currentRunId = entity.getCurrentRunId();

        // Location
        dto.currentLocation = entity.getCurrentLocation();
        dto.latitude = entity.getLatitude();
        dto.longitude = entity.getLongitude();

        // Statistics
        dto.totalWellsDrilled = entity.getTotalWellsDrilled();
        dto.totalFootageDrilledFt = entity.getTotalFootageDrilledFt();
        dto.totalNptHours = entity.getTotalNptHours();
        dto.totalOperationalHours = entity.getTotalOperationalHours();

        // Maintenance
        dto.lastRigInspectionDate = entity.getLastRigInspectionDate();
        dto.nextRigInspectionDue = entity.getNextRigInspectionDue();
        dto.bopTestDate = entity.getBopTestDate();
        dto.certificationExpiryDate = entity.getCertificationExpiryDate();

        // Calculate BOP test overdue (14 days)
        if (entity.getBopTestDate() != null) {
            long fourteenDaysMs = 14L * 24 * 60 * 60 * 1000;
            dto.bopTestOverdue = (System.currentTimeMillis() - entity.getBopTestDate()) > fourteenDaysMs;
        }

        // Metadata
        dto.notes = entity.getNotes();
        dto.metadata = entity.getMetadata();

        dto.createdTime = entity.getCreatedTime();
        dto.updatedTime = entity.getUpdatedTime();

        return dto;
    }

    /**
     * Convert DTO to entity for persistence
     */
    public DrRig toEntity() {
        DrRig entity = new DrRig();
        entity.setId(this.id);
        entity.setTenantId(this.tenantId);
        entity.setRigCode(this.rigCode);
        entity.setRigName(this.rigName);
        entity.setAssetId(this.assetId);

        // Child assets
        entity.setDrawworksAssetId(this.drawworksAssetId);
        entity.setTopDriveAssetId(this.topDriveAssetId);
        entity.setMudPump1AssetId(this.mudPump1AssetId);
        entity.setMudPump2AssetId(this.mudPump2AssetId);
        entity.setMudPump3AssetId(this.mudPump3AssetId);
        entity.setMudSystemAssetId(this.mudSystemAssetId);
        entity.setBopStackAssetId(this.bopStackAssetId);
        entity.setGasDetectorAssetId(this.gasDetectorAssetId);

        // Type and status
        entity.setRigType(this.rigType);
        entity.setOperationalStatus(this.operationalStatus);

        // Specifications
        entity.setContractor(this.contractor);
        entity.setManufacturer(this.manufacturer);
        entity.setModel(this.model);
        entity.setYearBuilt(this.yearBuilt);
        entity.setMaxHookloadLbs(this.maxHookloadLbs);
        entity.setMaxRotaryTorqueFtLbs(this.maxRotaryTorqueFtLbs);
        entity.setMaxDepthCapabilityFt(this.maxDepthCapabilityFt);

        // Current operation
        entity.setCurrentWellId(this.currentWellId);
        entity.setCurrentRunId(this.currentRunId);

        // Location
        entity.setCurrentLocation(this.currentLocation);
        entity.setLatitude(this.latitude);
        entity.setLongitude(this.longitude);

        // Statistics
        entity.setTotalWellsDrilled(this.totalWellsDrilled);
        entity.setTotalFootageDrilledFt(this.totalFootageDrilledFt);
        entity.setTotalNptHours(this.totalNptHours);
        entity.setTotalOperationalHours(this.totalOperationalHours);

        // Maintenance
        entity.setLastRigInspectionDate(this.lastRigInspectionDate);
        entity.setNextRigInspectionDue(this.nextRigInspectionDue);
        entity.setBopTestDate(this.bopTestDate);
        entity.setCertificationExpiryDate(this.certificationExpiryDate);

        // Metadata
        entity.setNotes(this.notes);
        entity.setMetadata(this.metadata);

        return entity;
    }
}
