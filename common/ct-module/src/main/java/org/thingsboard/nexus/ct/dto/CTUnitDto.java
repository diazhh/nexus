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
package org.thingsboard.nexus.ct.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.nexus.ct.model.CTUnit;
import org.thingsboard.nexus.ct.model.UnitStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTUnitDto {

    private UUID id;
    private UUID tenantId;
    private String unitCode;
    private String unitName;
    private UUID assetId;

    private String manufacturer;
    private String model;
    private String serialNumber;
    private Integer yearManufactured;

    private Integer maxPressurePsi;
    private Integer maxTensionLbf;
    private Integer maxSpeedFtMin;
    private BigDecimal maxTubingOdInch;

    private UnitStatus operationalStatus;
    private String currentLocation;
    private BigDecimal latitude;
    private BigDecimal longitude;

    private BigDecimal totalOperationalHours;
    private Integer totalJobsCompleted;
    private BigDecimal totalMetersDeployed;

    private UUID currentReelId;
    private String currentReelCode;
    private Long reelCoupledDate;

    private Long lastMaintenanceDate;
    private BigDecimal lastMaintenanceHours;
    private BigDecimal nextMaintenanceDueHours;
    private Integer maintenanceIntervalHours;
    private Boolean maintenanceOverdue;

    private Long lastPressureTestDate;
    private Integer lastPressureTestPsi;
    private Long certificationExpiryDate;

    private String notes;
    private JsonNode metadata;

    private Long createdTime;
    private Long updatedTime;

    public static CTUnitDto fromEntity(CTUnit entity) {
        if (entity == null) {
            return null;
        }
        
        CTUnitDto dto = new CTUnitDto();
        dto.id = entity.getId();
        dto.tenantId = entity.getTenantId();
        dto.unitCode = entity.getUnitCode();
        dto.unitName = entity.getUnitName();
        dto.assetId = entity.getAssetId();
        dto.manufacturer = entity.getManufacturer();
        dto.model = entity.getModel();
        dto.serialNumber = entity.getSerialNumber();
        dto.yearManufactured = entity.getYearManufactured();
        dto.maxPressurePsi = entity.getMaxPressurePsi();
        dto.maxTensionLbf = entity.getMaxTensionLbf();
        dto.maxSpeedFtMin = entity.getMaxSpeedFtMin();
        dto.maxTubingOdInch = entity.getMaxTubingOdInch();
        dto.operationalStatus = entity.getOperationalStatus();
        dto.currentLocation = entity.getCurrentLocation();
        dto.latitude = entity.getLatitude();
        dto.longitude = entity.getLongitude();
        dto.totalOperationalHours = entity.getTotalOperationalHours();
        dto.totalJobsCompleted = entity.getTotalJobsCompleted();
        dto.totalMetersDeployed = entity.getTotalMetersDeployed();
        dto.currentReelId = entity.getCurrentReelId();
        dto.reelCoupledDate = entity.getReelCoupledDate();
        dto.lastMaintenanceDate = entity.getLastMaintenanceDate();
        dto.lastMaintenanceHours = entity.getLastMaintenanceHours();
        dto.nextMaintenanceDueHours = entity.getNextMaintenanceDueHours();
        dto.maintenanceIntervalHours = entity.getMaintenanceIntervalHours();
        dto.lastPressureTestDate = entity.getLastPressureTestDate();
        dto.lastPressureTestPsi = entity.getLastPressureTestPsi();
        dto.certificationExpiryDate = entity.getCertificationExpiryDate();
        dto.notes = entity.getNotes();
        dto.metadata = entity.getMetadata();
        dto.createdTime = entity.getCreatedTime();
        dto.updatedTime = entity.getUpdatedTime();
        
        if (entity.getNextMaintenanceDueHours() != null && entity.getTotalOperationalHours() != null) {
            dto.maintenanceOverdue = entity.getTotalOperationalHours().compareTo(entity.getNextMaintenanceDueHours()) > 0;
        }
        
        return dto;
    }
}
