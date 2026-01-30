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
package org.thingsboard.nexus.dr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.dr.dto.DrRigDto;
import org.thingsboard.nexus.dr.exception.DrBusinessException;
import org.thingsboard.nexus.dr.exception.DrEntityNotFoundException;
import org.thingsboard.nexus.dr.model.DrRig;
import org.thingsboard.nexus.dr.model.enums.RigStatus;
import org.thingsboard.nexus.dr.model.enums.RigType;
import org.thingsboard.nexus.dr.repository.DrRigRepository;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;
import org.thingsboard.server.common.data.template.TemplateInstanceResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Drilling Rigs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrRigService {

    private final DrRigRepository rigRepository;
    private final DrTemplateService templateService;
    private final DrAttributeService attributeService;

    // --- Query Operations ---

    @Transactional(readOnly = true)
    public DrRigDto getById(UUID id) {
        log.debug("Getting drilling rig by id: {}", id);
        DrRig rig = rigRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", id.toString()));
        return DrRigDto.fromEntity(rig);
    }

    @Transactional(readOnly = true)
    public DrRigDto getByCode(String rigCode) {
        log.debug("Getting drilling rig by code: {}", rigCode);
        DrRig rig = rigRepository.findByRigCode(rigCode)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", rigCode));
        return DrRigDto.fromEntity(rig);
    }

    @Transactional(readOnly = true)
    public DrRigDto getByAssetId(UUID assetId) {
        log.debug("Getting drilling rig by asset id: {}", assetId);
        DrRig rig = rigRepository.findByAssetId(assetId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", "asset:" + assetId));
        return DrRigDto.fromEntity(rig);
    }

    @Transactional(readOnly = true)
    public Page<DrRigDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting drilling rigs for tenant: {}", tenantId);
        Page<DrRig> rigs = rigRepository.findByTenantId(tenantId, pageable);
        return rigs.map(DrRigDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<DrRigDto> getAllByTenant(UUID tenantId) {
        log.debug("Getting all drilling rigs for tenant: {}", tenantId);
        List<DrRig> rigs = rigRepository.findByTenantId(tenantId);
        return rigs.stream().map(DrRigDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DrRigDto> getByFilters(UUID tenantId, RigStatus status, RigType rigType,
                                       String contractor, String location, Pageable pageable) {
        log.debug("Getting drilling rigs with filters - tenant: {}, status: {}, type: {}",
                tenantId, status, rigType);
        Page<DrRig> rigs = rigRepository.findByFilters(tenantId, status, rigType, contractor, location, pageable);
        return rigs.map(DrRigDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<DrRigDto> getByStatus(UUID tenantId, RigStatus status) {
        log.debug("Getting drilling rigs by status - tenant: {}, status: {}", tenantId, status);
        List<DrRig> rigs = rigRepository.findByTenantIdAndOperationalStatus(tenantId, status);
        return rigs.stream().map(DrRigDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrRigDto> getByType(UUID tenantId, RigType rigType) {
        log.debug("Getting drilling rigs by type - tenant: {}, type: {}", tenantId, rigType);
        List<DrRig> rigs = rigRepository.findByTenantIdAndRigType(tenantId, rigType);
        return rigs.stream().map(DrRigDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrRigDto> getAvailableRigs(UUID tenantId) {
        log.debug("Getting available drilling rigs for tenant: {}", tenantId);
        List<DrRig> rigs = rigRepository.findAvailableRigs(tenantId);
        return rigs.stream().map(DrRigDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrRigDto> getRigsWithOverdueBopTest(UUID tenantId) {
        log.debug("Getting rigs with overdue BOP test for tenant: {}", tenantId);
        long fourteenDaysAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000);
        List<DrRig> rigs = rigRepository.findRigsWithOverdueBopTest(tenantId, fourteenDaysAgo);
        return rigs.stream().map(DrRigDto::fromEntity).collect(Collectors.toList());
    }

    // --- Create Operations ---

    @Transactional
    public DrRigDto createFromTemplate(UUID tenantId, CreateFromTemplateRequest request) {
        return createFromTemplate(tenantId, request.getTemplateId(), request.getVariables(), tenantId);
    }

    @Transactional
    public DrRigDto createFromTemplate(UUID tenantId, UUID templateId, Map<String, Object> variables, UUID createdBy) {
        log.info("Creating drilling rig from template {} for tenant {}", templateId, tenantId);

        String rigCode = (String) variables.get("rigCode");
        if (rigCode == null || rigCode.isEmpty()) {
            throw new DrBusinessException("Rig code is required");
        }

        if (rigRepository.existsByRigCode(rigCode)) {
            throw new DrBusinessException("Rig code already exists: " + rigCode);
        }

        // Instantiate template to create digital twin assets
        TemplateInstanceResult instanceResult = templateService.instantiateTemplate(
                new TenantId(tenantId),
                templateId,
                variables,
                createdBy
        );

        // Create the rig entity with references to created assets
        DrRig rig = new DrRig();
        rig.setId(UUID.randomUUID());
        rig.setTenantId(tenantId);
        rig.setAssetId(instanceResult.getRootAssetId());
        rig.setRigCode(rigCode);
        rig.setRigName((String) variables.get("rigName"));
        rig.setCreatedBy(createdBy);
        rig.setCreatedTime(System.currentTimeMillis());

        // Map child asset IDs from template instantiation
        Map<String, UUID> nodeMap = instanceResult.getNodeKeyToAssetIdMap();
        rig.setDrawworksAssetId(nodeMap.get("drawworks"));
        rig.setTopDriveAssetId(nodeMap.get("top_drive"));
        rig.setMudPump1AssetId(nodeMap.get("mud_pump_1"));
        rig.setMudPump2AssetId(nodeMap.get("mud_pump_2"));
        rig.setMudPump3AssetId(nodeMap.get("mud_pump_3"));
        rig.setMudSystemAssetId(nodeMap.get("mud_system"));
        rig.setBopStackAssetId(nodeMap.get("bop_stack"));
        rig.setGasDetectorAssetId(nodeMap.get("gas_detector"));

        // Set rig specifications from variables
        String rigTypeStr = (String) variables.get("rigType");
        if (rigTypeStr != null) {
            rig.setRigType(RigType.valueOf(rigTypeStr));
        }

        rig.setContractor((String) variables.get("contractor"));
        rig.setManufacturer((String) variables.get("manufacturer"));
        rig.setModel((String) variables.get("model"));

        Object yearBuilt = variables.get("yearBuilt");
        if (yearBuilt != null) {
            rig.setYearBuilt(yearBuilt instanceof Integer ? (Integer) yearBuilt : Integer.parseInt(yearBuilt.toString()));
        }

        Object maxHookload = variables.get("maxHookloadLbs");
        if (maxHookload != null) {
            rig.setMaxHookloadLbs(maxHookload instanceof Integer ? (Integer) maxHookload : Integer.parseInt(maxHookload.toString()));
        }

        Object maxTorque = variables.get("maxRotaryTorqueFtLbs");
        if (maxTorque != null) {
            rig.setMaxRotaryTorqueFtLbs(maxTorque instanceof Integer ? (Integer) maxTorque : Integer.parseInt(maxTorque.toString()));
        }

        Object maxDepth = variables.get("maxDepthCapabilityFt");
        if (maxDepth != null) {
            rig.setMaxDepthCapabilityFt(maxDepth instanceof BigDecimal ? (BigDecimal) maxDepth :
                    BigDecimal.valueOf(Double.parseDouble(maxDepth.toString())));
        }

        rig.setCurrentLocation((String) variables.get("location"));
        rig.setOperationalStatus(RigStatus.STANDBY);

        DrRig savedRig = rigRepository.save(rig);
        log.info("Drilling rig created from template: {} with asset ID: {}", savedRig.getId(), savedRig.getAssetId());

        return DrRigDto.fromEntity(savedRig);
    }

    @Transactional
    public DrRigDto create(DrRig rig) {
        log.info("Creating new drilling rig: {}", rig.getRigCode());

        if (rigRepository.existsByRigCode(rig.getRigCode())) {
            throw new DrBusinessException("Rig code already exists: " + rig.getRigCode());
        }

        if (rig.getCreatedTime() == null) {
            rig.setCreatedTime(System.currentTimeMillis());
        }

        DrRig savedRig = rigRepository.save(rig);
        log.info("Drilling rig created: {}", savedRig.getId());

        return DrRigDto.fromEntity(savedRig);
    }

    // --- Update Operations ---

    @Transactional
    public DrRigDto update(UUID id, DrRig updatedRig) {
        log.info("Updating drilling rig: {}", id);

        DrRig existingRig = rigRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", id.toString()));

        // Check for duplicate rig code
        if (!existingRig.getRigCode().equals(updatedRig.getRigCode()) &&
            rigRepository.existsByRigCodeAndIdNot(updatedRig.getRigCode(), id)) {
            throw new DrBusinessException("Rig code already exists: " + updatedRig.getRigCode());
        }

        // Update fields
        existingRig.setRigCode(updatedRig.getRigCode());
        existingRig.setRigName(updatedRig.getRigName());
        existingRig.setRigType(updatedRig.getRigType());
        existingRig.setContractor(updatedRig.getContractor());
        existingRig.setManufacturer(updatedRig.getManufacturer());
        existingRig.setModel(updatedRig.getModel());
        existingRig.setYearBuilt(updatedRig.getYearBuilt());
        existingRig.setMaxHookloadLbs(updatedRig.getMaxHookloadLbs());
        existingRig.setMaxRotaryTorqueFtLbs(updatedRig.getMaxRotaryTorqueFtLbs());
        existingRig.setMaxDepthCapabilityFt(updatedRig.getMaxDepthCapabilityFt());
        existingRig.setCurrentLocation(updatedRig.getCurrentLocation());
        existingRig.setLatitude(updatedRig.getLatitude());
        existingRig.setLongitude(updatedRig.getLongitude());
        existingRig.setNotes(updatedRig.getNotes());
        existingRig.setMetadata(updatedRig.getMetadata());
        existingRig.setUpdatedTime(System.currentTimeMillis());

        DrRig savedRig = rigRepository.save(existingRig);
        log.info("Drilling rig updated: {}", savedRig.getId());

        return DrRigDto.fromEntity(savedRig);
    }

    @Transactional
    public DrRigDto updateFromDto(UUID id, DrRigDto updatedRig) {
        log.info("Updating drilling rig: {}", id);

        DrRig existingRig = rigRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", id.toString()));

        // Check for duplicate rig code
        if (!existingRig.getRigCode().equals(updatedRig.getRigCode()) &&
            rigRepository.existsByRigCodeAndIdNot(updatedRig.getRigCode(), id)) {
            throw new DrBusinessException("Rig code already exists: " + updatedRig.getRigCode());
        }

        // Update fields
        existingRig.setRigCode(updatedRig.getRigCode());
        existingRig.setRigName(updatedRig.getRigName());
        existingRig.setRigType(updatedRig.getRigType());
        existingRig.setContractor(updatedRig.getContractor());
        existingRig.setManufacturer(updatedRig.getManufacturer());
        existingRig.setModel(updatedRig.getModel());
        existingRig.setYearBuilt(updatedRig.getYearBuilt());
        existingRig.setMaxHookloadLbs(updatedRig.getMaxHookloadLbs());
        existingRig.setMaxRotaryTorqueFtLbs(updatedRig.getMaxRotaryTorqueFtLbs());
        existingRig.setMaxDepthCapabilityFt(updatedRig.getMaxDepthCapabilityFt());
        existingRig.setCurrentLocation(updatedRig.getCurrentLocation());
        existingRig.setLatitude(updatedRig.getLatitude());
        existingRig.setLongitude(updatedRig.getLongitude());
        existingRig.setNotes(updatedRig.getNotes());
        existingRig.setMetadata(updatedRig.getMetadata());
        existingRig.setUpdatedTime(System.currentTimeMillis());

        DrRig savedRig = rigRepository.save(existingRig);
        log.info("Drilling rig updated: {}", savedRig.getId());

        return DrRigDto.fromEntity(savedRig);
    }

    @Transactional
    public DrRigDto updateStatus(UUID rigId, RigStatus newStatus) {
        log.info("Updating rig {} status to {}", rigId, newStatus);

        DrRig rig = rigRepository.findById(rigId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", rigId.toString()));

        rig.setOperationalStatus(newStatus);
        rig.setUpdatedTime(System.currentTimeMillis());

        DrRig savedRig = rigRepository.save(rig);
        return DrRigDto.fromEntity(savedRig);
    }

    @Transactional
    public DrRigDto updateLocation(UUID rigId, String location, BigDecimal latitude, BigDecimal longitude) {
        log.info("Updating rig {} location to {}", rigId, location);

        DrRig rig = rigRepository.findById(rigId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", rigId.toString()));

        rig.setCurrentLocation(location);
        rig.setLatitude(latitude);
        rig.setLongitude(longitude);
        rig.setUpdatedTime(System.currentTimeMillis());

        DrRig savedRig = rigRepository.save(rig);
        return DrRigDto.fromEntity(savedRig);
    }

    @Transactional
    public DrRigDto updateLocation(UUID rigId, String location, Double latitude, Double longitude) {
        BigDecimal lat = latitude != null ? BigDecimal.valueOf(latitude) : null;
        BigDecimal lon = longitude != null ? BigDecimal.valueOf(longitude) : null;
        return updateLocation(rigId, location, lat, lon);
    }

    // --- Well Assignment Operations ---

    @Transactional
    public DrRigDto assignToWell(UUID rigId, UUID wellId) {
        log.info("Assigning rig {} to well {}", rigId, wellId);

        DrRig rig = rigRepository.findById(rigId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", rigId.toString()));

        if (rig.getCurrentWellId() != null) {
            throw new DrBusinessException("Rig is already assigned to a well. Release it first.");
        }

        // Check if another rig is assigned to this well
        rigRepository.findByCurrentWellId(wellId).ifPresent(existingRig -> {
            throw new DrBusinessException("Another rig is already assigned to this well: " + existingRig.getRigCode());
        });

        rig.setCurrentWellId(wellId);
        rig.setOperationalStatus(RigStatus.RIG_UP);
        rig.setUpdatedTime(System.currentTimeMillis());

        DrRig savedRig = rigRepository.save(rig);
        log.info("Rig assigned to well successfully");
        return DrRigDto.fromEntity(savedRig);
    }

    @Transactional
    public DrRigDto releaseFromWell(UUID rigId) {
        log.info("Releasing rig {} from well", rigId);

        DrRig rig = rigRepository.findById(rigId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", rigId.toString()));

        if (rig.getCurrentWellId() == null) {
            throw new DrBusinessException("Rig is not assigned to any well");
        }

        // Update statistics
        if (rig.getTotalWellsDrilled() != null) {
            rig.setTotalWellsDrilled(rig.getTotalWellsDrilled() + 1);
        } else {
            rig.setTotalWellsDrilled(1);
        }

        rig.setCurrentWellId(null);
        rig.setCurrentRunId(null);
        rig.setOperationalStatus(RigStatus.RIG_DOWN);
        rig.setUpdatedTime(System.currentTimeMillis());

        DrRig savedRig = rigRepository.save(rig);
        log.info("Rig released from well successfully");
        return DrRigDto.fromEntity(savedRig);
    }

    // --- BOP Test Operations ---

    @Transactional
    public DrRigDto recordBopTest(UUID rigId, Long testDate) {
        return recordBopTest(rigId, testDate, null);
    }

    @Transactional
    public DrRigDto recordBopTest(UUID rigId, Long testDate, String notes) {
        log.info("Recording BOP test for rig {}", rigId);

        DrRig rig = rigRepository.findById(rigId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", rigId.toString()));

        rig.setBopTestDate(testDate);
        rig.setUpdatedTime(System.currentTimeMillis());

        if (notes != null) {
            String existingNotes = rig.getNotes() != null ? rig.getNotes() : "";
            rig.setNotes(existingNotes + "\n[" + System.currentTimeMillis() + "] BOP Test: " + notes);
        }

        DrRig savedRig = rigRepository.save(rig);
        log.info("BOP test recorded successfully");
        return DrRigDto.fromEntity(savedRig);
    }

    // --- Inspection Operations ---

    @Transactional
    public DrRigDto recordInspection(UUID rigId, Long inspectionDate, Long nextInspectionDue, String notes) {
        log.info("Recording inspection for rig {}", rigId);

        DrRig rig = rigRepository.findById(rigId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", rigId.toString()));

        rig.setLastRigInspectionDate(inspectionDate);
        if (nextInspectionDue != null) {
            rig.setNextRigInspectionDue(nextInspectionDue);
        }
        rig.setUpdatedTime(System.currentTimeMillis());

        if (notes != null) {
            String existingNotes = rig.getNotes() != null ? rig.getNotes() : "";
            rig.setNotes(existingNotes + "\n[" + System.currentTimeMillis() + "] Inspection: " + notes);
        }

        DrRig savedRig = rigRepository.save(rig);
        log.info("Inspection recorded successfully");
        return DrRigDto.fromEntity(savedRig);
    }

    // --- Statistics Operations ---

    @Transactional
    public DrRigDto updateStatistics(UUID rigId, Integer totalWellsDrilled, Double totalFootageDrilledFt,
                                     Double totalNptHours, Double totalOperationalHours) {
        log.info("Updating statistics for rig {}", rigId);

        DrRig rig = rigRepository.findById(rigId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", rigId.toString()));

        if (totalWellsDrilled != null) {
            rig.setTotalWellsDrilled(totalWellsDrilled);
        }
        if (totalFootageDrilledFt != null) {
            rig.setTotalFootageDrilledFt(BigDecimal.valueOf(totalFootageDrilledFt));
        }
        if (totalNptHours != null) {
            rig.setTotalNptHours(BigDecimal.valueOf(totalNptHours));
        }
        if (totalOperationalHours != null) {
            rig.setTotalOperationalHours(BigDecimal.valueOf(totalOperationalHours));
        }
        rig.setUpdatedTime(System.currentTimeMillis());

        DrRig savedRig = rigRepository.save(rig);
        log.info("Statistics updated successfully");
        return DrRigDto.fromEntity(savedRig);
    }

    // --- Delete Operations ---

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting drilling rig: {}", id);

        DrRig rig = rigRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", id.toString()));

        if (rig.getCurrentWellId() != null) {
            throw new DrBusinessException("Cannot delete rig assigned to a well. Release it first.");
        }

        rigRepository.delete(rig);
        log.info("Drilling rig deleted: {}", id);
    }

    // --- Statistics ---

    @Transactional(readOnly = true)
    public long countByStatus(UUID tenantId, RigStatus status) {
        return rigRepository.countByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true)
    public long countByType(UUID tenantId, RigType rigType) {
        return rigRepository.countByTenantIdAndRigType(tenantId, rigType);
    }
}
