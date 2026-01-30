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
import org.thingsboard.nexus.dr.model.DrRun;
import org.thingsboard.nexus.dr.model.enums.HoleSection;
import org.thingsboard.nexus.dr.model.enums.RunStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Drilling Run
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrRunDto {

    private UUID id;
    private UUID tenantId;
    private String runNumber;

    // References
    private UUID rigId;
    private String rigCode;      // enriched field
    private String rigName;      // enriched field
    private UUID wellId;
    private String wellName;     // enriched field
    private UUID bhaId;
    private String bhaNumber;    // enriched field

    // MWD/LWD Tool References
    private UUID mwdToolAssetId;
    private UUID lwdToolAssetId;

    // Hole Section Configuration
    private HoleSection holeSection;
    private BigDecimal holeSizeIn;
    private BigDecimal casingSizeIn;
    private BigDecimal previousCasingShoeMdFt;

    // Planned Depths
    private BigDecimal plannedStartDepthMdFt;
    private BigDecimal plannedEndDepthMdFt;
    private BigDecimal plannedStartDepthTvdFt;
    private BigDecimal plannedEndDepthTvdFt;

    // Actual Depths
    private BigDecimal startDepthMdFt;
    private BigDecimal endDepthMdFt;
    private BigDecimal currentDepthMdFt;
    private BigDecimal startDepthTvdFt;
    private BigDecimal endDepthTvdFt;
    private BigDecimal currentDepthTvdFt;

    // Mud Properties
    private String mudType;
    private BigDecimal mudWeightPpg;
    private BigDecimal porePressurePpg;
    private BigDecimal fracGradientPpg;

    // Dates
    private Long spudDate;
    private Long startDate;
    private Long endDate;

    // Status
    private RunStatus status;

    // KPIs
    private BigDecimal totalFootageFt;
    private BigDecimal avgRopFtHr;
    private BigDecimal maxRopFtHr;
    private BigDecimal totalRotatingHours;
    private BigDecimal totalSlidingHours;
    private BigDecimal totalCirculatingHours;
    private BigDecimal totalConnectionTimeHours;
    private BigDecimal totalTripTimeHours;
    private BigDecimal totalNptHours;
    private BigDecimal drillingEfficiencyPercent;

    // Connection Statistics
    private Integer totalConnections;
    private BigDecimal avgConnectionTimeMin;

    // Survey Statistics
    private Integer surveyCount;
    private BigDecimal maxInclinationDeg;
    private BigDecimal maxDlsDegPer100ft;

    // Vibration Statistics
    private BigDecimal avgAxialVibrationG;
    private BigDecimal avgLateralVibrationG;
    private BigDecimal maxShockG;

    // End of Run Summary
    private String reasonEnded;
    private String bitConditionOut;

    // Metadata
    private String notes;
    private JsonNode metadata;

    private Long createdTime;
    private Long updatedTime;

    // Calculated fields
    private BigDecimal progressPercent;   // calculated: currentDepth / plannedEndDepth * 100
    private Long durationHours;           // calculated: endDate - startDate

    /**
     * Create DTO from entity
     */
    public static DrRunDto fromEntity(DrRun entity) {
        if (entity == null) {
            return null;
        }

        DrRunDto dto = new DrRunDto();
        dto.id = entity.getId();
        dto.tenantId = entity.getTenantId();
        dto.runNumber = entity.getRunNumber();

        // References
        dto.rigId = entity.getRigId();
        dto.wellId = entity.getWellId();
        dto.bhaId = entity.getBhaId();

        // MWD/LWD Tools
        dto.mwdToolAssetId = entity.getMwdToolAssetId();
        dto.lwdToolAssetId = entity.getLwdToolAssetId();

        // Hole Section
        dto.holeSection = entity.getHoleSection();
        dto.holeSizeIn = entity.getHoleSizeIn();
        dto.casingSizeIn = entity.getCasingSizeIn();
        dto.previousCasingShoeMdFt = entity.getPreviousCasingShoeMdFt();

        // Planned Depths
        dto.plannedStartDepthMdFt = entity.getPlannedStartDepthMdFt();
        dto.plannedEndDepthMdFt = entity.getPlannedEndDepthMdFt();
        dto.plannedStartDepthTvdFt = entity.getPlannedStartDepthTvdFt();
        dto.plannedEndDepthTvdFt = entity.getPlannedEndDepthTvdFt();

        // Actual Depths
        dto.startDepthMdFt = entity.getStartDepthMdFt();
        dto.endDepthMdFt = entity.getEndDepthMdFt();
        dto.currentDepthMdFt = entity.getCurrentDepthMdFt();
        dto.startDepthTvdFt = entity.getStartDepthTvdFt();
        dto.endDepthTvdFt = entity.getEndDepthTvdFt();
        dto.currentDepthTvdFt = entity.getCurrentDepthTvdFt();

        // Mud Properties
        dto.mudType = entity.getMudType();
        dto.mudWeightPpg = entity.getMudWeightPpg();
        dto.porePressurePpg = entity.getPorePressurePpg();
        dto.fracGradientPpg = entity.getFracGradientPpg();

        // Dates
        dto.spudDate = entity.getSpudDate();
        dto.startDate = entity.getStartDate();
        dto.endDate = entity.getEndDate();

        // Status
        dto.status = entity.getStatus();

        // KPIs
        dto.totalFootageFt = entity.getTotalFootageFt();
        dto.avgRopFtHr = entity.getAvgRopFtHr();
        dto.maxRopFtHr = entity.getMaxRopFtHr();
        dto.totalRotatingHours = entity.getTotalRotatingHours();
        dto.totalSlidingHours = entity.getTotalSlidingHours();
        dto.totalCirculatingHours = entity.getTotalCirculatingHours();
        dto.totalConnectionTimeHours = entity.getTotalConnectionTimeHours();
        dto.totalTripTimeHours = entity.getTotalTripTimeHours();
        dto.totalNptHours = entity.getTotalNptHours();
        dto.drillingEfficiencyPercent = entity.getDrillingEfficiencyPercent();

        // Connection Statistics
        dto.totalConnections = entity.getTotalConnections();
        dto.avgConnectionTimeMin = entity.getAvgConnectionTimeMin();

        // Survey Statistics
        dto.surveyCount = entity.getSurveyCount();
        dto.maxInclinationDeg = entity.getMaxInclinationDeg();
        dto.maxDlsDegPer100ft = entity.getMaxDlsDegPer100ft();

        // Vibration Statistics
        dto.avgAxialVibrationG = entity.getAvgAxialVibrationG();
        dto.avgLateralVibrationG = entity.getAvgLateralVibrationG();
        dto.maxShockG = entity.getMaxShockG();

        // End of Run Summary
        dto.reasonEnded = entity.getReasonEnded();
        dto.bitConditionOut = entity.getBitConditionOut();

        // Metadata
        dto.notes = entity.getNotes();
        dto.metadata = entity.getMetadata();

        dto.createdTime = entity.getCreatedTime();
        dto.updatedTime = entity.getUpdatedTime();

        // Calculate progress percent
        if (dto.currentDepthMdFt != null && dto.plannedEndDepthMdFt != null &&
            dto.plannedStartDepthMdFt != null &&
            dto.plannedEndDepthMdFt.compareTo(dto.plannedStartDepthMdFt) > 0) {
            BigDecimal planned = dto.plannedEndDepthMdFt.subtract(dto.plannedStartDepthMdFt);
            BigDecimal current = dto.currentDepthMdFt.subtract(dto.plannedStartDepthMdFt);
            dto.progressPercent = current.multiply(BigDecimal.valueOf(100)).divide(planned, 2, BigDecimal.ROUND_HALF_UP);
        }

        // Calculate duration in hours
        if (dto.startDate != null && dto.endDate != null) {
            dto.durationHours = (dto.endDate - dto.startDate) / (1000 * 60 * 60);
        } else if (dto.startDate != null) {
            dto.durationHours = (System.currentTimeMillis() - dto.startDate) / (1000 * 60 * 60);
        }

        return dto;
    }

    /**
     * Convert DTO to entity for persistence
     */
    public DrRun toEntity() {
        DrRun entity = new DrRun();
        entity.setId(this.id);
        entity.setTenantId(this.tenantId);
        entity.setRunNumber(this.runNumber);

        // References
        entity.setRigId(this.rigId);
        entity.setWellId(this.wellId);
        entity.setBhaId(this.bhaId);

        // MWD/LWD Tools
        entity.setMwdToolAssetId(this.mwdToolAssetId);
        entity.setLwdToolAssetId(this.lwdToolAssetId);

        // Hole Section
        entity.setHoleSection(this.holeSection);
        entity.setHoleSizeIn(this.holeSizeIn);
        entity.setCasingSizeIn(this.casingSizeIn);
        entity.setPreviousCasingShoeMdFt(this.previousCasingShoeMdFt);

        // Planned Depths
        entity.setPlannedStartDepthMdFt(this.plannedStartDepthMdFt);
        entity.setPlannedEndDepthMdFt(this.plannedEndDepthMdFt);
        entity.setPlannedStartDepthTvdFt(this.plannedStartDepthTvdFt);
        entity.setPlannedEndDepthTvdFt(this.plannedEndDepthTvdFt);

        // Actual Depths
        entity.setStartDepthMdFt(this.startDepthMdFt);
        entity.setEndDepthMdFt(this.endDepthMdFt);
        entity.setCurrentDepthMdFt(this.currentDepthMdFt);
        entity.setStartDepthTvdFt(this.startDepthTvdFt);
        entity.setEndDepthTvdFt(this.endDepthTvdFt);
        entity.setCurrentDepthTvdFt(this.currentDepthTvdFt);

        // Mud Properties
        entity.setMudType(this.mudType);
        entity.setMudWeightPpg(this.mudWeightPpg);
        entity.setPorePressurePpg(this.porePressurePpg);
        entity.setFracGradientPpg(this.fracGradientPpg);

        // Dates
        entity.setSpudDate(this.spudDate);
        entity.setStartDate(this.startDate);
        entity.setEndDate(this.endDate);

        // Status
        entity.setStatus(this.status);

        // KPIs
        entity.setTotalFootageFt(this.totalFootageFt);
        entity.setAvgRopFtHr(this.avgRopFtHr);
        entity.setMaxRopFtHr(this.maxRopFtHr);
        entity.setTotalRotatingHours(this.totalRotatingHours);
        entity.setTotalSlidingHours(this.totalSlidingHours);
        entity.setTotalCirculatingHours(this.totalCirculatingHours);
        entity.setTotalConnectionTimeHours(this.totalConnectionTimeHours);
        entity.setTotalTripTimeHours(this.totalTripTimeHours);
        entity.setTotalNptHours(this.totalNptHours);
        entity.setDrillingEfficiencyPercent(this.drillingEfficiencyPercent);

        // Connection Statistics
        entity.setTotalConnections(this.totalConnections);
        entity.setAvgConnectionTimeMin(this.avgConnectionTimeMin);

        // Survey Statistics
        entity.setSurveyCount(this.surveyCount);
        entity.setMaxInclinationDeg(this.maxInclinationDeg);
        entity.setMaxDlsDegPer100ft(this.maxDlsDegPer100ft);

        // Vibration Statistics
        entity.setAvgAxialVibrationG(this.avgAxialVibrationG);
        entity.setAvgLateralVibrationG(this.avgLateralVibrationG);
        entity.setMaxShockG(this.maxShockG);

        // End of Run Summary
        entity.setReasonEnded(this.reasonEnded);
        entity.setBitConditionOut(this.bitConditionOut);

        // Metadata
        entity.setNotes(this.notes);
        entity.setMetadata(this.metadata);

        return entity;
    }
}
