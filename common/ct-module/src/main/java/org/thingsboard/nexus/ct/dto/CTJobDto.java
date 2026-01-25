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
import org.thingsboard.nexus.ct.model.CTJob;
import org.thingsboard.nexus.ct.model.JobStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTJobDto {

    private UUID id;
    private UUID tenantId;
    private String jobNumber;
    private String jobName;
    private String jobType;
    private String priority;

    private UUID wellId;
    private String wellName;
    private String fieldName;
    private String clientName;

    private BigDecimal wellDepthMdFt;
    private BigDecimal wellDepthTvdFt;
    private BigDecimal targetDepthFromFt;
    private BigDecimal targetDepthToFt;
    private Integer wellheadPressurePsi;

    private UUID unitId;
    private String unitCode;
    private UUID reelId;
    private String reelCode;
    private UUID bhaConfigurationId;

    private UUID operatorUserId;
    private UUID supervisorUserId;
    private UUID engineerUserId;

    private Long plannedStartDate;
    private Long plannedEndDate;
    private BigDecimal estimatedDurationHours;

    private Long actualStartDate;
    private Long actualEndDate;
    private BigDecimal actualDurationHours;

    private JobStatus status;
    private String statusReason;
    private String currentPhase;

    private Integer maxPlannedPressurePsi;
    private Integer maxPlannedTensionLbf;
    private Integer maxPlannedSpeedFtMin;
    private BigDecimal plannedPumpRateBpm;

    private Integer maxActualPressurePsi;
    private Integer maxActualTensionLbf;
    private Integer maxActualSpeedFtMin;
    private BigDecimal maxActualDepthFt;

    private JsonNode chemicalsUsed;
    private BigDecimal totalFluidPumpedBbl;
    private BigDecimal nitrogenVolumeScf;

    private Boolean jobSuccess;
    private Boolean objectivesAchieved;
    private BigDecimal metersDeployed;
    private Integer cyclesPerformed;

    private BigDecimal productiveTimeHours;
    private BigDecimal nonProductiveTimeHours;
    private BigDecimal riggingTimeHours;
    private JsonNode nptBreakdown;

    private BigDecimal estimatedCostUsd;
    private BigDecimal actualCostUsd;

    private Integer incidentsCount;
    private Boolean hasStuckPipe;
    private Boolean hasHseIncident;

    private Boolean jobReportGenerated;
    private String jobReportPath;

    private String description;
    private String notes;
    private String lessonsLearned;
    private JsonNode metadata;

    private UUID createdBy;
    private Long createdTime;
    private Long updatedTime;
    private UUID approvedBy;
    private Long approvedTime;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getJobNumber() { return jobNumber; }
    public String getJobName() { return jobName; }
    public UUID getUnitId() { return unitId; }
    public UUID getReelId() { return reelId; }
    public JobStatus getStatus() { return status; }
    public Long getCreatedTime() { return createdTime; }

    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public void setReelCode(String reelCode) { this.reelCode = reelCode; }

    public static CTJobDto fromEntity(CTJob entity) {
        if (entity == null) {
            return null;
        }
        CTJobDto dto = new CTJobDto();
        dto.id = entity.getId();
        dto.tenantId = entity.getTenantId();
        dto.jobNumber = entity.getJobNumber();
        dto.jobName = entity.getJobName();
        dto.jobType = entity.getJobType();
        dto.priority = entity.getPriority();
        dto.wellId = entity.getWellId();
        dto.wellName = entity.getWellName();
        dto.fieldName = entity.getFieldName();
        dto.clientName = entity.getClientName();
        dto.wellDepthMdFt = entity.getWellDepthMdFt();
        dto.wellDepthTvdFt = entity.getWellDepthTvdFt();
        dto.targetDepthFromFt = entity.getTargetDepthFromFt();
        dto.targetDepthToFt = entity.getTargetDepthToFt();
        dto.wellheadPressurePsi = entity.getWellheadPressurePsi();
        dto.unitId = entity.getUnitId();
        dto.reelId = entity.getReelId();
        dto.bhaConfigurationId = entity.getBhaConfigurationId();
        dto.operatorUserId = entity.getOperatorUserId();
        dto.supervisorUserId = entity.getSupervisorUserId();
        dto.engineerUserId = entity.getEngineerUserId();
        dto.plannedStartDate = entity.getPlannedStartDate();
        dto.plannedEndDate = entity.getPlannedEndDate();
        dto.estimatedDurationHours = entity.getEstimatedDurationHours();
        dto.actualStartDate = entity.getActualStartDate();
        dto.actualEndDate = entity.getActualEndDate();
        dto.actualDurationHours = entity.getActualDurationHours();
        dto.status = entity.getStatus();
        dto.statusReason = entity.getStatusReason();
        dto.currentPhase = entity.getCurrentPhase();
        dto.maxPlannedPressurePsi = entity.getMaxPlannedPressurePsi();
        dto.maxPlannedTensionLbf = entity.getMaxPlannedTensionLbf();
        dto.maxPlannedSpeedFtMin = entity.getMaxPlannedSpeedFtMin();
        dto.plannedPumpRateBpm = entity.getPlannedPumpRateBpm();
        dto.maxActualPressurePsi = entity.getMaxActualPressurePsi();
        dto.maxActualTensionLbf = entity.getMaxActualTensionLbf();
        dto.maxActualSpeedFtMin = entity.getMaxActualSpeedFtMin();
        dto.maxActualDepthFt = entity.getMaxActualDepthFt();
        dto.chemicalsUsed = entity.getChemicalsUsed();
        dto.totalFluidPumpedBbl = entity.getTotalFluidPumpedBbl();
        dto.nitrogenVolumeScf = entity.getNitrogenVolumeScf();
        dto.jobSuccess = entity.getJobSuccess();
        dto.objectivesAchieved = entity.getObjectivesAchieved();
        dto.metersDeployed = entity.getMetersDeployed();
        dto.cyclesPerformed = entity.getCyclesPerformed();
        dto.productiveTimeHours = entity.getProductiveTimeHours();
        dto.nonProductiveTimeHours = entity.getNonProductiveTimeHours();
        dto.riggingTimeHours = entity.getRiggingTimeHours();
        dto.nptBreakdown = entity.getNptBreakdown();
        dto.estimatedCostUsd = entity.getEstimatedCostUsd();
        dto.actualCostUsd = entity.getActualCostUsd();
        dto.incidentsCount = entity.getIncidentsCount();
        dto.hasStuckPipe = entity.getHasStuckPipe();
        dto.hasHseIncident = entity.getHasHseIncident();
        dto.jobReportGenerated = entity.getJobReportGenerated();
        dto.jobReportPath = entity.getJobReportPath();
        dto.description = entity.getDescription();
        dto.notes = entity.getNotes();
        dto.lessonsLearned = entity.getLessonsLearned();
        dto.metadata = entity.getMetadata();
        dto.createdBy = entity.getCreatedBy();
        dto.createdTime = entity.getCreatedTime();
        dto.updatedTime = entity.getUpdatedTime();
        dto.approvedBy = entity.getApprovedBy();
        dto.approvedTime = entity.getApprovedTime();
        return dto;
    }
}
