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
import org.thingsboard.nexus.ct.model.UnitStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Coiled Tubing Unit.
 * Maps to a ThingsBoard Asset of type "ct_unit" with SERVER_SCOPE attributes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTUnitDto {

    // Asset Type constant
    public static final String ASSET_TYPE = "ct_unit";

    // Attribute key constants
    public static final String ATTR_UNIT_CODE = "unit_code";
    public static final String ATTR_UNIT_NAME = "unit_name";
    public static final String ATTR_MANUFACTURER = "manufacturer";
    public static final String ATTR_MODEL = "model";
    public static final String ATTR_SERIAL_NUMBER = "serial_number";
    public static final String ATTR_YEAR_MANUFACTURED = "year_manufactured";
    public static final String ATTR_MAX_PRESSURE_PSI = "max_pressure_psi";
    public static final String ATTR_MAX_TENSION_LBF = "max_tension_lbf";
    public static final String ATTR_MAX_SPEED_FT_MIN = "max_speed_ft_min";
    public static final String ATTR_MAX_TUBING_OD_INCH = "max_tubing_od_inch";
    public static final String ATTR_OPERATIONAL_STATUS = "operational_status";
    public static final String ATTR_CURRENT_LOCATION = "current_location";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";
    public static final String ATTR_TOTAL_OPERATIONAL_HOURS = "total_operational_hours";
    public static final String ATTR_TOTAL_JOBS_COMPLETED = "total_jobs_completed";
    public static final String ATTR_TOTAL_METERS_DEPLOYED = "total_meters_deployed";
    public static final String ATTR_CURRENT_REEL_ID = "current_reel_id";
    public static final String ATTR_CURRENT_REEL_CODE = "current_reel_code";
    public static final String ATTR_REEL_COUPLED_DATE = "reel_coupled_date";
    public static final String ATTR_LAST_MAINTENANCE_DATE = "last_maintenance_date";
    public static final String ATTR_LAST_MAINTENANCE_HOURS = "last_maintenance_hours";
    public static final String ATTR_NEXT_MAINTENANCE_DUE_HOURS = "next_maintenance_due_hours";
    public static final String ATTR_MAINTENANCE_INTERVAL_HOURS = "maintenance_interval_hours";
    public static final String ATTR_LAST_PRESSURE_TEST_DATE = "last_pressure_test_date";
    public static final String ATTR_LAST_PRESSURE_TEST_PSI = "last_pressure_test_psi";
    public static final String ATTR_CERTIFICATION_EXPIRY_DATE = "certification_expiry_date";
    public static final String ATTR_NOTES = "notes";

    // Asset identity (the asset IS the Unit)
    private UUID assetId;
    private UUID tenantId;
    private String name;   // Asset name
    private String label;  // Asset label

    // Unit identification
    private String unitCode;
    private String unitName;

    // Specifications
    private String manufacturer;
    private String model;
    private String serialNumber;
    private Integer yearManufactured;

    // Capacity
    private Integer maxPressurePsi;
    private Integer maxTensionLbf;
    private Integer maxSpeedFtMin;
    private BigDecimal maxTubingOdInch;

    // Status and Location
    private UnitStatus operationalStatus;
    private String currentLocation;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Statistics
    private BigDecimal totalOperationalHours;
    private Integer totalJobsCompleted;
    private BigDecimal totalMetersDeployed;

    // Current Reel
    private UUID currentReelId;
    private String currentReelCode;
    private Long reelCoupledDate;

    // Maintenance
    private Long lastMaintenanceDate;
    private BigDecimal lastMaintenanceHours;
    private BigDecimal nextMaintenanceDueHours;
    private Integer maintenanceIntervalHours;
    private Boolean maintenanceOverdue;

    // Certification
    private Long lastPressureTestDate;
    private Integer lastPressureTestPsi;
    private Long certificationExpiryDate;

    // Metadata
    private String notes;
    private JsonNode metadata;

    private Long createdTime;
    private Long updatedTime;

    /**
     * Convert DTO to map of attributes for saving to ThingsBoard
     */
    public Map<String, Object> toAttributeMap() {
        Map<String, Object> attributes = new HashMap<>();

        putIfNotNull(attributes, ATTR_UNIT_CODE, unitCode);
        putIfNotNull(attributes, ATTR_UNIT_NAME, unitName);
        putIfNotNull(attributes, ATTR_MANUFACTURER, manufacturer);
        putIfNotNull(attributes, ATTR_MODEL, model);
        putIfNotNull(attributes, ATTR_SERIAL_NUMBER, serialNumber);
        putIfNotNull(attributes, ATTR_YEAR_MANUFACTURED, yearManufactured);
        putIfNotNull(attributes, ATTR_MAX_PRESSURE_PSI, maxPressurePsi);
        putIfNotNull(attributes, ATTR_MAX_TENSION_LBF, maxTensionLbf);
        putIfNotNull(attributes, ATTR_MAX_SPEED_FT_MIN, maxSpeedFtMin);
        putIfNotNull(attributes, ATTR_MAX_TUBING_OD_INCH, maxTubingOdInch);
        putIfNotNull(attributes, ATTR_OPERATIONAL_STATUS, operationalStatus != null ? operationalStatus.name() : null);
        putIfNotNull(attributes, ATTR_CURRENT_LOCATION, currentLocation);
        putIfNotNull(attributes, ATTR_LATITUDE, latitude);
        putIfNotNull(attributes, ATTR_LONGITUDE, longitude);
        putIfNotNull(attributes, ATTR_TOTAL_OPERATIONAL_HOURS, totalOperationalHours);
        putIfNotNull(attributes, ATTR_TOTAL_JOBS_COMPLETED, totalJobsCompleted);
        putIfNotNull(attributes, ATTR_TOTAL_METERS_DEPLOYED, totalMetersDeployed);
        putIfNotNull(attributes, ATTR_CURRENT_REEL_ID, currentReelId != null ? currentReelId.toString() : null);
        putIfNotNull(attributes, ATTR_CURRENT_REEL_CODE, currentReelCode);
        putIfNotNull(attributes, ATTR_REEL_COUPLED_DATE, reelCoupledDate);
        putIfNotNull(attributes, ATTR_LAST_MAINTENANCE_DATE, lastMaintenanceDate);
        putIfNotNull(attributes, ATTR_LAST_MAINTENANCE_HOURS, lastMaintenanceHours);
        putIfNotNull(attributes, ATTR_NEXT_MAINTENANCE_DUE_HOURS, nextMaintenanceDueHours);
        putIfNotNull(attributes, ATTR_MAINTENANCE_INTERVAL_HOURS, maintenanceIntervalHours);
        putIfNotNull(attributes, ATTR_LAST_PRESSURE_TEST_DATE, lastPressureTestDate);
        putIfNotNull(attributes, ATTR_LAST_PRESSURE_TEST_PSI, lastPressureTestPsi);
        putIfNotNull(attributes, ATTR_CERTIFICATION_EXPIRY_DATE, certificationExpiryDate);
        putIfNotNull(attributes, ATTR_NOTES, notes);

        return attributes;
    }

    /**
     * Create DTO from Asset and its attributes
     */
    public static CTUnitDto fromAssetAndAttributes(Asset asset, List<AttributeKvEntry> attributes) {
        if (asset == null) {
            return null;
        }

        CTUnitDto dto = new CTUnitDto();
        dto.assetId = asset.getId().getId();
        dto.tenantId = asset.getTenantId().getId();
        dto.name = asset.getName();
        dto.label = asset.getLabel();
        dto.createdTime = asset.getCreatedTime();

        // Parse attributes
        for (AttributeKvEntry attr : attributes) {
            String key = attr.getKey();
            switch (key) {
                case ATTR_UNIT_CODE:
                    dto.unitCode = attr.getStrValue().orElse(null);
                    break;
                case ATTR_UNIT_NAME:
                    dto.unitName = attr.getStrValue().orElse(null);
                    break;
                case ATTR_MANUFACTURER:
                    dto.manufacturer = attr.getStrValue().orElse(null);
                    break;
                case ATTR_MODEL:
                    dto.model = attr.getStrValue().orElse(null);
                    break;
                case ATTR_SERIAL_NUMBER:
                    dto.serialNumber = attr.getStrValue().orElse(null);
                    break;
                case ATTR_YEAR_MANUFACTURED:
                    dto.yearManufactured = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_MAX_PRESSURE_PSI:
                    dto.maxPressurePsi = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_MAX_TENSION_LBF:
                    dto.maxTensionLbf = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_MAX_SPEED_FT_MIN:
                    dto.maxSpeedFtMin = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_MAX_TUBING_OD_INCH:
                    dto.maxTubingOdInch = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_OPERATIONAL_STATUS:
                    dto.operationalStatus = attr.getStrValue().map(UnitStatus::valueOf).orElse(null);
                    break;
                case ATTR_CURRENT_LOCATION:
                    dto.currentLocation = attr.getStrValue().orElse(null);
                    break;
                case ATTR_LATITUDE:
                    dto.latitude = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_LONGITUDE:
                    dto.longitude = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_OPERATIONAL_HOURS:
                    dto.totalOperationalHours = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_JOBS_COMPLETED:
                    dto.totalJobsCompleted = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_TOTAL_METERS_DEPLOYED:
                    dto.totalMetersDeployed = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_CURRENT_REEL_ID:
                    dto.currentReelId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_CURRENT_REEL_CODE:
                    dto.currentReelCode = attr.getStrValue().orElse(null);
                    break;
                case ATTR_REEL_COUPLED_DATE:
                    dto.reelCoupledDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_LAST_MAINTENANCE_DATE:
                    dto.lastMaintenanceDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_LAST_MAINTENANCE_HOURS:
                    dto.lastMaintenanceHours = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_NEXT_MAINTENANCE_DUE_HOURS:
                    dto.nextMaintenanceDueHours = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_MAINTENANCE_INTERVAL_HOURS:
                    dto.maintenanceIntervalHours = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_LAST_PRESSURE_TEST_DATE:
                    dto.lastPressureTestDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_LAST_PRESSURE_TEST_PSI:
                    dto.lastPressureTestPsi = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_CERTIFICATION_EXPIRY_DATE:
                    dto.certificationExpiryDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_NOTES:
                    dto.notes = attr.getStrValue().orElse(null);
                    break;
                default:
                    // Ignore unknown attributes
                    break;
            }
        }

        // Calculate maintenance overdue
        if (dto.nextMaintenanceDueHours != null && dto.totalOperationalHours != null) {
            dto.maintenanceOverdue = dto.totalOperationalHours.compareTo(dto.nextMaintenanceDueHours) > 0;
        }

        return dto;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
