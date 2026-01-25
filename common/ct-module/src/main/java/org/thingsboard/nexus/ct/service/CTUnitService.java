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
import org.thingsboard.nexus.ct.dto.CTUnitDto;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.CTReel;
import org.thingsboard.nexus.ct.model.CTUnit;
import org.thingsboard.nexus.ct.model.ReelStatus;
import org.thingsboard.nexus.ct.model.UnitStatus;
import org.thingsboard.nexus.ct.repository.CTReelRepository;
import org.thingsboard.nexus.ct.repository.CTUnitRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTUnitService {

    private final CTUnitRepository unitRepository;
    private final CTReelRepository reelRepository;

    @Transactional(readOnly = true)
    public CTUnitDto getById(UUID id) {
        log.debug("Getting CT Unit by id: {}", id);
        CTUnit unit = unitRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", id.toString()));
        return CTUnitDto.fromEntity(unit);
    }

    @Transactional(readOnly = true)
    public CTUnitDto getByCode(String unitCode) {
        log.debug("Getting CT Unit by code: {}", unitCode);
        CTUnit unit = unitRepository.findByUnitCode(unitCode)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", unitCode));
        return CTUnitDto.fromEntity(unit);
    }

    @Transactional(readOnly = true)
    public Page<CTUnitDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting CT Units for tenant: {}", tenantId);
        Page<CTUnit> units = unitRepository.findByTenantId(tenantId, pageable);
        return units.map(CTUnitDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CTUnitDto> getByFilters(UUID tenantId, UnitStatus status, String location, Pageable pageable) {
        log.debug("Getting CT Units with filters - tenant: {}, status: {}, location: {}", 
                  tenantId, status, location);
        Page<CTUnit> units = unitRepository.findByFilters(tenantId, status, location, pageable);
        return units.map(CTUnitDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CTUnitDto> getByStatus(UUID tenantId, UnitStatus status) {
        log.debug("Getting CT Units by status - tenant: {}, status: {}", tenantId, status);
        List<CTUnit> units = unitRepository.findByTenantIdAndOperationalStatus(tenantId, status);
        return units.stream()
            .map(CTUnitDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional
    public CTUnitDto create(CTUnit unit) {
        log.info("Creating new CT Unit: {}", unit.getUnitCode());
        
        if (unitRepository.existsByUnitCode(unit.getUnitCode())) {
            throw new CTBusinessException("Unit code already exists: " + unit.getUnitCode());
        }
        
        if (unit.getCreatedTime() == null) {
            unit.setCreatedTime(System.currentTimeMillis());
        }
        
        CTUnit savedUnit = unitRepository.save(unit);
        log.info("CT Unit created successfully: {}", savedUnit.getId());
        
        return CTUnitDto.fromEntity(savedUnit);
    }

    @Transactional
    public CTUnitDto update(UUID id, CTUnit updatedUnit) {
        log.info("Updating CT Unit: {}", id);
        
        CTUnit existingUnit = unitRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", id.toString()));
        
        if (!existingUnit.getUnitCode().equals(updatedUnit.getUnitCode()) &&
            unitRepository.existsByUnitCode(updatedUnit.getUnitCode())) {
            throw new CTBusinessException("Unit code already exists: " + updatedUnit.getUnitCode());
        }
        
        existingUnit.setUnitName(updatedUnit.getUnitName());
        existingUnit.setManufacturer(updatedUnit.getManufacturer());
        existingUnit.setModel(updatedUnit.getModel());
        existingUnit.setSerialNumber(updatedUnit.getSerialNumber());
        existingUnit.setYearManufactured(updatedUnit.getYearManufactured());
        existingUnit.setMaxPressurePsi(updatedUnit.getMaxPressurePsi());
        existingUnit.setMaxTensionLbf(updatedUnit.getMaxTensionLbf());
        existingUnit.setMaxSpeedFtMin(updatedUnit.getMaxSpeedFtMin());
        existingUnit.setMaxTubingOdInch(updatedUnit.getMaxTubingOdInch());
        existingUnit.setOperationalStatus(updatedUnit.getOperationalStatus());
        existingUnit.setCurrentLocation(updatedUnit.getCurrentLocation());
        existingUnit.setLatitude(updatedUnit.getLatitude());
        existingUnit.setLongitude(updatedUnit.getLongitude());
        existingUnit.setNotes(updatedUnit.getNotes());
        existingUnit.setMetadata(updatedUnit.getMetadata());
        existingUnit.setUpdatedTime(System.currentTimeMillis());
        
        CTUnit savedUnit = unitRepository.save(existingUnit);
        log.info("CT Unit updated successfully: {}", savedUnit.getId());
        
        return CTUnitDto.fromEntity(savedUnit);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting CT Unit: {}", id);
        
        CTUnit unit = unitRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", id.toString()));
        
        if (unit.getCurrentReelId() != null) {
            throw new CTBusinessException("Cannot delete unit with assigned reel. Detach reel first.");
        }
        
        unitRepository.delete(unit);
        log.info("CT Unit deleted successfully: {}", id);
    }


    @Transactional
    public CTUnitDto updateStatus(UUID unitId, UnitStatus newStatus) {
        log.info("Updating unit {} status to {}", unitId, newStatus);
        
        CTUnit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", unitId.toString()));
        
        unit.setOperationalStatus(newStatus);
        unit.setUpdatedTime(System.currentTimeMillis());
        CTUnit savedUnit = unitRepository.save(unit);
        
        log.info("Unit status updated successfully");
        return CTUnitDto.fromEntity(savedUnit);
    }

    @Transactional
    public CTUnitDto updateLocation(UUID unitId, String location, Double latitude, Double longitude) {
        log.info("Updating unit {} location to {}", unitId, location);
        
        CTUnit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", unitId.toString()));
        
        unit.setCurrentLocation(location);
        if (latitude != null) {
            unit.setLatitude(BigDecimal.valueOf(latitude));
        }
        if (longitude != null) {
            unit.setLongitude(BigDecimal.valueOf(longitude));
        }
        unit.setUpdatedTime(System.currentTimeMillis());
        CTUnit savedUnit = unitRepository.save(unit);
        
        log.info("Unit location updated successfully");
        return CTUnitDto.fromEntity(savedUnit);
    }

    @Transactional
    public CTUnitDto assignReel(UUID unitId, UUID reelId) {
        log.info("Assigning reel {} to unit {}", reelId, unitId);
        
        CTUnit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", unitId.toString()));
        
        CTReel reel = reelRepository.findById(reelId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", reelId.toString()));
        
        if (unit.getCurrentReelId() != null) {
            throw new CTBusinessException("Unit already has a reel assigned. Detach current reel first.");
        }
        
        if (!reel.getStatus().equals(ReelStatus.AVAILABLE)) {
            throw new CTBusinessException("Reel is not available for assignment. Status: " + reel.getStatus());
        }
        
        unit.setCurrentReelId(reelId);
        unit.setReelCoupledDate(System.currentTimeMillis());
        CTUnit savedUnit = unitRepository.save(unit);
        
        reel.setStatus(ReelStatus.IN_USE);
        reel.setCurrentUnitId(unitId);
        reelRepository.save(reel);
        
        log.info("Reel assigned successfully");
        return CTUnitDto.fromEntity(savedUnit);
    }

    @Transactional
    public CTUnitDto detachReel(UUID unitId) {
        log.info("Detaching reel from unit {}", unitId);
        
        CTUnit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", unitId.toString()));
        
        if (unit.getCurrentReelId() == null) {
            throw new CTBusinessException("Unit has no reel assigned");
        }
        
        UUID reelId = unit.getCurrentReelId();
        CTReel reel = reelRepository.findById(reelId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", reelId.toString()));
        
        unit.setCurrentReelId(null);
        unit.setReelCoupledDate(null);
        CTUnit savedUnit = unitRepository.save(unit);
        
        reel.setStatus(ReelStatus.AVAILABLE);
        reel.setCurrentUnitId(null);
        reelRepository.save(reel);
        
        log.info("Reel detached successfully");
        return CTUnitDto.fromEntity(savedUnit);
    }

    @Transactional
    public CTUnitDto recordMaintenance(UUID unitId, Long maintenanceDate, String notes) {
        log.info("Recording maintenance for unit {}", unitId);
        
        CTUnit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", unitId.toString()));
        
        unit.setLastMaintenanceDate(maintenanceDate);
        unit.setUpdatedTime(System.currentTimeMillis());
        if (notes != null) {
            String existingNotes = unit.getNotes() != null ? unit.getNotes() : "";
            unit.setNotes(existingNotes + "\n[" + System.currentTimeMillis() + "] Maintenance: " + notes);
        }
        CTUnit savedUnit = unitRepository.save(unit);
        
        log.info("Maintenance recorded successfully");
        return CTUnitDto.fromEntity(savedUnit);
    }

    @Transactional(readOnly = true)
    public List<CTUnitDto> getAvailableUnits(UUID tenantId) {
        log.debug("Getting available CT Units for tenant: {}", tenantId);
        List<CTUnit> units = unitRepository.findByTenantIdAndOperationalStatus(tenantId, UnitStatus.OPERATIONAL);
        return units.stream()
            .map(CTUnitDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CTUnitDto> getUnitsRequiringMaintenance(UUID tenantId) {
        log.debug("Getting CT Units requiring maintenance for tenant: {}", tenantId);
        Long now = System.currentTimeMillis();
        Long thirtyDaysAgo = now - (30L * 24L * 60L * 60L * 1000L);
        
        List<CTUnit> units = unitRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();
        return units.stream()
            .filter(unit -> unit.getLastMaintenanceDate() == null || 
                          unit.getLastMaintenanceDate() < thirtyDaysAgo)
            .map(CTUnitDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countByStatus(UUID tenantId, UnitStatus status) {
        return unitRepository.countByTenantIdAndStatus(tenantId, status);
    }
}
