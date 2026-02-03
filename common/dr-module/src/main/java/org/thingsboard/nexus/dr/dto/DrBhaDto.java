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
 * DTO for Bottom Hole Assembly (BHA).
 * Maps to a ThingsBoard Asset of type "dr_bha" with SERVER_SCOPE attributes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrBhaDto {

    // Asset Type constant
    public static final String ASSET_TYPE = "dr_bha";

    // Attribute key constants
    public static final String ATTR_BHA_NUMBER = "bha_number";
    public static final String ATTR_BHA_TYPE = "bha_type";
    public static final String ATTR_IS_DIRECTIONAL = "is_directional";
    public static final String ATTR_BIT_SERIAL = "bit_serial";
    public static final String ATTR_BIT_TYPE = "bit_type";
    public static final String ATTR_BIT_SIZE_IN = "bit_size_in";
    public static final String ATTR_BIT_IADC_CODE = "bit_iadc_code";
    public static final String ATTR_BIT_MANUFACTURER = "bit_manufacturer";
    public static final String ATTR_BIT_MODEL = "bit_model";
    public static final String ATTR_BIT_TFA_SQ_IN = "bit_tfa_sq_in";
    public static final String ATTR_BIT_NOZZLES = "bit_nozzles";
    public static final String ATTR_TOTAL_LENGTH_FT = "total_length_ft";
    public static final String ATTR_TOTAL_WEIGHT_LBS = "total_weight_lbs";
    public static final String ATTR_MOTOR_MANUFACTURER = "motor_manufacturer";
    public static final String ATTR_MOTOR_MODEL = "motor_model";
    public static final String ATTR_MOTOR_OD_IN = "motor_od_in";
    public static final String ATTR_MOTOR_BEND_ANGLE_DEG = "motor_bend_angle_deg";
    public static final String ATTR_MOTOR_LOBE_CONFIGURATION = "motor_lobe_configuration";
    public static final String ATTR_RSS_MANUFACTURER = "rss_manufacturer";
    public static final String ATTR_RSS_MODEL = "rss_model";
    public static final String ATTR_RSS_TYPE = "rss_type";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_TOTAL_FOOTAGE_DRILLED = "total_footage_drilled";
    public static final String ATTR_TOTAL_HOURS_ON_BOTTOM = "total_hours_on_bottom";
    public static final String ATTR_TOTAL_RUNS = "total_runs";
    public static final String ATTR_COMPONENTS_JSON = "components_json";
    public static final String ATTR_BIT_DULL_INNER = "bit_dull_inner";
    public static final String ATTR_BIT_DULL_OUTER = "bit_dull_outer";
    public static final String ATTR_BIT_DULL_CHAR = "bit_dull_char";
    public static final String ATTR_BIT_DULL_LOCATION = "bit_dull_location";
    public static final String ATTR_BIT_BEARING_CONDITION = "bit_bearing_condition";
    public static final String ATTR_BIT_GAUGE_CONDITION = "bit_gauge_condition";
    public static final String ATTR_BIT_REASON_PULLED = "bit_reason_pulled";
    public static final String ATTR_NOTES = "notes";

    // Asset identity (the asset IS the BHA)
    private UUID assetId;
    private UUID tenantId;
    private String name;   // Asset name
    private String label;  // Asset label

    // BHA identification
    private String bhaNumber;

    // BHA Type and Configuration
    private String bhaType;  // ROTARY, MOTOR, RSS, HYBRID
    private Boolean isDirectional;

    // Bit Information
    private String bitSerial;
    private String bitType;
    private BigDecimal bitSizeIn;
    private String bitIadcCode;
    private String bitManufacturer;
    private String bitModel;
    private BigDecimal bitTfaSqIn;
    private String bitNozzles;

    // BHA Dimensions
    private BigDecimal totalLengthFt;
    private BigDecimal totalWeightLbs;

    // Motor Information
    private String motorManufacturer;
    private String motorModel;
    private BigDecimal motorOdIn;
    private BigDecimal motorBendAngleDeg;
    private String motorLobeConfiguration;

    // RSS Information
    private String rssManufacturer;
    private String rssModel;
    private String rssType;

    // Status and Tracking
    private String status;  // AVAILABLE, IN_USE, RETIRED
    private BigDecimal totalFootageDrilled;
    private BigDecimal totalHoursOnBottom;
    private Integer totalRuns;

    // Components
    private JsonNode componentsJson;

    // Dull Grading
    private String bitDullInner;
    private String bitDullOuter;
    private String bitDullChar;
    private String bitDullLocation;
    private String bitBearingCondition;
    private String bitGaugeCondition;
    private String bitReasonPulled;

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

        putIfNotNull(attributes, ATTR_BHA_NUMBER, bhaNumber);
        putIfNotNull(attributes, ATTR_BHA_TYPE, bhaType);
        putIfNotNull(attributes, ATTR_IS_DIRECTIONAL, isDirectional);
        putIfNotNull(attributes, ATTR_BIT_SERIAL, bitSerial);
        putIfNotNull(attributes, ATTR_BIT_TYPE, bitType);
        putIfNotNull(attributes, ATTR_BIT_SIZE_IN, bitSizeIn);
        putIfNotNull(attributes, ATTR_BIT_IADC_CODE, bitIadcCode);
        putIfNotNull(attributes, ATTR_BIT_MANUFACTURER, bitManufacturer);
        putIfNotNull(attributes, ATTR_BIT_MODEL, bitModel);
        putIfNotNull(attributes, ATTR_BIT_TFA_SQ_IN, bitTfaSqIn);
        putIfNotNull(attributes, ATTR_BIT_NOZZLES, bitNozzles);
        putIfNotNull(attributes, ATTR_TOTAL_LENGTH_FT, totalLengthFt);
        putIfNotNull(attributes, ATTR_TOTAL_WEIGHT_LBS, totalWeightLbs);
        putIfNotNull(attributes, ATTR_MOTOR_MANUFACTURER, motorManufacturer);
        putIfNotNull(attributes, ATTR_MOTOR_MODEL, motorModel);
        putIfNotNull(attributes, ATTR_MOTOR_OD_IN, motorOdIn);
        putIfNotNull(attributes, ATTR_MOTOR_BEND_ANGLE_DEG, motorBendAngleDeg);
        putIfNotNull(attributes, ATTR_MOTOR_LOBE_CONFIGURATION, motorLobeConfiguration);
        putIfNotNull(attributes, ATTR_RSS_MANUFACTURER, rssManufacturer);
        putIfNotNull(attributes, ATTR_RSS_MODEL, rssModel);
        putIfNotNull(attributes, ATTR_RSS_TYPE, rssType);
        putIfNotNull(attributes, ATTR_STATUS, status);
        putIfNotNull(attributes, ATTR_TOTAL_FOOTAGE_DRILLED, totalFootageDrilled);
        putIfNotNull(attributes, ATTR_TOTAL_HOURS_ON_BOTTOM, totalHoursOnBottom);
        putIfNotNull(attributes, ATTR_TOTAL_RUNS, totalRuns);
        putIfNotNull(attributes, ATTR_COMPONENTS_JSON, componentsJson);
        putIfNotNull(attributes, ATTR_BIT_DULL_INNER, bitDullInner);
        putIfNotNull(attributes, ATTR_BIT_DULL_OUTER, bitDullOuter);
        putIfNotNull(attributes, ATTR_BIT_DULL_CHAR, bitDullChar);
        putIfNotNull(attributes, ATTR_BIT_DULL_LOCATION, bitDullLocation);
        putIfNotNull(attributes, ATTR_BIT_BEARING_CONDITION, bitBearingCondition);
        putIfNotNull(attributes, ATTR_BIT_GAUGE_CONDITION, bitGaugeCondition);
        putIfNotNull(attributes, ATTR_BIT_REASON_PULLED, bitReasonPulled);
        putIfNotNull(attributes, ATTR_NOTES, notes);

        return attributes;
    }

    /**
     * Create DTO from Asset and its attributes
     */
    public static DrBhaDto fromAssetAndAttributes(Asset asset, List<AttributeKvEntry> attributes) {
        if (asset == null) {
            return null;
        }

        DrBhaDto dto = new DrBhaDto();
        dto.assetId = asset.getId().getId();
        dto.tenantId = asset.getTenantId().getId();
        dto.name = asset.getName();
        dto.label = asset.getLabel();
        dto.createdTime = asset.getCreatedTime();

        // Parse attributes
        for (AttributeKvEntry attr : attributes) {
            String key = attr.getKey();
            switch (key) {
                case ATTR_BHA_NUMBER:
                    dto.bhaNumber = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BHA_TYPE:
                    dto.bhaType = attr.getStrValue().orElse(null);
                    break;
                case ATTR_IS_DIRECTIONAL:
                    dto.isDirectional = attr.getBooleanValue().orElse(null);
                    break;
                case ATTR_BIT_SERIAL:
                    dto.bitSerial = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_TYPE:
                    dto.bitType = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_SIZE_IN:
                    dto.bitSizeIn = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_BIT_IADC_CODE:
                    dto.bitIadcCode = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_MANUFACTURER:
                    dto.bitManufacturer = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_MODEL:
                    dto.bitModel = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_TFA_SQ_IN:
                    dto.bitTfaSqIn = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_BIT_NOZZLES:
                    dto.bitNozzles = attr.getStrValue().orElse(null);
                    break;
                case ATTR_TOTAL_LENGTH_FT:
                    dto.totalLengthFt = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_WEIGHT_LBS:
                    dto.totalWeightLbs = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_MOTOR_MANUFACTURER:
                    dto.motorManufacturer = attr.getStrValue().orElse(null);
                    break;
                case ATTR_MOTOR_MODEL:
                    dto.motorModel = attr.getStrValue().orElse(null);
                    break;
                case ATTR_MOTOR_OD_IN:
                    dto.motorOdIn = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_MOTOR_BEND_ANGLE_DEG:
                    dto.motorBendAngleDeg = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_MOTOR_LOBE_CONFIGURATION:
                    dto.motorLobeConfiguration = attr.getStrValue().orElse(null);
                    break;
                case ATTR_RSS_MANUFACTURER:
                    dto.rssManufacturer = attr.getStrValue().orElse(null);
                    break;
                case ATTR_RSS_MODEL:
                    dto.rssModel = attr.getStrValue().orElse(null);
                    break;
                case ATTR_RSS_TYPE:
                    dto.rssType = attr.getStrValue().orElse(null);
                    break;
                case ATTR_STATUS:
                    dto.status = attr.getStrValue().orElse(null);
                    break;
                case ATTR_TOTAL_FOOTAGE_DRILLED:
                    dto.totalFootageDrilled = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_HOURS_ON_BOTTOM:
                    dto.totalHoursOnBottom = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_RUNS:
                    dto.totalRuns = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_BIT_DULL_INNER:
                    dto.bitDullInner = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_DULL_OUTER:
                    dto.bitDullOuter = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_DULL_CHAR:
                    dto.bitDullChar = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_DULL_LOCATION:
                    dto.bitDullLocation = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_BEARING_CONDITION:
                    dto.bitBearingCondition = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_GAUGE_CONDITION:
                    dto.bitGaugeCondition = attr.getStrValue().orElse(null);
                    break;
                case ATTR_BIT_REASON_PULLED:
                    dto.bitReasonPulled = attr.getStrValue().orElse(null);
                    break;
                case ATTR_NOTES:
                    dto.notes = attr.getStrValue().orElse(null);
                    break;
                default:
                    // Ignore unknown attributes
                    break;
            }
        }

        return dto;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
