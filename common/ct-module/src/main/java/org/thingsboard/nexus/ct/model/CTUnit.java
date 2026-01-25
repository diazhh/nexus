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
package org.thingsboard.nexus.ct.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ct_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTUnit {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "unit_code", unique = true, nullable = false, length = 50)
    private String unitCode;

    @Column(name = "unit_name", nullable = false)
    private String unitName;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "year_manufactured")
    private Integer yearManufactured;

    @Column(name = "max_pressure_psi")
    private Integer maxPressurePsi;

    @Column(name = "max_tension_lbf")
    private Integer maxTensionLbf;

    @Column(name = "max_speed_ft_min")
    private Integer maxSpeedFtMin;

    @Column(name = "max_tubing_od_inch", precision = 4, scale = 3)
    private BigDecimal maxTubingOdInch;

    @Column(name = "hydraulic_system_asset_id")
    private UUID hydraulicSystemAssetId;

    @Column(name = "injection_system_asset_id")
    private UUID injectionSystemAssetId;

    @Column(name = "control_system_asset_id")
    private UUID controlSystemAssetId;

    @Column(name = "power_pack_asset_id")
    private UUID powerPackAssetId;

    @Column(name = "gooseneck_asset_id")
    private UUID gooseneckAssetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 50)
    private UnitStatus operationalStatus;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "total_operational_hours", precision = 10, scale = 2)
    private BigDecimal totalOperationalHours;

    @Column(name = "total_jobs_completed")
    private Integer totalJobsCompleted;

    @Column(name = "total_meters_deployed", precision = 12, scale = 2)
    private BigDecimal totalMetersDeployed;

    @Column(name = "current_reel_id")
    private UUID currentReelId;

    @Column(name = "reel_coupled_date")
    private Long reelCoupledDate;

    @Column(name = "last_maintenance_date")
    private Long lastMaintenanceDate;

    @Column(name = "last_maintenance_hours", precision = 10, scale = 2)
    private BigDecimal lastMaintenanceHours;

    @Column(name = "next_maintenance_due_hours", precision = 10, scale = 2)
    private BigDecimal nextMaintenanceDueHours;

    @Column(name = "maintenance_interval_hours")
    private Integer maintenanceIntervalHours;

    @Column(name = "last_pressure_test_date")
    private Long lastPressureTestDate;

    @Column(name = "last_pressure_test_psi")
    private Integer lastPressureTestPsi;

    @Column(name = "certification_expiry_date")
    private Long certificationExpiryDate;

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

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getUnitCode() { return unitCode; }
    public String getUnitName() { return unitName; }
    public UUID getAssetId() { return assetId; }
    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public String getSerialNumber() { return serialNumber; }
    public Integer getYearManufactured() { return yearManufactured; }
    public Integer getMaxPressurePsi() { return maxPressurePsi; }
    public Integer getMaxTensionLbf() { return maxTensionLbf; }
    public Integer getMaxSpeedFtMin() { return maxSpeedFtMin; }
    public BigDecimal getMaxTubingOdInch() { return maxTubingOdInch; }
    public UnitStatus getOperationalStatus() { return operationalStatus; }
    public String getCurrentLocation() { return currentLocation; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public BigDecimal getTotalOperationalHours() { return totalOperationalHours; }
    public Integer getTotalJobsCompleted() { return totalJobsCompleted; }
    public BigDecimal getTotalMetersDeployed() { return totalMetersDeployed; }
    public UUID getCurrentReelId() { return currentReelId; }
    public Long getReelCoupledDate() { return reelCoupledDate; }
    public Long getLastMaintenanceDate() { return lastMaintenanceDate; }
    public BigDecimal getLastMaintenanceHours() { return lastMaintenanceHours; }
    public BigDecimal getNextMaintenanceDueHours() { return nextMaintenanceDueHours; }
    public Integer getMaintenanceIntervalHours() { return maintenanceIntervalHours; }
    public Long getLastPressureTestDate() { return lastPressureTestDate; }
    public Integer getLastPressureTestPsi() { return lastPressureTestPsi; }
    public Long getCertificationExpiryDate() { return certificationExpiryDate; }
    public String getNotes() { return notes; }
    public JsonNode getMetadata() { return metadata; }
    public Long getCreatedTime() { return createdTime; }
    public Long getUpdatedTime() { return updatedTime; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public void setModel(String model) { this.model = model; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public void setYearManufactured(Integer yearManufactured) { this.yearManufactured = yearManufactured; }
    public void setMaxPressurePsi(Integer maxPressurePsi) { this.maxPressurePsi = maxPressurePsi; }
    public void setMaxTensionLbf(Integer maxTensionLbf) { this.maxTensionLbf = maxTensionLbf; }
    public void setMaxSpeedFtMin(Integer maxSpeedFtMin) { this.maxSpeedFtMin = maxSpeedFtMin; }
    public void setMaxTubingOdInch(BigDecimal maxTubingOdInch) { this.maxTubingOdInch = maxTubingOdInch; }
    public void setOperationalStatus(UnitStatus operationalStatus) { this.operationalStatus = operationalStatus; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setTotalOperationalHours(BigDecimal totalOperationalHours) { this.totalOperationalHours = totalOperationalHours; }
    public void setTotalJobsCompleted(Integer totalJobsCompleted) { this.totalJobsCompleted = totalJobsCompleted; }
    public void setTotalMetersDeployed(BigDecimal totalMetersDeployed) { this.totalMetersDeployed = totalMetersDeployed; }
    public void setCurrentReelId(UUID currentReelId) { this.currentReelId = currentReelId; }
    public void setReelCoupledDate(Long reelCoupledDate) { this.reelCoupledDate = reelCoupledDate; }
    public void setLastMaintenanceDate(Long lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }
    public void setLastMaintenanceHours(BigDecimal lastMaintenanceHours) { this.lastMaintenanceHours = lastMaintenanceHours; }
    public void setNextMaintenanceDueHours(BigDecimal nextMaintenanceDueHours) { this.nextMaintenanceDueHours = nextMaintenanceDueHours; }
    public void setMaintenanceIntervalHours(Integer maintenanceIntervalHours) { this.maintenanceIntervalHours = maintenanceIntervalHours; }
    public void setLastPressureTestDate(Long lastPressureTestDate) { this.lastPressureTestDate = lastPressureTestDate; }
    public void setLastPressureTestPsi(Integer lastPressureTestPsi) { this.lastPressureTestPsi = lastPressureTestPsi; }
    public void setCertificationExpiryDate(Long certificationExpiryDate) { this.certificationExpiryDate = certificationExpiryDate; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setMetadata(JsonNode metadata) { this.metadata = metadata; }
    public void setCreatedTime(Long createdTime) { this.createdTime = createdTime; }
    public void setUpdatedTime(Long updatedTime) { this.updatedTime = updatedTime; }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdTime == null) {
            createdTime = System.currentTimeMillis();
        }
        if (operationalStatus == null) {
            operationalStatus = UnitStatus.STANDBY;
        }
        if (totalOperationalHours == null) {
            totalOperationalHours = BigDecimal.ZERO;
        }
        if (totalJobsCompleted == null) {
            totalJobsCompleted = 0;
        }
        if (totalMetersDeployed == null) {
            totalMetersDeployed = BigDecimal.ZERO;
        }
        if (maintenanceIntervalHours == null) {
            maintenanceIntervalHours = 500;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
