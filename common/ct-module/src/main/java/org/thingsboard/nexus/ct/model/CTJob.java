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
@Table(name = "ct_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTJob {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "job_number", unique = true, nullable = false, length = 50)
    private String jobNumber;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "job_type", nullable = false, length = 100)
    private String jobType;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "well_id")
    private UUID wellId;

    @Column(name = "well_name", nullable = false)
    private String wellName;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "well_depth_md_ft", precision = 10, scale = 2)
    private BigDecimal wellDepthMdFt;

    @Column(name = "well_depth_tvd_ft", precision = 10, scale = 2)
    private BigDecimal wellDepthTvdFt;

    @Column(name = "target_depth_from_ft", precision = 10, scale = 2)
    private BigDecimal targetDepthFromFt;

    @Column(name = "target_depth_to_ft", precision = 10, scale = 2)
    private BigDecimal targetDepthToFt;

    @Column(name = "wellhead_pressure_psi")
    private Integer wellheadPressurePsi;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "reel_id", nullable = false)
    private UUID reelId;

    @Column(name = "bha_configuration_id")
    private UUID bhaConfigurationId;

    @Column(name = "operator_user_id")
    private UUID operatorUserId;

    @Column(name = "supervisor_user_id")
    private UUID supervisorUserId;

    @Column(name = "engineer_user_id")
    private UUID engineerUserId;

    @Column(name = "planned_start_date")
    private Long plannedStartDate;

    @Column(name = "planned_end_date")
    private Long plannedEndDate;

    @Column(name = "estimated_duration_hours", precision = 6, scale = 2)
    private BigDecimal estimatedDurationHours;

    @Column(name = "actual_start_date")
    private Long actualStartDate;

    @Column(name = "actual_end_date")
    private Long actualEndDate;

    @Column(name = "actual_duration_hours", precision = 6, scale = 2)
    private BigDecimal actualDurationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private JobStatus status;

    @Column(name = "status_reason", columnDefinition = "TEXT")
    private String statusReason;

    @Column(name = "current_phase", length = 100)
    private String currentPhase;

    @Column(name = "max_planned_pressure_psi")
    private Integer maxPlannedPressurePsi;

    @Column(name = "max_planned_tension_lbf")
    private Integer maxPlannedTensionLbf;

    @Column(name = "max_planned_speed_ft_min")
    private Integer maxPlannedSpeedFtMin;

    @Column(name = "planned_pump_rate_bpm", precision = 6, scale = 2)
    private BigDecimal plannedPumpRateBpm;

    @Column(name = "max_actual_pressure_psi")
    private Integer maxActualPressurePsi;

    @Column(name = "max_actual_tension_lbf")
    private Integer maxActualTensionLbf;

    @Column(name = "max_actual_speed_ft_min")
    private Integer maxActualSpeedFtMin;

    @Column(name = "max_actual_depth_ft", precision = 10, scale = 2)
    private BigDecimal maxActualDepthFt;

    @Type(JsonBinaryType.class)
    @Column(name = "chemicals_used", columnDefinition = "jsonb")
    private JsonNode chemicalsUsed;

    @Column(name = "total_fluid_pumped_bbl", precision = 10, scale = 2)
    private BigDecimal totalFluidPumpedBbl;

    @Column(name = "nitrogen_volume_scf", precision = 12, scale = 2)
    private BigDecimal nitrogenVolumeScf;

    @Column(name = "job_success")
    private Boolean jobSuccess;

    @Column(name = "objectives_achieved")
    private Boolean objectivesAchieved;

    @Column(name = "meters_deployed", precision = 10, scale = 2)
    private BigDecimal metersDeployed;

    @Column(name = "cycles_performed")
    private Integer cyclesPerformed;

    @Column(name = "productive_time_hours", precision = 6, scale = 2)
    private BigDecimal productiveTimeHours;

    @Column(name = "non_productive_time_hours", precision = 6, scale = 2)
    private BigDecimal nonProductiveTimeHours;

    @Column(name = "rigging_time_hours", precision = 6, scale = 2)
    private BigDecimal riggingTimeHours;

    @Type(JsonBinaryType.class)
    @Column(name = "npt_breakdown", columnDefinition = "jsonb")
    private JsonNode nptBreakdown;

    @Column(name = "estimated_cost_usd", precision = 12, scale = 2)
    private BigDecimal estimatedCostUsd;

    @Column(name = "actual_cost_usd", precision = 12, scale = 2)
    private BigDecimal actualCostUsd;

    @Column(name = "incidents_count")
    private Integer incidentsCount;

    @Column(name = "has_stuck_pipe")
    private Boolean hasStuckPipe;

    @Column(name = "has_hse_incident")
    private Boolean hasHseIncident;

    @Column(name = "job_report_generated")
    private Boolean jobReportGenerated;

    @Column(name = "job_report_path", length = 500)
    private String jobReportPath;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

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

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_time")
    private Long approvedTime;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getJobNumber() { return jobNumber; }
    public String getJobName() { return jobName; }
    public String getJobType() { return jobType; }
    public String getPriority() { return priority; }
    public UUID getWellId() { return wellId; }
    public String getWellName() { return wellName; }
    public String getFieldName() { return fieldName; }
    public String getClientName() { return clientName; }
    public BigDecimal getWellDepthMdFt() { return wellDepthMdFt; }
    public BigDecimal getWellDepthTvdFt() { return wellDepthTvdFt; }
    public BigDecimal getTargetDepthFromFt() { return targetDepthFromFt; }
    public BigDecimal getTargetDepthToFt() { return targetDepthToFt; }
    public Integer getWellheadPressurePsi() { return wellheadPressurePsi; }
    public UUID getUnitId() { return unitId; }
    public UUID getReelId() { return reelId; }
    public UUID getBhaConfigurationId() { return bhaConfigurationId; }
    public UUID getOperatorUserId() { return operatorUserId; }
    public UUID getSupervisorUserId() { return supervisorUserId; }
    public UUID getEngineerUserId() { return engineerUserId; }
    public Long getPlannedStartDate() { return plannedStartDate; }
    public Long getPlannedEndDate() { return plannedEndDate; }
    public BigDecimal getEstimatedDurationHours() { return estimatedDurationHours; }
    public Long getActualStartDate() { return actualStartDate; }
    public Long getActualEndDate() { return actualEndDate; }
    public BigDecimal getActualDurationHours() { return actualDurationHours; }
    public JobStatus getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
    public String getCurrentPhase() { return currentPhase; }
    public Integer getMaxPlannedPressurePsi() { return maxPlannedPressurePsi; }
    public Integer getMaxPlannedTensionLbf() { return maxPlannedTensionLbf; }
    public Integer getMaxPlannedSpeedFtMin() { return maxPlannedSpeedFtMin; }
    public BigDecimal getPlannedPumpRateBpm() { return plannedPumpRateBpm; }
    public Integer getMaxActualPressurePsi() { return maxActualPressurePsi; }
    public Integer getMaxActualTensionLbf() { return maxActualTensionLbf; }
    public Integer getMaxActualSpeedFtMin() { return maxActualSpeedFtMin; }
    public BigDecimal getMaxActualDepthFt() { return maxActualDepthFt; }
    public JsonNode getChemicalsUsed() { return chemicalsUsed; }
    public BigDecimal getTotalFluidPumpedBbl() { return totalFluidPumpedBbl; }
    public BigDecimal getNitrogenVolumeScf() { return nitrogenVolumeScf; }
    public Boolean getJobSuccess() { return jobSuccess; }
    public Boolean getObjectivesAchieved() { return objectivesAchieved; }
    public BigDecimal getMetersDeployed() { return metersDeployed; }
    public Integer getCyclesPerformed() { return cyclesPerformed; }
    public BigDecimal getProductiveTimeHours() { return productiveTimeHours; }
    public BigDecimal getNonProductiveTimeHours() { return nonProductiveTimeHours; }
    public BigDecimal getRiggingTimeHours() { return riggingTimeHours; }
    public JsonNode getNptBreakdown() { return nptBreakdown; }
    public BigDecimal getEstimatedCostUsd() { return estimatedCostUsd; }
    public BigDecimal getActualCostUsd() { return actualCostUsd; }
    public Integer getIncidentsCount() { return incidentsCount; }
    public Boolean getHasStuckPipe() { return hasStuckPipe; }
    public Boolean getHasHseIncident() { return hasHseIncident; }
    public Boolean getJobReportGenerated() { return jobReportGenerated; }
    public String getJobReportPath() { return jobReportPath; }
    public String getDescription() { return description; }
    public String getNotes() { return notes; }
    public String getLessonsLearned() { return lessonsLearned; }
    public JsonNode getMetadata() { return metadata; }
    public UUID getCreatedBy() { return createdBy; }
    public Long getCreatedTime() { return createdTime; }
    public UUID getUpdatedBy() { return updatedBy; }
    public Long getUpdatedTime() { return updatedTime; }
    public UUID getApprovedBy() { return approvedBy; }
    public Long getApprovedTime() { return approvedTime; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setJobNumber(String jobNumber) { this.jobNumber = jobNumber; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setWellId(UUID wellId) { this.wellId = wellId; }
    public void setWellName(String wellName) { this.wellName = wellName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setWellDepthMdFt(BigDecimal wellDepthMdFt) { this.wellDepthMdFt = wellDepthMdFt; }
    public void setWellDepthTvdFt(BigDecimal wellDepthTvdFt) { this.wellDepthTvdFt = wellDepthTvdFt; }
    public void setTargetDepthFromFt(BigDecimal targetDepthFromFt) { this.targetDepthFromFt = targetDepthFromFt; }
    public void setTargetDepthToFt(BigDecimal targetDepthToFt) { this.targetDepthToFt = targetDepthToFt; }
    public void setWellheadPressurePsi(Integer wellheadPressurePsi) { this.wellheadPressurePsi = wellheadPressurePsi; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public void setReelId(UUID reelId) { this.reelId = reelId; }
    public void setBhaConfigurationId(UUID bhaConfigurationId) { this.bhaConfigurationId = bhaConfigurationId; }
    public void setOperatorUserId(UUID operatorUserId) { this.operatorUserId = operatorUserId; }
    public void setSupervisorUserId(UUID supervisorUserId) { this.supervisorUserId = supervisorUserId; }
    public void setEngineerUserId(UUID engineerUserId) { this.engineerUserId = engineerUserId; }
    public void setPlannedStartDate(Long plannedStartDate) { this.plannedStartDate = plannedStartDate; }
    public void setPlannedEndDate(Long plannedEndDate) { this.plannedEndDate = plannedEndDate; }
    public void setEstimatedDurationHours(BigDecimal estimatedDurationHours) { this.estimatedDurationHours = estimatedDurationHours; }
    public void setActualStartDate(Long actualStartDate) { this.actualStartDate = actualStartDate; }
    public void setActualEndDate(Long actualEndDate) { this.actualEndDate = actualEndDate; }
    public void setActualDurationHours(BigDecimal actualDurationHours) { this.actualDurationHours = actualDurationHours; }
    public void setStatus(JobStatus status) { this.status = status; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }
    public void setMaxPlannedPressurePsi(Integer maxPlannedPressurePsi) { this.maxPlannedPressurePsi = maxPlannedPressurePsi; }
    public void setMaxPlannedTensionLbf(Integer maxPlannedTensionLbf) { this.maxPlannedTensionLbf = maxPlannedTensionLbf; }
    public void setMaxPlannedSpeedFtMin(Integer maxPlannedSpeedFtMin) { this.maxPlannedSpeedFtMin = maxPlannedSpeedFtMin; }
    public void setPlannedPumpRateBpm(BigDecimal plannedPumpRateBpm) { this.plannedPumpRateBpm = plannedPumpRateBpm; }
    public void setMaxActualPressurePsi(Integer maxActualPressurePsi) { this.maxActualPressurePsi = maxActualPressurePsi; }
    public void setMaxActualTensionLbf(Integer maxActualTensionLbf) { this.maxActualTensionLbf = maxActualTensionLbf; }
    public void setMaxActualSpeedFtMin(Integer maxActualSpeedFtMin) { this.maxActualSpeedFtMin = maxActualSpeedFtMin; }
    public void setMaxActualDepthFt(BigDecimal maxActualDepthFt) { this.maxActualDepthFt = maxActualDepthFt; }
    public void setChemicalsUsed(JsonNode chemicalsUsed) { this.chemicalsUsed = chemicalsUsed; }
    public void setTotalFluidPumpedBbl(BigDecimal totalFluidPumpedBbl) { this.totalFluidPumpedBbl = totalFluidPumpedBbl; }
    public void setNitrogenVolumeScf(BigDecimal nitrogenVolumeScf) { this.nitrogenVolumeScf = nitrogenVolumeScf; }
    public void setJobSuccess(Boolean jobSuccess) { this.jobSuccess = jobSuccess; }
    public void setObjectivesAchieved(Boolean objectivesAchieved) { this.objectivesAchieved = objectivesAchieved; }
    public void setMetersDeployed(BigDecimal metersDeployed) { this.metersDeployed = metersDeployed; }
    public void setCyclesPerformed(Integer cyclesPerformed) { this.cyclesPerformed = cyclesPerformed; }
    public void setProductiveTimeHours(BigDecimal productiveTimeHours) { this.productiveTimeHours = productiveTimeHours; }
    public void setNonProductiveTimeHours(BigDecimal nonProductiveTimeHours) { this.nonProductiveTimeHours = nonProductiveTimeHours; }
    public void setRiggingTimeHours(BigDecimal riggingTimeHours) { this.riggingTimeHours = riggingTimeHours; }
    public void setNptBreakdown(JsonNode nptBreakdown) { this.nptBreakdown = nptBreakdown; }
    public void setEstimatedCostUsd(BigDecimal estimatedCostUsd) { this.estimatedCostUsd = estimatedCostUsd; }
    public void setActualCostUsd(BigDecimal actualCostUsd) { this.actualCostUsd = actualCostUsd; }
    public void setIncidentsCount(Integer incidentsCount) { this.incidentsCount = incidentsCount; }
    public void setHasStuckPipe(Boolean hasStuckPipe) { this.hasStuckPipe = hasStuckPipe; }
    public void setHasHseIncident(Boolean hasHseIncident) { this.hasHseIncident = hasHseIncident; }
    public void setJobReportGenerated(Boolean jobReportGenerated) { this.jobReportGenerated = jobReportGenerated; }
    public void setJobReportPath(String jobReportPath) { this.jobReportPath = jobReportPath; }
    public void setDescription(String description) { this.description = description; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setLessonsLearned(String lessonsLearned) { this.lessonsLearned = lessonsLearned; }
    public void setMetadata(JsonNode metadata) { this.metadata = metadata; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setCreatedTime(Long createdTime) { this.createdTime = createdTime; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedTime(Long updatedTime) { this.updatedTime = updatedTime; }
    public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }
    public void setApprovedTime(Long approvedTime) { this.approvedTime = approvedTime; }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdTime == null) {
            createdTime = System.currentTimeMillis();
        }
        if (status == null) {
            status = JobStatus.PLANNED;
        }
        if (priority == null) {
            priority = "MEDIUM";
        }
        if (incidentsCount == null) {
            incidentsCount = 0;
        }
        if (hasStuckPipe == null) {
            hasStuckPipe = false;
        }
        if (hasHseIncident == null) {
            hasHseIncident = false;
        }
        if (jobReportGenerated == null) {
            jobReportGenerated = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
