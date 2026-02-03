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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Drilling Rig.
 * Maps to a ThingsBoard Asset of type "dr_rig" with SERVER_SCOPE attributes.
 *
 * The rig is the main drilling unit (digital twin) with child assets for components.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrRigDto {

    // Asset Type constant
    public static final String ASSET_TYPE = "dr_rig";

    // Attribute key constants
    public static final String ATTR_RIG_CODE = "rig_code";
    public static final String ATTR_RIG_NAME = "rig_name";
    public static final String ATTR_RIG_TYPE = "rig_type";
    public static final String ATTR_OPERATIONAL_STATUS = "operational_status";
    public static final String ATTR_CONTRACTOR = "contractor";
    public static final String ATTR_MANUFACTURER = "manufacturer";
    public static final String ATTR_MODEL = "model";
    public static final String ATTR_YEAR_BUILT = "year_built";
    public static final String ATTR_MAX_HOOKLOAD_LBS = "max_hookload_lbs";
    public static final String ATTR_MAX_ROTARY_TORQUE_FT_LBS = "max_rotary_torque_ft_lbs";
    public static final String ATTR_MAX_DEPTH_CAPABILITY_FT = "max_depth_capability_ft";
    public static final String ATTR_CURRENT_WELL_ID = "current_well_id";
    public static final String ATTR_CURRENT_RUN_ID = "current_run_id";
    public static final String ATTR_CURRENT_LOCATION = "current_location";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";
    public static final String ATTR_TOTAL_WELLS_DRILLED = "total_wells_drilled";
    public static final String ATTR_TOTAL_FOOTAGE_DRILLED_FT = "total_footage_drilled_ft";
    public static final String ATTR_TOTAL_NPT_HOURS = "total_npt_hours";
    public static final String ATTR_TOTAL_OPERATIONAL_HOURS = "total_operational_hours";
    public static final String ATTR_LAST_RIG_INSPECTION_DATE = "last_rig_inspection_date";
    public static final String ATTR_NEXT_RIG_INSPECTION_DUE = "next_rig_inspection_due";
    public static final String ATTR_BOP_TEST_DATE = "bop_test_date";
    public static final String ATTR_CERTIFICATION_EXPIRY_DATE = "certification_expiry_date";
    public static final String ATTR_NOTES = "notes";
    // Child asset references
    public static final String ATTR_DRAWWORKS_ASSET_ID = "drawworks_asset_id";
    public static final String ATTR_TOP_DRIVE_ASSET_ID = "top_drive_asset_id";
    public static final String ATTR_MUD_PUMP_1_ASSET_ID = "mud_pump_1_asset_id";
    public static final String ATTR_MUD_PUMP_2_ASSET_ID = "mud_pump_2_asset_id";
    public static final String ATTR_MUD_PUMP_3_ASSET_ID = "mud_pump_3_asset_id";
    public static final String ATTR_MUD_SYSTEM_ASSET_ID = "mud_system_asset_id";
    public static final String ATTR_BOP_STACK_ASSET_ID = "bop_stack_asset_id";
    public static final String ATTR_GAS_DETECTOR_ASSET_ID = "gas_detector_asset_id";

    // Asset identity (the asset IS the rig)
    private UUID assetId;
    private UUID tenantId;
    private String name;   // Asset name
    private String label;  // Asset label

    // Rig identification
    private String rigCode;
    private String rigName;

    // Child asset references (created by template)
    private UUID drawworksAssetId;
    private UUID topDriveAssetId;
    private UUID mudPump1AssetId;
    private UUID mudPump2AssetId;
    private UUID mudPump3AssetId;
    private UUID mudSystemAssetId;
    private UUID bopStackAssetId;
    private UUID gasDetectorAssetId;

    // Rig type and status
    private String rigType;           // LAND, JACKUP, SEMI_SUBMERSIBLE, DRILLSHIP, etc.
    private String operationalStatus; // DRILLING, STANDBY, MAINTENANCE, etc.

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
     * Convert DTO to map of attributes for saving to ThingsBoard
     */
    public Map<String, Object> toAttributeMap() {
        Map<String, Object> attributes = new HashMap<>();

        putIfNotNull(attributes, ATTR_RIG_CODE, rigCode);
        putIfNotNull(attributes, ATTR_RIG_NAME, rigName);
        putIfNotNull(attributes, ATTR_RIG_TYPE, rigType);
        putIfNotNull(attributes, ATTR_OPERATIONAL_STATUS, operationalStatus);
        putIfNotNull(attributes, ATTR_CONTRACTOR, contractor);
        putIfNotNull(attributes, ATTR_MANUFACTURER, manufacturer);
        putIfNotNull(attributes, ATTR_MODEL, model);
        putIfNotNull(attributes, ATTR_YEAR_BUILT, yearBuilt);
        putIfNotNull(attributes, ATTR_MAX_HOOKLOAD_LBS, maxHookloadLbs);
        putIfNotNull(attributes, ATTR_MAX_ROTARY_TORQUE_FT_LBS, maxRotaryTorqueFtLbs);
        putIfNotNull(attributes, ATTR_MAX_DEPTH_CAPABILITY_FT, maxDepthCapabilityFt);
        putIfNotNull(attributes, ATTR_CURRENT_WELL_ID, currentWellId != null ? currentWellId.toString() : null);
        putIfNotNull(attributes, ATTR_CURRENT_RUN_ID, currentRunId != null ? currentRunId.toString() : null);
        putIfNotNull(attributes, ATTR_CURRENT_LOCATION, currentLocation);
        putIfNotNull(attributes, ATTR_LATITUDE, latitude);
        putIfNotNull(attributes, ATTR_LONGITUDE, longitude);
        putIfNotNull(attributes, ATTR_TOTAL_WELLS_DRILLED, totalWellsDrilled);
        putIfNotNull(attributes, ATTR_TOTAL_FOOTAGE_DRILLED_FT, totalFootageDrilledFt);
        putIfNotNull(attributes, ATTR_TOTAL_NPT_HOURS, totalNptHours);
        putIfNotNull(attributes, ATTR_TOTAL_OPERATIONAL_HOURS, totalOperationalHours);
        putIfNotNull(attributes, ATTR_LAST_RIG_INSPECTION_DATE, lastRigInspectionDate);
        putIfNotNull(attributes, ATTR_NEXT_RIG_INSPECTION_DUE, nextRigInspectionDue);
        putIfNotNull(attributes, ATTR_BOP_TEST_DATE, bopTestDate);
        putIfNotNull(attributes, ATTR_CERTIFICATION_EXPIRY_DATE, certificationExpiryDate);
        putIfNotNull(attributes, ATTR_NOTES, notes);

        // Child asset references
        putIfNotNull(attributes, ATTR_DRAWWORKS_ASSET_ID, drawworksAssetId != null ? drawworksAssetId.toString() : null);
        putIfNotNull(attributes, ATTR_TOP_DRIVE_ASSET_ID, topDriveAssetId != null ? topDriveAssetId.toString() : null);
        putIfNotNull(attributes, ATTR_MUD_PUMP_1_ASSET_ID, mudPump1AssetId != null ? mudPump1AssetId.toString() : null);
        putIfNotNull(attributes, ATTR_MUD_PUMP_2_ASSET_ID, mudPump2AssetId != null ? mudPump2AssetId.toString() : null);
        putIfNotNull(attributes, ATTR_MUD_PUMP_3_ASSET_ID, mudPump3AssetId != null ? mudPump3AssetId.toString() : null);
        putIfNotNull(attributes, ATTR_MUD_SYSTEM_ASSET_ID, mudSystemAssetId != null ? mudSystemAssetId.toString() : null);
        putIfNotNull(attributes, ATTR_BOP_STACK_ASSET_ID, bopStackAssetId != null ? bopStackAssetId.toString() : null);
        putIfNotNull(attributes, ATTR_GAS_DETECTOR_ASSET_ID, gasDetectorAssetId != null ? gasDetectorAssetId.toString() : null);

        return attributes;
    }

    /**
     * Create DTO from Asset and its attributes
     */
    public static DrRigDto fromAssetAndAttributes(Asset asset, List<AttributeKvEntry> attributes) {
        if (asset == null) {
            return null;
        }

        DrRigDto dto = new DrRigDto();
        dto.assetId = asset.getId().getId();
        dto.tenantId = asset.getTenantId().getId();
        dto.name = asset.getName();
        dto.label = asset.getLabel();
        dto.createdTime = asset.getCreatedTime();

        // Parse attributes
        for (AttributeKvEntry attr : attributes) {
            String key = attr.getKey();
            switch (key) {
                case ATTR_RIG_CODE:
                    dto.rigCode = attr.getStrValue().orElse(null);
                    break;
                case ATTR_RIG_NAME:
                    dto.rigName = attr.getStrValue().orElse(null);
                    break;
                case ATTR_RIG_TYPE:
                    dto.rigType = attr.getStrValue().orElse(null);
                    break;
                case ATTR_OPERATIONAL_STATUS:
                    dto.operationalStatus = attr.getStrValue().orElse(null);
                    break;
                case ATTR_CONTRACTOR:
                    dto.contractor = attr.getStrValue().orElse(null);
                    break;
                case ATTR_MANUFACTURER:
                    dto.manufacturer = attr.getStrValue().orElse(null);
                    break;
                case ATTR_MODEL:
                    dto.model = attr.getStrValue().orElse(null);
                    break;
                case ATTR_YEAR_BUILT:
                    dto.yearBuilt = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_MAX_HOOKLOAD_LBS:
                    dto.maxHookloadLbs = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_MAX_ROTARY_TORQUE_FT_LBS:
                    dto.maxRotaryTorqueFtLbs = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_MAX_DEPTH_CAPABILITY_FT:
                    dto.maxDepthCapabilityFt = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_CURRENT_WELL_ID:
                    dto.currentWellId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_CURRENT_RUN_ID:
                    dto.currentRunId = attr.getStrValue().map(UUID::fromString).orElse(null);
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
                case ATTR_TOTAL_WELLS_DRILLED:
                    dto.totalWellsDrilled = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_TOTAL_FOOTAGE_DRILLED_FT:
                    dto.totalFootageDrilledFt = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_NPT_HOURS:
                    dto.totalNptHours = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_OPERATIONAL_HOURS:
                    dto.totalOperationalHours = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_LAST_RIG_INSPECTION_DATE:
                    dto.lastRigInspectionDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_NEXT_RIG_INSPECTION_DUE:
                    dto.nextRigInspectionDue = attr.getLongValue().orElse(null);
                    break;
                case ATTR_BOP_TEST_DATE:
                    dto.bopTestDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_CERTIFICATION_EXPIRY_DATE:
                    dto.certificationExpiryDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_NOTES:
                    dto.notes = attr.getStrValue().orElse(null);
                    break;
                case ATTR_DRAWWORKS_ASSET_ID:
                    dto.drawworksAssetId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_TOP_DRIVE_ASSET_ID:
                    dto.topDriveAssetId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_MUD_PUMP_1_ASSET_ID:
                    dto.mudPump1AssetId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_MUD_PUMP_2_ASSET_ID:
                    dto.mudPump2AssetId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_MUD_PUMP_3_ASSET_ID:
                    dto.mudPump3AssetId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_MUD_SYSTEM_ASSET_ID:
                    dto.mudSystemAssetId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_BOP_STACK_ASSET_ID:
                    dto.bopStackAssetId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_GAS_DETECTOR_ASSET_ID:
                    dto.gasDetectorAssetId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                default:
                    // Ignore unknown attributes
                    break;
            }
        }

        // Calculate BOP test overdue (14 days)
        if (dto.bopTestDate != null) {
            long fourteenDaysMs = 14L * 24 * 60 * 60 * 1000;
            dto.bopTestOverdue = (System.currentTimeMillis() - dto.bopTestDate) > fourteenDaysMs;
        }

        return dto;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
