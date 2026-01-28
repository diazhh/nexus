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
package org.thingsboard.nexus.ct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.ct.dto.CTReelDto;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.CTReel;
import org.thingsboard.nexus.ct.model.ReelStatus;
import org.thingsboard.nexus.ct.repository.CTReelRepository;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;
import org.thingsboard.server.common.data.template.TemplateInstanceResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTReelService {

    private final CTReelRepository reelRepository;
    private final CTTemplateService templateService;

    @Transactional(readOnly = true)
    public CTReelDto getById(UUID id) {
        log.debug("Getting CT Reel by id: {}", id);
        CTReel reel = reelRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", id.toString()));
        return CTReelDto.fromEntity(reel);
    }

    @Transactional(readOnly = true)
    public CTReelDto getByCode(String reelCode) {
        log.debug("Getting CT Reel by code: {}", reelCode);
        CTReel reel = reelRepository.findByReelCode(reelCode)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", reelCode));
        return CTReelDto.fromEntity(reel);
    }

    @Transactional(readOnly = true)
    public Page<CTReelDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting CT Reels for tenant: {}", tenantId);
        Page<CTReel> reels = reelRepository.findByTenantId(tenantId, pageable);
        return reels.map(CTReelDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CTReelDto> getByFilters(UUID tenantId, ReelStatus status, BigDecimal odInch,
                                        BigDecimal fatigueMin, BigDecimal fatigueMax, Pageable pageable) {
        log.debug("Getting CT Reels with filters - tenant: {}, status: {}, OD: {}", 
                  tenantId, status, odInch);
        Page<CTReel> reels = reelRepository.findByFilters(tenantId, status, odInch, 
                                                          fatigueMin, fatigueMax, pageable);
        return reels.map(CTReelDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CTReelDto> getByStatus(UUID tenantId, ReelStatus status) {
        log.debug("Getting CT Reels by status - tenant: {}, status: {}", tenantId, status);
        List<CTReel> reels = reelRepository.findByTenantIdAndStatus(tenantId, status);
        return reels.stream()
            .map(CTReelDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CTReelDto> getAvailableReelsBySize(UUID tenantId, BigDecimal odInch, BigDecimal maxFatigue) {
        log.debug("Getting available reels - tenant: {}, OD: {}, maxFatigue: {}", 
                  tenantId, odInch, maxFatigue);
        List<CTReel> reels = reelRepository.findAvailableReelsBySize(tenantId, odInch, maxFatigue);
        return reels.stream()
            .map(CTReelDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CTReelDto> getReelsAboveFatigueThreshold(UUID tenantId, BigDecimal threshold) {
        log.debug("Getting reels above fatigue threshold - tenant: {}, threshold: {}", 
                  tenantId, threshold);
        List<CTReel> reels = reelRepository.findReelsAboveFatigueThreshold(tenantId, threshold);
        return reels.stream()
            .map(CTReelDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional
    public CTReelDto create(CTReel reel) {
        log.info("Creating new CT Reel: {}", reel.getReelCode());
        
        if (reelRepository.existsByReelCode(reel.getReelCode())) {
            throw new CTBusinessException("Reel code already exists: " + reel.getReelCode());
        }
        
        if (reel.getCreatedTime() == null) {
            reel.setCreatedTime(System.currentTimeMillis());
        }
        
        CTReel savedReel = reelRepository.save(reel);
        log.info("CT Reel created successfully: {}", savedReel.getId());
        
        return CTReelDto.fromEntity(savedReel);
    }

    @Transactional
    public CTReelDto createFromTemplate(UUID tenantId, CreateFromTemplateRequest request) {
        log.info("Creating CT Reel from template {} for tenant {}", request.getTemplateId(), tenantId);

        Map<String, Object> variables = request.getVariables();

        String reelCode = (String) variables.get("reelCode");
        if (reelCode == null || reelCode.isEmpty()) {
            throw new CTBusinessException("Reel code is required");
        }

        if (reelRepository.existsByReelCode(reelCode)) {
            throw new CTBusinessException("Reel code already exists: " + reelCode);
        }

        TemplateInstanceResult instanceResult = templateService.instantiateTemplate(
                new TenantId(tenantId),
                request.getTemplateId(),
                variables,
                tenantId
        );

        CTReel reel = new CTReel();
        reel.setId(UUID.randomUUID());
        reel.setTenantId(tenantId);
        reel.setAssetId(instanceResult.getRootAssetId());
        reel.setReelCode(reelCode);
        reel.setReelName((String) variables.get("reelName"));

        Object tubingOD = variables.get("tubingOD");
        if (tubingOD != null) {
            reel.setTubingOdInch(tubingOD instanceof Double ?
                    BigDecimal.valueOf((Double) tubingOD) :
                    BigDecimal.valueOf(Double.parseDouble(tubingOD.toString())));
        }

        Object tubingID = variables.get("tubingID");
        if (tubingID != null) {
            reel.setTubingIdInch(tubingID instanceof Double ?
                    BigDecimal.valueOf((Double) tubingID) :
                    BigDecimal.valueOf(Double.parseDouble(tubingID.toString())));
        }

        Object wallThickness = variables.get("wallThickness");
        if (wallThickness != null) {
            reel.setWallThicknessInch(wallThickness instanceof Double ?
                    BigDecimal.valueOf((Double) wallThickness) :
                    BigDecimal.valueOf(Double.parseDouble(wallThickness.toString())));
        }

        Object totalLength = variables.get("totalLength");
        if (totalLength != null) {
            reel.setTotalLengthFt(totalLength instanceof Double ?
                    BigDecimal.valueOf((Double) totalLength) :
                    BigDecimal.valueOf(Double.parseDouble(totalLength.toString())));
        }

        String materialGrade = (String) variables.get("material");
        if (materialGrade == null) {
            materialGrade = (String) variables.get("grade");
        }
        reel.setMaterialGrade(materialGrade);
        reel.setStatus(ReelStatus.AVAILABLE);
        reel.setAccumulatedFatiguePercent(BigDecimal.ZERO);
        reel.setCreatedTime(System.currentTimeMillis());
        reel.setUpdatedTime(System.currentTimeMillis());

        CTReel savedReel = reelRepository.save(reel);
        log.info("CT Reel created from template successfully: {} with asset ID: {}",
                savedReel.getId(), savedReel.getAssetId());

        return CTReelDto.fromEntity(savedReel);
    }

    @Transactional
    public CTReelDto update(UUID id, CTReel updatedReel) {
        log.info("Updating CT Reel: {}", id);
        
        CTReel existingReel = reelRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", id.toString()));
        
        if (!existingReel.getReelCode().equals(updatedReel.getReelCode()) &&
            reelRepository.existsByReelCode(updatedReel.getReelCode())) {
            throw new CTBusinessException("Reel code already exists: " + updatedReel.getReelCode());
        }
        
        existingReel.setReelName(updatedReel.getReelName());
        existingReel.setTubingOdInch(updatedReel.getTubingOdInch());
        existingReel.setTubingIdInch(updatedReel.getTubingIdInch());
        existingReel.setWallThicknessInch(updatedReel.getWallThicknessInch());
        existingReel.setTotalLengthFt(updatedReel.getTotalLengthFt());
        existingReel.setMaterialGrade(updatedReel.getMaterialGrade());
        existingReel.setMaterialYieldStrengthPsi(updatedReel.getMaterialYieldStrengthPsi());
        existingReel.setMaterialTensileStrengthPsi(updatedReel.getMaterialTensileStrengthPsi());
        existingReel.setYoungsModulusPsi(updatedReel.getYoungsModulusPsi());
        existingReel.setHasWelds(updatedReel.getHasWelds());
        existingReel.setWeldStressConcentrationFactor(updatedReel.getWeldStressConcentrationFactor());
        existingReel.setCorrosionEnvironment(updatedReel.getCorrosionEnvironment());
        existingReel.setCorrosionFactor(updatedReel.getCorrosionFactor());
        existingReel.setReelCoreDiameterInch(updatedReel.getReelCoreDiameterInch());
        existingReel.setTypicalGooseneckRadiusInch(updatedReel.getTypicalGooseneckRadiusInch());
        existingReel.setStatus(updatedReel.getStatus());
        existingReel.setCurrentLocation(updatedReel.getCurrentLocation());
        existingReel.setNotes(updatedReel.getNotes());
        existingReel.setMetadata(updatedReel.getMetadata());
        existingReel.setUpdatedTime(System.currentTimeMillis());
        
        CTReel savedReel = reelRepository.save(existingReel);
        log.info("CT Reel updated successfully: {}", savedReel.getId());
        
        return CTReelDto.fromEntity(savedReel);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting CT Reel: {}", id);
        
        CTReel reel = reelRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", id.toString()));
        
        if (reel.getStatus().equals(ReelStatus.IN_USE)) {
            throw new CTBusinessException("Cannot delete reel that is currently in use");
        }
        
        reelRepository.delete(reel);
        log.info("CT Reel deleted successfully: {}", id);
    }

    @Transactional
    public CTReelDto updateStatus(UUID reelId, ReelStatus newStatus) {
        log.info("Updating reel {} status to {}", reelId, newStatus);
        
        CTReel reel = reelRepository.findById(reelId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", reelId.toString()));
        
        reel.setStatus(newStatus);
        reel.setUpdatedTime(System.currentTimeMillis());
        CTReel savedReel = reelRepository.save(reel);
        
        log.info("Reel status updated successfully");
        return CTReelDto.fromEntity(savedReel);
    }

    @Transactional
    public CTReelDto updateFatigue(UUID reelId, BigDecimal fatiguePercent, Integer cycles) {
        log.info("Updating reel {} fatigue to {}%", reelId, fatiguePercent);
        
        CTReel reel = reelRepository.findById(reelId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", reelId.toString()));
        
        reel.setAccumulatedFatiguePercent(fatiguePercent);
        if (cycles != null) {
            reel.setTotalCycles(cycles);
        }
        reel.setUpdatedTime(System.currentTimeMillis());
        CTReel savedReel = reelRepository.save(reel);
        
        log.info("Reel fatigue updated successfully");
        return CTReelDto.fromEntity(savedReel);
    }

    @Transactional
    public CTReelDto recordInspection(UUID reelId, Long inspectionDate, String inspectionType, 
                                     String inspectionResult, String notes) {
        log.info("Recording inspection for reel {}", reelId);
        
        CTReel reel = reelRepository.findById(reelId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", reelId.toString()));
        
        reel.setLastInspectionDate(inspectionDate);
        reel.setUpdatedTime(System.currentTimeMillis());
        
        if (notes != null) {
            String existingNotes = reel.getNotes() != null ? reel.getNotes() : "";
            String inspectionNote = String.format("\n[%d] Inspection: %s - Result: %s - %s", 
                                                  inspectionDate, inspectionType, inspectionResult, notes);
            reel.setNotes(existingNotes + inspectionNote);
        }
        
        CTReel savedReel = reelRepository.save(reel);
        log.info("Inspection recorded successfully");
        return CTReelDto.fromEntity(savedReel);
    }

    @Transactional
    public CTReelDto retireReel(UUID reelId, String reason) {
        log.info("Retiring reel {} - Reason: {}", reelId, reason);
        
        CTReel reel = reelRepository.findById(reelId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", reelId.toString()));
        
        if (reel.getStatus() == ReelStatus.IN_USE) {
            throw new CTBusinessException("Cannot retire reel that is currently in use");
        }
        
        reel.setStatus(ReelStatus.RETIRED);
        reel.setRetirementDate(System.currentTimeMillis());
        reel.setUpdatedTime(System.currentTimeMillis());
        
        String existingNotes = reel.getNotes() != null ? reel.getNotes() : "";
        reel.setNotes(existingNotes + "\n[RETIRED] Reason: " + reason);
        
        CTReel savedReel = reelRepository.save(reel);
        log.info("Reel retired successfully");
        return CTReelDto.fromEntity(savedReel);
    }

    @Transactional(readOnly = true)
    public long countByStatus(UUID tenantId, ReelStatus status) {
        return reelRepository.countByTenantIdAndStatus(tenantId, status);
    }
}
