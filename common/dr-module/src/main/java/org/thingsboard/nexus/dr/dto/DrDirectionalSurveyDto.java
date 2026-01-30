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
import org.thingsboard.nexus.dr.model.DrDirectionalSurvey;
import org.thingsboard.nexus.dr.model.enums.SurveyType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Directional Survey data transfer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrDirectionalSurveyDto {

    private UUID id;
    private UUID tenantId;
    private UUID runId;
    private UUID wellId;

    // Enriched fields
    private String wellName;
    private Integer runNumber;

    // --- Measured Data ---
    private BigDecimal mdFt;
    private BigDecimal inclinationDeg;
    private BigDecimal azimuthDeg;
    private BigDecimal toolfaceDeg;

    // --- Calculated Values ---
    private BigDecimal tvdFt;
    private BigDecimal northFt;
    private BigDecimal eastFt;
    private BigDecimal verticalSectionFt;
    private BigDecimal dlsDegPer100ft;
    private BigDecimal closureDistanceFt;
    private BigDecimal closureAzimuthDeg;

    // --- Survey Metadata ---
    private SurveyType surveyType;
    private Boolean isDefinitive;
    private String surveyQuality;
    private Long surveyTime;

    // --- Magnetic Data ---
    private BigDecimal magneticFieldStrength;
    private BigDecimal magneticDipAngleDeg;
    private BigDecimal gravityFieldStrength;

    // --- Temperature and Corrections ---
    private BigDecimal boreholeTempF;
    private Boolean sagCorrectionApplied;
    private Boolean magneticCorrectionApplied;

    // --- Error Model ---
    private BigDecimal northUncertaintyFt;
    private BigDecimal eastUncertaintyFt;
    private BigDecimal tvdUncertaintyFt;

    // --- Raw Data ---
    private JsonNode rawData;

    // --- Metadata ---
    private String notes;
    private UUID createdBy;
    private Long createdTime;

    /**
     * Convert entity to DTO.
     */
    public static DrDirectionalSurveyDto fromEntity(DrDirectionalSurvey entity) {
        if (entity == null) {
            return null;
        }

        return DrDirectionalSurveyDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .runId(entity.getRunId())
                .wellId(entity.getWellId())
                // Measured Data
                .mdFt(entity.getMdFt())
                .inclinationDeg(entity.getInclinationDeg())
                .azimuthDeg(entity.getAzimuthDeg())
                .toolfaceDeg(entity.getToolfaceDeg())
                // Calculated Values
                .tvdFt(entity.getTvdFt())
                .northFt(entity.getNorthFt())
                .eastFt(entity.getEastFt())
                .verticalSectionFt(entity.getVerticalSectionFt())
                .dlsDegPer100ft(entity.getDlsDegPer100ft())
                .closureDistanceFt(entity.getClosureDistanceFt())
                .closureAzimuthDeg(entity.getClosureAzimuthDeg())
                // Survey Metadata
                .surveyType(entity.getSurveyType())
                .isDefinitive(entity.getIsDefinitive())
                .surveyQuality(entity.getSurveyQuality())
                .surveyTime(entity.getSurveyTime())
                // Magnetic Data
                .magneticFieldStrength(entity.getMagneticFieldStrength())
                .magneticDipAngleDeg(entity.getMagneticDipAngleDeg())
                .gravityFieldStrength(entity.getGravityFieldStrength())
                // Temperature and Corrections
                .boreholeTempF(entity.getBoreholeTempF())
                .sagCorrectionApplied(entity.getSagCorrectionApplied())
                .magneticCorrectionApplied(entity.getMagneticCorrectionApplied())
                // Error Model
                .northUncertaintyFt(entity.getNorthUncertaintyFt())
                .eastUncertaintyFt(entity.getEastUncertaintyFt())
                .tvdUncertaintyFt(entity.getTvdUncertaintyFt())
                // Raw Data
                .rawData(entity.getRawData())
                // Metadata
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .createdTime(entity.getCreatedTime())
                .build();
    }

    /**
     * Convert DTO to entity.
     */
    public DrDirectionalSurvey toEntity() {
        return DrDirectionalSurvey.builder()
                .id(this.id)
                .tenantId(this.tenantId)
                .runId(this.runId)
                .wellId(this.wellId)
                // Measured Data
                .mdFt(this.mdFt)
                .inclinationDeg(this.inclinationDeg)
                .azimuthDeg(this.azimuthDeg)
                .toolfaceDeg(this.toolfaceDeg)
                // Calculated Values
                .tvdFt(this.tvdFt)
                .northFt(this.northFt)
                .eastFt(this.eastFt)
                .verticalSectionFt(this.verticalSectionFt)
                .dlsDegPer100ft(this.dlsDegPer100ft)
                .closureDistanceFt(this.closureDistanceFt)
                .closureAzimuthDeg(this.closureAzimuthDeg)
                // Survey Metadata
                .surveyType(this.surveyType)
                .isDefinitive(this.isDefinitive)
                .surveyQuality(this.surveyQuality)
                .surveyTime(this.surveyTime)
                // Magnetic Data
                .magneticFieldStrength(this.magneticFieldStrength)
                .magneticDipAngleDeg(this.magneticDipAngleDeg)
                .gravityFieldStrength(this.gravityFieldStrength)
                // Temperature and Corrections
                .boreholeTempF(this.boreholeTempF)
                .sagCorrectionApplied(this.sagCorrectionApplied)
                .magneticCorrectionApplied(this.magneticCorrectionApplied)
                // Error Model
                .northUncertaintyFt(this.northUncertaintyFt)
                .eastUncertaintyFt(this.eastUncertaintyFt)
                .tvdUncertaintyFt(this.tvdUncertaintyFt)
                // Raw Data
                .rawData(this.rawData)
                // Metadata
                .notes(this.notes)
                .createdBy(this.createdBy)
                .createdTime(this.createdTime)
                .build();
    }
}
