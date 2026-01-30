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
import org.thingsboard.nexus.dr.model.DrMudLog;
import org.thingsboard.nexus.dr.model.enums.LithologyType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Mud Log data transfer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrMudLogDto {

    private UUID id;
    private UUID tenantId;
    private UUID runId;
    private UUID wellId;

    // Enriched fields
    private String wellName;
    private Integer runNumber;
    private String formationDisplayName;

    // --- Depth Information ---
    private BigDecimal mdFt;
    private BigDecimal mdTopFt;
    private BigDecimal mdBottomFt;
    private BigDecimal tvdFt;

    // --- Lithology Information ---
    private LithologyType primaryLithology;
    private LithologyType secondaryLithology;
    private BigDecimal primaryLithologyPercent;
    private String lithologyDescription;
    private String color;
    private String grainSize;
    private String hardness;
    private String porosityType;
    private BigDecimal porosityPercent;
    private String cementType;
    private String sorting;
    private String roundness;

    // --- Gas and Show Information ---
    private BigDecimal totalGasUnits;
    private BigDecimal backgroundGasUnits;
    private BigDecimal connectionGasUnits;
    private BigDecimal tripGasUnits;
    private BigDecimal c1Percent;
    private BigDecimal c2Percent;
    private BigDecimal c3Percent;
    private BigDecimal ic4Percent;
    private BigDecimal nc4Percent;
    private BigDecimal ic5Percent;
    private BigDecimal nc5Percent;

    // Calculated gas ratios
    private BigDecimal wetnessRatio;
    private BigDecimal balanceRatio;
    private BigDecimal characterRatio;

    // --- Hydrocarbon Shows ---
    private String oilShowType;
    private String oilShowIntensity;
    private String fluorescenceColor;
    private BigDecimal fluorescencePercent;
    private String cutDescription;
    private String stainDescription;

    // --- Drilling Parameters at this depth ---
    private BigDecimal ropFtHr;
    private BigDecimal wobKlbs;
    private BigDecimal rpm;
    private BigDecimal torqueFtLbs;
    private BigDecimal pumpPressurePsi;
    private BigDecimal flowRateGpm;

    // --- Mud Properties ---
    private BigDecimal mudWeightPpg;
    private BigDecimal mudWeightOutPpg;
    private BigDecimal mudViscosity;
    private BigDecimal mudTempInF;
    private BigDecimal mudTempOutF;
    private BigDecimal chloridesPpm;

    // --- Sample Information ---
    private String sampleType;
    private String sampleNumber;
    private BigDecimal lagTimeMinutes;
    private Long sampleTime;

    // --- Formation Information ---
    private String formationName;
    private BigDecimal formationTopFt;
    private String geologicalAge;

    // --- Raw Data ---
    private JsonNode rawData;

    // --- Metadata ---
    private String loggedBy;
    private String notes;
    private UUID createdBy;
    private Long createdTime;
    private Long updatedTime;

    /**
     * Convert entity to DTO.
     */
    public static DrMudLogDto fromEntity(DrMudLog entity) {
        if (entity == null) {
            return null;
        }

        DrMudLogDto dto = DrMudLogDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .runId(entity.getRunId())
                .wellId(entity.getWellId())
                // Depth Information
                .mdFt(entity.getMdFt())
                .mdTopFt(entity.getMdTopFt())
                .mdBottomFt(entity.getMdBottomFt())
                .tvdFt(entity.getTvdFt())
                // Lithology
                .primaryLithology(entity.getPrimaryLithology())
                .secondaryLithology(entity.getSecondaryLithology())
                .primaryLithologyPercent(entity.getPrimaryLithologyPercent())
                .lithologyDescription(entity.getLithologyDescription())
                .color(entity.getColor())
                .grainSize(entity.getGrainSize())
                .hardness(entity.getHardness())
                .porosityType(entity.getPorosityType())
                .porosityPercent(entity.getPorosityPercent())
                .cementType(entity.getCite())
                .sorting(entity.getSorting())
                .roundness(entity.getRoundness())
                // Gas Data
                .totalGasUnits(entity.getTotalGasUnits())
                .backgroundGasUnits(entity.getBackgroundGasUnits())
                .connectionGasUnits(entity.getConnectionGasUnits())
                .tripGasUnits(entity.getTripGasUnits())
                .c1Percent(entity.getC1Percent())
                .c2Percent(entity.getC2Percent())
                .c3Percent(entity.getC3Percent())
                .ic4Percent(entity.getIc4Percent())
                .nc4Percent(entity.getNc4Percent())
                .ic5Percent(entity.getIc5Percent())
                .nc5Percent(entity.getNc5Percent())
                // Hydrocarbon Shows
                .oilShowType(entity.getOilShowType())
                .oilShowIntensity(entity.getOilShowIntensity())
                .fluorescenceColor(entity.getFluorescenceColor())
                .fluorescencePercent(entity.getFluorescencePercent())
                .cutDescription(entity.getCutDescription())
                .stainDescription(entity.getStainDescription())
                // Drilling Parameters
                .ropFtHr(entity.getRopFtHr())
                .wobKlbs(entity.getWobKlbs())
                .rpm(entity.getRpm())
                .torqueFtLbs(entity.getTorqueFtLbs())
                .pumpPressurePsi(entity.getPumpPressurePsi())
                .flowRateGpm(entity.getFlowRateGpm())
                // Mud Properties
                .mudWeightPpg(entity.getMudWeightPpg())
                .mudWeightOutPpg(entity.getMudWeightOutPpg())
                .mudViscosity(entity.getMudViscosity())
                .mudTempInF(entity.getMudTempInF())
                .mudTempOutF(entity.getMudTempOutF())
                .chloridesPpm(entity.getChloridesPpm())
                // Sample Info
                .sampleType(entity.getSampleType())
                .sampleNumber(entity.getSampleNumber())
                .lagTimeMinutes(entity.getLagTimeMinutes())
                .sampleTime(entity.getSampleTime())
                // Formation
                .formationName(entity.getFormationName())
                .formationTopFt(entity.getFormationTopFt())
                .geologicalAge(entity.getGeologicalAge())
                // Raw Data
                .rawData(entity.getRawData())
                // Metadata
                .loggedBy(entity.getLoggedBy())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .createdTime(entity.getCreatedTime())
                .updatedTime(entity.getUpdatedTime())
                .build();

        // Calculate gas ratios
        dto.calculateGasRatios();

        return dto;
    }

    /**
     * Calculate gas ratios for interpretation.
     */
    private void calculateGasRatios() {
        // Wetness Ratio = (C2 + C3 + C4 + C5) / C1 * 100
        if (c1Percent != null && c1Percent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heavies = BigDecimal.ZERO;
            if (c2Percent != null) heavies = heavies.add(c2Percent);
            if (c3Percent != null) heavies = heavies.add(c3Percent);
            if (ic4Percent != null) heavies = heavies.add(ic4Percent);
            if (nc4Percent != null) heavies = heavies.add(nc4Percent);
            if (ic5Percent != null) heavies = heavies.add(ic5Percent);
            if (nc5Percent != null) heavies = heavies.add(nc5Percent);

            this.wetnessRatio = heavies.multiply(BigDecimal.valueOf(100))
                    .divide(c1Percent, 2, java.math.RoundingMode.HALF_UP);
        }

        // Balance Ratio = (C1 + C2) / (C3 + C4 + C5)
        if (c3Percent != null || ic4Percent != null || nc4Percent != null ||
                ic5Percent != null || nc5Percent != null) {
            BigDecimal light = BigDecimal.ZERO;
            BigDecimal heavy = BigDecimal.ZERO;

            if (c1Percent != null) light = light.add(c1Percent);
            if (c2Percent != null) light = light.add(c2Percent);
            if (c3Percent != null) heavy = heavy.add(c3Percent);
            if (ic4Percent != null) heavy = heavy.add(ic4Percent);
            if (nc4Percent != null) heavy = heavy.add(nc4Percent);
            if (ic5Percent != null) heavy = heavy.add(ic5Percent);
            if (nc5Percent != null) heavy = heavy.add(nc5Percent);

            if (heavy.compareTo(BigDecimal.ZERO) > 0) {
                this.balanceRatio = light.divide(heavy, 2, java.math.RoundingMode.HALF_UP);
            }
        }

        // Character Ratio = C3 / (C4 + C5)
        if (c3Percent != null && c3Percent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal c4c5 = BigDecimal.ZERO;
            if (ic4Percent != null) c4c5 = c4c5.add(ic4Percent);
            if (nc4Percent != null) c4c5 = c4c5.add(nc4Percent);
            if (ic5Percent != null) c4c5 = c4c5.add(ic5Percent);
            if (nc5Percent != null) c4c5 = c4c5.add(nc5Percent);

            if (c4c5.compareTo(BigDecimal.ZERO) > 0) {
                this.characterRatio = c3Percent.divide(c4c5, 2, java.math.RoundingMode.HALF_UP);
            }
        }
    }

    /**
     * Convert DTO to entity.
     */
    public DrMudLog toEntity() {
        return DrMudLog.builder()
                .id(this.id)
                .tenantId(this.tenantId)
                .runId(this.runId)
                .wellId(this.wellId)
                // Depth Information
                .mdFt(this.mdFt)
                .mdTopFt(this.mdTopFt)
                .mdBottomFt(this.mdBottomFt)
                .tvdFt(this.tvdFt)
                // Lithology
                .primaryLithology(this.primaryLithology)
                .secondaryLithology(this.secondaryLithology)
                .primaryLithologyPercent(this.primaryLithologyPercent)
                .lithologyDescription(this.lithologyDescription)
                .color(this.color)
                .grainSize(this.grainSize)
                .hardness(this.hardness)
                .porosityType(this.porosityType)
                .porosityPercent(this.porosityPercent)
                .cite(this.cementType)
                .sorting(this.sorting)
                .roundness(this.roundness)
                // Gas Data
                .totalGasUnits(this.totalGasUnits)
                .backgroundGasUnits(this.backgroundGasUnits)
                .connectionGasUnits(this.connectionGasUnits)
                .tripGasUnits(this.tripGasUnits)
                .c1Percent(this.c1Percent)
                .c2Percent(this.c2Percent)
                .c3Percent(this.c3Percent)
                .ic4Percent(this.ic4Percent)
                .nc4Percent(this.nc4Percent)
                .ic5Percent(this.ic5Percent)
                .nc5Percent(this.nc5Percent)
                // Hydrocarbon Shows
                .oilShowType(this.oilShowType)
                .oilShowIntensity(this.oilShowIntensity)
                .fluorescenceColor(this.fluorescenceColor)
                .fluorescencePercent(this.fluorescencePercent)
                .cutDescription(this.cutDescription)
                .stainDescription(this.stainDescription)
                // Drilling Parameters
                .ropFtHr(this.ropFtHr)
                .wobKlbs(this.wobKlbs)
                .rpm(this.rpm)
                .torqueFtLbs(this.torqueFtLbs)
                .pumpPressurePsi(this.pumpPressurePsi)
                .flowRateGpm(this.flowRateGpm)
                // Mud Properties
                .mudWeightPpg(this.mudWeightPpg)
                .mudWeightOutPpg(this.mudWeightOutPpg)
                .mudViscosity(this.mudViscosity)
                .mudTempInF(this.mudTempInF)
                .mudTempOutF(this.mudTempOutF)
                .chloridesPpm(this.chloridesPpm)
                // Sample Info
                .sampleType(this.sampleType)
                .sampleNumber(this.sampleNumber)
                .lagTimeMinutes(this.lagTimeMinutes)
                .sampleTime(this.sampleTime)
                // Formation
                .formationName(this.formationName)
                .formationTopFt(this.formationTopFt)
                .geologicalAge(this.geologicalAge)
                // Raw Data
                .rawData(this.rawData)
                // Metadata
                .loggedBy(this.loggedBy)
                .notes(this.notes)
                .createdBy(this.createdBy)
                .createdTime(this.createdTime)
                .updatedTime(this.updatedTime)
                .build();
    }
}
