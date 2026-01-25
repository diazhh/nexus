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
@Table(name = "ct_reels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTReel {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "reel_code", unique = true, nullable = false, length = 50)
    private String reelCode;

    @Column(name = "reel_name", nullable = false)
    private String reelName;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "tubing_od_inch", nullable = false, precision = 4, scale = 3)
    private BigDecimal tubingOdInch;

    @Column(name = "tubing_id_inch", nullable = false, precision = 4, scale = 3)
    private BigDecimal tubingIdInch;

    @Column(name = "wall_thickness_inch", nullable = false, precision = 4, scale = 3)
    private BigDecimal wallThicknessInch;

    @Column(name = "total_length_ft", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalLengthFt;

    @Column(name = "material_grade", nullable = false, length = 50)
    private String materialGrade;

    @Column(name = "material_yield_strength_psi")
    private Integer materialYieldStrengthPsi;

    @Column(name = "material_tensile_strength_psi")
    private Integer materialTensileStrengthPsi;

    @Column(name = "youngs_modulus_psi")
    private Long youngsModulusPsi;

    @Column(name = "has_welds")
    private Boolean hasWelds;

    @Column(name = "weld_stress_concentration_factor", precision = 3, scale = 2)
    private BigDecimal weldStressConcentrationFactor;

    @Column(name = "corrosion_environment", length = 50)
    private String corrosionEnvironment;

    @Column(name = "corrosion_factor", precision = 3, scale = 2)
    private BigDecimal corrosionFactor;

    @Column(name = "reel_core_diameter_inch", precision = 6, scale = 2)
    private BigDecimal reelCoreDiameterInch;

    @Column(name = "typical_gooseneck_radius_inch", precision = 6, scale = 2)
    private BigDecimal typicalGooseneckRadiusInch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReelStatus status;

    @Column(name = "current_unit_id")
    private UUID currentUnitId;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "accumulated_fatigue_percent", precision = 5, scale = 2)
    private BigDecimal accumulatedFatiguePercent;

    @Column(name = "total_cycles")
    private Integer totalCycles;

    @Column(name = "estimated_remaining_cycles")
    private Integer estimatedRemainingCycles;

    @Column(name = "fatigue_calculation_method", length = 50)
    private String fatigueCalculationMethod;

    @Column(name = "total_jobs_used")
    private Integer totalJobsUsed;

    @Column(name = "total_meters_deployed", precision = 12, scale = 2)
    private BigDecimal totalMetersDeployed;

    @Column(name = "total_hours_in_use", precision = 10, scale = 2)
    private BigDecimal totalHoursInUse;

    @Column(name = "last_inspection_date")
    private Long lastInspectionDate;

    @Column(name = "last_inspection_type", length = 50)
    private String lastInspectionType;

    @Column(name = "last_inspection_result", length = 50)
    private String lastInspectionResult;

    @Column(name = "next_inspection_due_date")
    private Long nextInspectionDueDate;

    @Column(name = "has_corrosion")
    private Boolean hasCorrosion;

    @Column(name = "has_mechanical_damage")
    private Boolean hasMechanicalDamage;

    @Column(name = "ovality_percent", precision = 4, scale = 2)
    private BigDecimal ovalityPercent;

    @Column(name = "wall_thickness_loss_percent", precision = 4, scale = 2)
    private BigDecimal wallThicknessLossPercent;

    @Column(name = "manufacturing_date")
    private Long manufacturingDate;

    @Column(name = "first_use_date")
    private Long firstUseDate;

    @Column(name = "retirement_date")
    private Long retirementDate;

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
    public String getReelCode() { return reelCode; }
    public String getReelName() { return reelName; }
    public UUID getAssetId() { return assetId; }
    public BigDecimal getTubingOdInch() { return tubingOdInch; }
    public BigDecimal getTubingIdInch() { return tubingIdInch; }
    public BigDecimal getWallThicknessInch() { return wallThicknessInch; }
    public BigDecimal getTotalLengthFt() { return totalLengthFt; }
    public String getMaterialGrade() { return materialGrade; }
    public Integer getMaterialYieldStrengthPsi() { return materialYieldStrengthPsi; }
    public Integer getMaterialTensileStrengthPsi() { return materialTensileStrengthPsi; }
    public Long getYoungsModulusPsi() { return youngsModulusPsi; }
    public Boolean getHasWelds() { return hasWelds; }
    public BigDecimal getWeldStressConcentrationFactor() { return weldStressConcentrationFactor; }
    public String getCorrosionEnvironment() { return corrosionEnvironment; }
    public BigDecimal getCorrosionFactor() { return corrosionFactor; }
    public BigDecimal getReelCoreDiameterInch() { return reelCoreDiameterInch; }
    public BigDecimal getTypicalGooseneckRadiusInch() { return typicalGooseneckRadiusInch; }
    public ReelStatus getStatus() { return status; }
    public UUID getCurrentUnitId() { return currentUnitId; }
    public String getCurrentLocation() { return currentLocation; }
    public BigDecimal getAccumulatedFatiguePercent() { return accumulatedFatiguePercent; }
    public Integer getTotalCycles() { return totalCycles; }
    public Integer getEstimatedRemainingCycles() { return estimatedRemainingCycles; }
    public String getFatigueCalculationMethod() { return fatigueCalculationMethod; }
    public Integer getTotalJobsUsed() { return totalJobsUsed; }
    public BigDecimal getTotalMetersDeployed() { return totalMetersDeployed; }
    public BigDecimal getTotalHoursInUse() { return totalHoursInUse; }
    public Long getLastInspectionDate() { return lastInspectionDate; }
    public String getLastInspectionType() { return lastInspectionType; }
    public String getLastInspectionResult() { return lastInspectionResult; }
    public Long getNextInspectionDueDate() { return nextInspectionDueDate; }
    public Boolean getHasCorrosion() { return hasCorrosion; }
    public Boolean getHasMechanicalDamage() { return hasMechanicalDamage; }
    public BigDecimal getOvalityPercent() { return ovalityPercent; }
    public BigDecimal getWallThicknessLossPercent() { return wallThicknessLossPercent; }
    public Long getManufacturingDate() { return manufacturingDate; }
    public Long getFirstUseDate() { return firstUseDate; }
    public Long getRetirementDate() { return retirementDate; }
    public String getNotes() { return notes; }
    public JsonNode getMetadata() { return metadata; }
    public Long getCreatedTime() { return createdTime; }
    public Long getUpdatedTime() { return updatedTime; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setReelCode(String reelCode) { this.reelCode = reelCode; }
    public void setReelName(String reelName) { this.reelName = reelName; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setTubingOdInch(BigDecimal tubingOdInch) { this.tubingOdInch = tubingOdInch; }
    public void setTubingIdInch(BigDecimal tubingIdInch) { this.tubingIdInch = tubingIdInch; }
    public void setWallThicknessInch(BigDecimal wallThicknessInch) { this.wallThicknessInch = wallThicknessInch; }
    public void setTotalLengthFt(BigDecimal totalLengthFt) { this.totalLengthFt = totalLengthFt; }
    public void setMaterialGrade(String materialGrade) { this.materialGrade = materialGrade; }
    public void setMaterialYieldStrengthPsi(Integer materialYieldStrengthPsi) { this.materialYieldStrengthPsi = materialYieldStrengthPsi; }
    public void setMaterialTensileStrengthPsi(Integer materialTensileStrengthPsi) { this.materialTensileStrengthPsi = materialTensileStrengthPsi; }
    public void setYoungsModulusPsi(Long youngsModulusPsi) { this.youngsModulusPsi = youngsModulusPsi; }
    public void setHasWelds(Boolean hasWelds) { this.hasWelds = hasWelds; }
    public void setWeldStressConcentrationFactor(BigDecimal weldStressConcentrationFactor) { this.weldStressConcentrationFactor = weldStressConcentrationFactor; }
    public void setCorrosionEnvironment(String corrosionEnvironment) { this.corrosionEnvironment = corrosionEnvironment; }
    public void setCorrosionFactor(BigDecimal corrosionFactor) { this.corrosionFactor = corrosionFactor; }
    public void setReelCoreDiameterInch(BigDecimal reelCoreDiameterInch) { this.reelCoreDiameterInch = reelCoreDiameterInch; }
    public void setTypicalGooseneckRadiusInch(BigDecimal typicalGooseneckRadiusInch) { this.typicalGooseneckRadiusInch = typicalGooseneckRadiusInch; }
    public void setStatus(ReelStatus status) { this.status = status; }
    public void setCurrentUnitId(UUID currentUnitId) { this.currentUnitId = currentUnitId; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public void setAccumulatedFatiguePercent(BigDecimal accumulatedFatiguePercent) { this.accumulatedFatiguePercent = accumulatedFatiguePercent; }
    public void setTotalCycles(Integer totalCycles) { this.totalCycles = totalCycles; }
    public void setEstimatedRemainingCycles(Integer estimatedRemainingCycles) { this.estimatedRemainingCycles = estimatedRemainingCycles; }
    public void setFatigueCalculationMethod(String fatigueCalculationMethod) { this.fatigueCalculationMethod = fatigueCalculationMethod; }
    public void setTotalJobsUsed(Integer totalJobsUsed) { this.totalJobsUsed = totalJobsUsed; }
    public void setTotalMetersDeployed(BigDecimal totalMetersDeployed) { this.totalMetersDeployed = totalMetersDeployed; }
    public void setTotalHoursInUse(BigDecimal totalHoursInUse) { this.totalHoursInUse = totalHoursInUse; }
    public void setLastInspectionDate(Long lastInspectionDate) { this.lastInspectionDate = lastInspectionDate; }
    public void setLastInspectionType(String lastInspectionType) { this.lastInspectionType = lastInspectionType; }
    public void setLastInspectionResult(String lastInspectionResult) { this.lastInspectionResult = lastInspectionResult; }
    public void setNextInspectionDueDate(Long nextInspectionDueDate) { this.nextInspectionDueDate = nextInspectionDueDate; }
    public void setHasCorrosion(Boolean hasCorrosion) { this.hasCorrosion = hasCorrosion; }
    public void setHasMechanicalDamage(Boolean hasMechanicalDamage) { this.hasMechanicalDamage = hasMechanicalDamage; }
    public void setOvalityPercent(BigDecimal ovalityPercent) { this.ovalityPercent = ovalityPercent; }
    public void setWallThicknessLossPercent(BigDecimal wallThicknessLossPercent) { this.wallThicknessLossPercent = wallThicknessLossPercent; }
    public void setManufacturingDate(Long manufacturingDate) { this.manufacturingDate = manufacturingDate; }
    public void setFirstUseDate(Long firstUseDate) { this.firstUseDate = firstUseDate; }
    public void setRetirementDate(Long retirementDate) { this.retirementDate = retirementDate; }
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
        if (status == null) {
            status = ReelStatus.AVAILABLE;
        }
        if (hasWelds == null) {
            hasWelds = false;
        }
        if (weldStressConcentrationFactor == null) {
            weldStressConcentrationFactor = new BigDecimal("1.00");
        }
        if (corrosionEnvironment == null) {
            corrosionEnvironment = "NONE";
        }
        if (corrosionFactor == null) {
            corrosionFactor = new BigDecimal("1.00");
        }
        if (accumulatedFatiguePercent == null) {
            accumulatedFatiguePercent = BigDecimal.ZERO;
        }
        if (totalCycles == null) {
            totalCycles = 0;
        }
        if (fatigueCalculationMethod == null) {
            fatigueCalculationMethod = "PALMGREN_MINER";
        }
        if (totalJobsUsed == null) {
            totalJobsUsed = 0;
        }
        if (totalMetersDeployed == null) {
            totalMetersDeployed = BigDecimal.ZERO;
        }
        if (totalHoursInUse == null) {
            totalHoursInUse = BigDecimal.ZERO;
        }
        if (hasCorrosion == null) {
            hasCorrosion = false;
        }
        if (hasMechanicalDamage == null) {
            hasMechanicalDamage = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
