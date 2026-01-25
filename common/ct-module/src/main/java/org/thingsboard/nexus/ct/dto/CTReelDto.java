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
import org.thingsboard.nexus.ct.model.CTReel;
import org.thingsboard.nexus.ct.model.ReelStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTReelDto {

    private UUID id;
    private UUID tenantId;
    private String reelCode;
    private String reelName;
    private UUID assetId;

    private BigDecimal tubingOdInch;
    private BigDecimal tubingIdInch;
    private BigDecimal wallThicknessInch;
    private BigDecimal totalLengthFt;

    private String materialGrade;
    private Integer materialYieldStrengthPsi;
    private Integer materialTensileStrengthPsi;
    private Long youngsModulusPsi;

    private Boolean hasWelds;
    private BigDecimal weldStressConcentrationFactor;
    private String corrosionEnvironment;
    private BigDecimal corrosionFactor;

    private BigDecimal reelCoreDiameterInch;
    private BigDecimal typicalGooseneckRadiusInch;

    private ReelStatus status;
    private UUID currentUnitId;
    private String currentUnitCode;
    private String currentLocation;

    private BigDecimal accumulatedFatiguePercent;
    private Integer totalCycles;
    private Integer estimatedRemainingCycles;
    private String fatigueCalculationMethod;
    private String fatigueLevel;

    private Integer totalJobsUsed;
    private BigDecimal totalMetersDeployed;
    private BigDecimal totalHoursInUse;

    private Long lastInspectionDate;
    private String lastInspectionType;
    private String lastInspectionResult;
    private Long nextInspectionDueDate;

    private Boolean hasCorrosion;
    private Boolean hasMechanicalDamage;
    private BigDecimal ovalityPercent;
    private BigDecimal wallThicknessLossPercent;

    private Long manufacturingDate;
    private Long firstUseDate;
    private Long retirementDate;

    private String notes;
    private JsonNode metadata;

    private Long createdTime;
    private Long updatedTime;

    public static CTReelDto fromEntity(CTReel entity) {
        if (entity == null) {
            return null;
        }
        
        CTReelDto dto = new CTReelDto();
        dto.id = entity.getId();
        dto.tenantId = entity.getTenantId();
        dto.reelCode = entity.getReelCode();
        dto.reelName = entity.getReelName();
        dto.assetId = entity.getAssetId();
        dto.tubingOdInch = entity.getTubingOdInch();
        dto.tubingIdInch = entity.getTubingIdInch();
        dto.wallThicknessInch = entity.getWallThicknessInch();
        dto.totalLengthFt = entity.getTotalLengthFt();
        dto.materialGrade = entity.getMaterialGrade();
        dto.materialYieldStrengthPsi = entity.getMaterialYieldStrengthPsi();
        dto.materialTensileStrengthPsi = entity.getMaterialTensileStrengthPsi();
        dto.youngsModulusPsi = entity.getYoungsModulusPsi();
        dto.hasWelds = entity.getHasWelds();
        dto.weldStressConcentrationFactor = entity.getWeldStressConcentrationFactor();
        dto.corrosionEnvironment = entity.getCorrosionEnvironment();
        dto.corrosionFactor = entity.getCorrosionFactor();
        dto.reelCoreDiameterInch = entity.getReelCoreDiameterInch();
        dto.typicalGooseneckRadiusInch = entity.getTypicalGooseneckRadiusInch();
        dto.status = entity.getStatus();
        dto.currentUnitId = entity.getCurrentUnitId();
        dto.currentLocation = entity.getCurrentLocation();
        dto.accumulatedFatiguePercent = entity.getAccumulatedFatiguePercent();
        dto.totalCycles = entity.getTotalCycles();
        dto.estimatedRemainingCycles = entity.getEstimatedRemainingCycles();
        dto.fatigueCalculationMethod = entity.getFatigueCalculationMethod();
        dto.totalJobsUsed = entity.getTotalJobsUsed();
        dto.totalMetersDeployed = entity.getTotalMetersDeployed();
        dto.totalHoursInUse = entity.getTotalHoursInUse();
        dto.lastInspectionDate = entity.getLastInspectionDate();
        dto.lastInspectionType = entity.getLastInspectionType();
        dto.lastInspectionResult = entity.getLastInspectionResult();
        dto.nextInspectionDueDate = entity.getNextInspectionDueDate();
        dto.hasCorrosion = entity.getHasCorrosion();
        dto.hasMechanicalDamage = entity.getHasMechanicalDamage();
        dto.ovalityPercent = entity.getOvalityPercent();
        dto.wallThicknessLossPercent = entity.getWallThicknessLossPercent();
        dto.manufacturingDate = entity.getManufacturingDate();
        dto.firstUseDate = entity.getFirstUseDate();
        dto.retirementDate = entity.getRetirementDate();
        dto.notes = entity.getNotes();
        dto.metadata = entity.getMetadata();
        dto.createdTime = entity.getCreatedTime();
        dto.updatedTime = entity.getUpdatedTime();

        if (entity.getAccumulatedFatiguePercent() != null) {
            BigDecimal fatigue = entity.getAccumulatedFatiguePercent();
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
}
