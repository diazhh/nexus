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
import org.thingsboard.nexus.ct.model.ReelStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Coiled Tubing Reel.
 * Maps to a ThingsBoard Asset of type "ct_reel" with SERVER_SCOPE attributes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTReelDto {

    // Asset Type constant
    public static final String ASSET_TYPE = "ct_reel";

    // Attribute key constants
    public static final String ATTR_REEL_CODE = "reel_code";
    public static final String ATTR_REEL_NAME = "reel_name";
    public static final String ATTR_TUBING_OD_INCH = "tubing_od_inch";
    public static final String ATTR_TUBING_ID_INCH = "tubing_id_inch";
    public static final String ATTR_WALL_THICKNESS_INCH = "wall_thickness_inch";
    public static final String ATTR_TOTAL_LENGTH_FT = "total_length_ft";
    public static final String ATTR_MATERIAL_GRADE = "material_grade";
    public static final String ATTR_MATERIAL_YIELD_STRENGTH_PSI = "material_yield_strength_psi";
    public static final String ATTR_MATERIAL_TENSILE_STRENGTH_PSI = "material_tensile_strength_psi";
    public static final String ATTR_YOUNGS_MODULUS_PSI = "youngs_modulus_psi";
    public static final String ATTR_HAS_WELDS = "has_welds";
    public static final String ATTR_WELD_STRESS_CONCENTRATION_FACTOR = "weld_stress_concentration_factor";
    public static final String ATTR_CORROSION_ENVIRONMENT = "corrosion_environment";
    public static final String ATTR_CORROSION_FACTOR = "corrosion_factor";
    public static final String ATTR_REEL_CORE_DIAMETER_INCH = "reel_core_diameter_inch";
    public static final String ATTR_TYPICAL_GOOSENECK_RADIUS_INCH = "typical_gooseneck_radius_inch";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_CURRENT_UNIT_ID = "current_unit_id";
    public static final String ATTR_CURRENT_UNIT_CODE = "current_unit_code";
    public static final String ATTR_CURRENT_LOCATION = "current_location";
    public static final String ATTR_ACCUMULATED_FATIGUE_PERCENT = "accumulated_fatigue_percent";
    public static final String ATTR_TOTAL_CYCLES = "total_cycles";
    public static final String ATTR_ESTIMATED_REMAINING_CYCLES = "estimated_remaining_cycles";
    public static final String ATTR_FATIGUE_CALCULATION_METHOD = "fatigue_calculation_method";
    public static final String ATTR_TOTAL_JOBS_USED = "total_jobs_used";
    public static final String ATTR_TOTAL_METERS_DEPLOYED = "total_meters_deployed";
    public static final String ATTR_TOTAL_HOURS_IN_USE = "total_hours_in_use";
    public static final String ATTR_LAST_INSPECTION_DATE = "last_inspection_date";
    public static final String ATTR_LAST_INSPECTION_TYPE = "last_inspection_type";
    public static final String ATTR_LAST_INSPECTION_RESULT = "last_inspection_result";
    public static final String ATTR_NEXT_INSPECTION_DUE_DATE = "next_inspection_due_date";
    public static final String ATTR_HAS_CORROSION = "has_corrosion";
    public static final String ATTR_HAS_MECHANICAL_DAMAGE = "has_mechanical_damage";
    public static final String ATTR_OVALITY_PERCENT = "ovality_percent";
    public static final String ATTR_WALL_THICKNESS_LOSS_PERCENT = "wall_thickness_loss_percent";
    public static final String ATTR_MANUFACTURING_DATE = "manufacturing_date";
    public static final String ATTR_FIRST_USE_DATE = "first_use_date";
    public static final String ATTR_RETIREMENT_DATE = "retirement_date";
    public static final String ATTR_NOTES = "notes";

    // Asset identity (the asset IS the Reel)
    private UUID assetId;
    private UUID tenantId;
    private String name;   // Asset name
    private String label;  // Asset label

    // Reel identification
    private String reelCode;
    private String reelName;

    // Tubing specifications
    private BigDecimal tubingOdInch;
    private BigDecimal tubingIdInch;
    private BigDecimal wallThicknessInch;
    private BigDecimal totalLengthFt;

    // Material properties
    private String materialGrade;
    private Integer materialYieldStrengthPsi;
    private Integer materialTensileStrengthPsi;
    private Long youngsModulusPsi;

    // Weld and corrosion
    private Boolean hasWelds;
    private BigDecimal weldStressConcentrationFactor;
    private String corrosionEnvironment;
    private BigDecimal corrosionFactor;

    // Physical dimensions
    private BigDecimal reelCoreDiameterInch;
    private BigDecimal typicalGooseneckRadiusInch;

    // Status and location
    private ReelStatus status;
    private UUID currentUnitId;
    private String currentUnitCode;
    private String currentLocation;

    // Fatigue tracking
    private BigDecimal accumulatedFatiguePercent;
    private Integer totalCycles;
    private Integer estimatedRemainingCycles;
    private String fatigueCalculationMethod;
    private String fatigueLevel;  // Calculated field

    // Usage statistics
    private Integer totalJobsUsed;
    private BigDecimal totalMetersDeployed;
    private BigDecimal totalHoursInUse;

    // Inspection
    private Long lastInspectionDate;
    private String lastInspectionType;
    private String lastInspectionResult;
    private Long nextInspectionDueDate;

    // Condition
    private Boolean hasCorrosion;
    private Boolean hasMechanicalDamage;
    private BigDecimal ovalityPercent;
    private BigDecimal wallThicknessLossPercent;

    // Lifecycle
    private Long manufacturingDate;
    private Long firstUseDate;
    private Long retirementDate;

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

        putIfNotNull(attributes, ATTR_REEL_CODE, reelCode);
        putIfNotNull(attributes, ATTR_REEL_NAME, reelName);
        putIfNotNull(attributes, ATTR_TUBING_OD_INCH, tubingOdInch);
        putIfNotNull(attributes, ATTR_TUBING_ID_INCH, tubingIdInch);
        putIfNotNull(attributes, ATTR_WALL_THICKNESS_INCH, wallThicknessInch);
        putIfNotNull(attributes, ATTR_TOTAL_LENGTH_FT, totalLengthFt);
        putIfNotNull(attributes, ATTR_MATERIAL_GRADE, materialGrade);
        putIfNotNull(attributes, ATTR_MATERIAL_YIELD_STRENGTH_PSI, materialYieldStrengthPsi);
        putIfNotNull(attributes, ATTR_MATERIAL_TENSILE_STRENGTH_PSI, materialTensileStrengthPsi);
        putIfNotNull(attributes, ATTR_YOUNGS_MODULUS_PSI, youngsModulusPsi);
        putIfNotNull(attributes, ATTR_HAS_WELDS, hasWelds);
        putIfNotNull(attributes, ATTR_WELD_STRESS_CONCENTRATION_FACTOR, weldStressConcentrationFactor);
        putIfNotNull(attributes, ATTR_CORROSION_ENVIRONMENT, corrosionEnvironment);
        putIfNotNull(attributes, ATTR_CORROSION_FACTOR, corrosionFactor);
        putIfNotNull(attributes, ATTR_REEL_CORE_DIAMETER_INCH, reelCoreDiameterInch);
        putIfNotNull(attributes, ATTR_TYPICAL_GOOSENECK_RADIUS_INCH, typicalGooseneckRadiusInch);
        putIfNotNull(attributes, ATTR_STATUS, status != null ? status.name() : null);
        putIfNotNull(attributes, ATTR_CURRENT_UNIT_ID, currentUnitId != null ? currentUnitId.toString() : null);
        putIfNotNull(attributes, ATTR_CURRENT_UNIT_CODE, currentUnitCode);
        putIfNotNull(attributes, ATTR_CURRENT_LOCATION, currentLocation);
        putIfNotNull(attributes, ATTR_ACCUMULATED_FATIGUE_PERCENT, accumulatedFatiguePercent);
        putIfNotNull(attributes, ATTR_TOTAL_CYCLES, totalCycles);
        putIfNotNull(attributes, ATTR_ESTIMATED_REMAINING_CYCLES, estimatedRemainingCycles);
        putIfNotNull(attributes, ATTR_FATIGUE_CALCULATION_METHOD, fatigueCalculationMethod);
        putIfNotNull(attributes, ATTR_TOTAL_JOBS_USED, totalJobsUsed);
        putIfNotNull(attributes, ATTR_TOTAL_METERS_DEPLOYED, totalMetersDeployed);
        putIfNotNull(attributes, ATTR_TOTAL_HOURS_IN_USE, totalHoursInUse);
        putIfNotNull(attributes, ATTR_LAST_INSPECTION_DATE, lastInspectionDate);
        putIfNotNull(attributes, ATTR_LAST_INSPECTION_TYPE, lastInspectionType);
        putIfNotNull(attributes, ATTR_LAST_INSPECTION_RESULT, lastInspectionResult);
        putIfNotNull(attributes, ATTR_NEXT_INSPECTION_DUE_DATE, nextInspectionDueDate);
        putIfNotNull(attributes, ATTR_HAS_CORROSION, hasCorrosion);
        putIfNotNull(attributes, ATTR_HAS_MECHANICAL_DAMAGE, hasMechanicalDamage);
        putIfNotNull(attributes, ATTR_OVALITY_PERCENT, ovalityPercent);
        putIfNotNull(attributes, ATTR_WALL_THICKNESS_LOSS_PERCENT, wallThicknessLossPercent);
        putIfNotNull(attributes, ATTR_MANUFACTURING_DATE, manufacturingDate);
        putIfNotNull(attributes, ATTR_FIRST_USE_DATE, firstUseDate);
        putIfNotNull(attributes, ATTR_RETIREMENT_DATE, retirementDate);
        putIfNotNull(attributes, ATTR_NOTES, notes);

        return attributes;
    }

    /**
     * Create DTO from Asset and its attributes
     */
    public static CTReelDto fromAssetAndAttributes(Asset asset, List<AttributeKvEntry> attributes) {
        if (asset == null) {
            return null;
        }

        CTReelDto dto = new CTReelDto();
        dto.assetId = asset.getId().getId();
        dto.tenantId = asset.getTenantId().getId();
        dto.name = asset.getName();
        dto.label = asset.getLabel();
        dto.createdTime = asset.getCreatedTime();

        // Parse attributes
        for (AttributeKvEntry attr : attributes) {
            String key = attr.getKey();
            switch (key) {
                case ATTR_REEL_CODE:
                    dto.reelCode = attr.getStrValue().orElse(null);
                    break;
                case ATTR_REEL_NAME:
                    dto.reelName = attr.getStrValue().orElse(null);
                    break;
                case ATTR_TUBING_OD_INCH:
                    dto.tubingOdInch = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TUBING_ID_INCH:
                    dto.tubingIdInch = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_WALL_THICKNESS_INCH:
                    dto.wallThicknessInch = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_LENGTH_FT:
                    dto.totalLengthFt = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_MATERIAL_GRADE:
                    dto.materialGrade = attr.getStrValue().orElse(null);
                    break;
                case ATTR_MATERIAL_YIELD_STRENGTH_PSI:
                    dto.materialYieldStrengthPsi = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_MATERIAL_TENSILE_STRENGTH_PSI:
                    dto.materialTensileStrengthPsi = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_YOUNGS_MODULUS_PSI:
                    dto.youngsModulusPsi = attr.getLongValue().orElse(null);
                    break;
                case ATTR_HAS_WELDS:
                    dto.hasWelds = attr.getBooleanValue().orElse(null);
                    break;
                case ATTR_WELD_STRESS_CONCENTRATION_FACTOR:
                    dto.weldStressConcentrationFactor = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_CORROSION_ENVIRONMENT:
                    dto.corrosionEnvironment = attr.getStrValue().orElse(null);
                    break;
                case ATTR_CORROSION_FACTOR:
                    dto.corrosionFactor = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_REEL_CORE_DIAMETER_INCH:
                    dto.reelCoreDiameterInch = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TYPICAL_GOOSENECK_RADIUS_INCH:
                    dto.typicalGooseneckRadiusInch = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_STATUS:
                    dto.status = attr.getStrValue().map(ReelStatus::valueOf).orElse(null);
                    break;
                case ATTR_CURRENT_UNIT_ID:
                    dto.currentUnitId = attr.getStrValue().map(UUID::fromString).orElse(null);
                    break;
                case ATTR_CURRENT_UNIT_CODE:
                    dto.currentUnitCode = attr.getStrValue().orElse(null);
                    break;
                case ATTR_CURRENT_LOCATION:
                    dto.currentLocation = attr.getStrValue().orElse(null);
                    break;
                case ATTR_ACCUMULATED_FATIGUE_PERCENT:
                    dto.accumulatedFatiguePercent = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_CYCLES:
                    dto.totalCycles = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_ESTIMATED_REMAINING_CYCLES:
                    dto.estimatedRemainingCycles = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_FATIGUE_CALCULATION_METHOD:
                    dto.fatigueCalculationMethod = attr.getStrValue().orElse(null);
                    break;
                case ATTR_TOTAL_JOBS_USED:
                    dto.totalJobsUsed = attr.getLongValue().map(Long::intValue).orElse(null);
                    break;
                case ATTR_TOTAL_METERS_DEPLOYED:
                    dto.totalMetersDeployed = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_TOTAL_HOURS_IN_USE:
                    dto.totalHoursInUse = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_LAST_INSPECTION_DATE:
                    dto.lastInspectionDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_LAST_INSPECTION_TYPE:
                    dto.lastInspectionType = attr.getStrValue().orElse(null);
                    break;
                case ATTR_LAST_INSPECTION_RESULT:
                    dto.lastInspectionResult = attr.getStrValue().orElse(null);
                    break;
                case ATTR_NEXT_INSPECTION_DUE_DATE:
                    dto.nextInspectionDueDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_HAS_CORROSION:
                    dto.hasCorrosion = attr.getBooleanValue().orElse(null);
                    break;
                case ATTR_HAS_MECHANICAL_DAMAGE:
                    dto.hasMechanicalDamage = attr.getBooleanValue().orElse(null);
                    break;
                case ATTR_OVALITY_PERCENT:
                    dto.ovalityPercent = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_WALL_THICKNESS_LOSS_PERCENT:
                    dto.wallThicknessLossPercent = attr.getDoubleValue().map(BigDecimal::valueOf).orElse(null);
                    break;
                case ATTR_MANUFACTURING_DATE:
                    dto.manufacturingDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_FIRST_USE_DATE:
                    dto.firstUseDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_RETIREMENT_DATE:
                    dto.retirementDate = attr.getLongValue().orElse(null);
                    break;
                case ATTR_NOTES:
                    dto.notes = attr.getStrValue().orElse(null);
                    break;
                default:
                    // Ignore unknown attributes
                    break;
            }
        }

        // Calculate fatigue level
        if (dto.accumulatedFatiguePercent != null) {
            BigDecimal fatigue = dto.accumulatedFatiguePercent;
            if (fatigue.compareTo(new BigDecimal("95")) >= 0) {
                dto.fatigueLevel = "CRITICAL";
            } else if (fatigue.compareTo(new BigDecimal("80")) >= 0) {
                dto.fatigueLevel = "HIGH";
            } else if (fatigue.compareTo(new BigDecimal("60")) >= 0) {
                dto.fatigueLevel = "MODERATE";
            } else if (fatigue.compareTo(new BigDecimal("40")) >= 0) {
                dto.fatigueLevel = "GOOD";
            } else {
                dto.fatigueLevel = "EXCELLENT";
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
