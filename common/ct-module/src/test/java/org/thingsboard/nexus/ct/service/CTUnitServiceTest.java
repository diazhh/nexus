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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.thingsboard.nexus.ct.dto.CTUnitDto;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.CTReel;
import org.thingsboard.nexus.ct.model.CTUnit;
import org.thingsboard.nexus.ct.model.ReelStatus;
import org.thingsboard.nexus.ct.model.UnitStatus;
import org.thingsboard.nexus.ct.repository.CTReelRepository;
import org.thingsboard.nexus.ct.repository.CTUnitRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CTUnitServiceTest {

    @Mock
    private CTUnitRepository unitRepository;

    @Mock
    private CTReelRepository reelRepository;

    @InjectMocks
    private CTUnitService unitService;

    private UUID tenantId;
    private UUID unitId;
    private UUID reelId;
    private CTUnit testUnit;
    private CTReel testReel;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        unitId = UUID.randomUUID();
        reelId = UUID.randomUUID();

        testUnit = new CTUnit();
        testUnit.setId(unitId);
        testUnit.setTenantId(tenantId);
        testUnit.setUnitCode("CT-001");
        testUnit.setUnitName("Test Unit 1");
        testUnit.setManufacturer("Test Manufacturer");
        testUnit.setModel("Model X");
        testUnit.setOperationalStatus(UnitStatus.OPERATIONAL);
        testUnit.setCurrentLocation("Test Location");
        testUnit.setCreatedTime(System.currentTimeMillis());

        testReel = new CTReel();
        testReel.setId(reelId);
        testReel.setTenantId(tenantId);
        testReel.setReelCode("REEL-001");
        testReel.setStatus(ReelStatus.AVAILABLE);
    }

    @Test
    void testGetById_Success() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));

        CTUnitDto result = unitService.getById(unitId);

        assertNotNull(result);
        assertEquals(unitId, result.getId());
        assertEquals("CT-001", result.getUnitCode());
        verify(unitRepository, times(1)).findById(unitId);
    }

    @Test
    void testGetById_NotFound() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThrows(CTEntityNotFoundException.class, () -> unitService.getById(unitId));
        verify(unitRepository, times(1)).findById(unitId);
    }

    @Test
    void testGetByCode_Success() {
        when(unitRepository.findByUnitCode("CT-001")).thenReturn(Optional.of(testUnit));

        CTUnitDto result = unitService.getByCode("CT-001");

        assertNotNull(result);
        assertEquals("CT-001", result.getUnitCode());
        verify(unitRepository, times(1)).findByUnitCode("CT-001");
    }

    @Test
    void testGetByCode_NotFound() {
        when(unitRepository.findByUnitCode("CT-999")).thenReturn(Optional.empty());

        assertThrows(CTEntityNotFoundException.class, () -> unitService.getByCode("CT-999"));
        verify(unitRepository, times(1)).findByUnitCode("CT-999");
    }

    @Test
    void testGetByTenant_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<CTUnit> units = Arrays.asList(testUnit);
        Page<CTUnit> page = new PageImpl<>(units, pageable, units.size());

        when(unitRepository.findByTenantId(tenantId, pageable)).thenReturn(page);

        Page<CTUnitDto> result = unitService.getByTenant(tenantId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("CT-001", result.getContent().get(0).getUnitCode());
        verify(unitRepository, times(1)).findByTenantId(tenantId, pageable);
    }

    @Test
    void testGetByStatus_Success() {
        List<CTUnit> units = Arrays.asList(testUnit);
        when(unitRepository.findByTenantIdAndOperationalStatus(tenantId, UnitStatus.OPERATIONAL))
            .thenReturn(units);

        List<CTUnitDto> result = unitService.getByStatus(tenantId, UnitStatus.OPERATIONAL);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(UnitStatus.OPERATIONAL, result.get(0).getOperationalStatus());
        verify(unitRepository, times(1)).findByTenantIdAndOperationalStatus(tenantId, UnitStatus.OPERATIONAL);
    }

    @Test
    void testCreate_Success() {
        when(unitRepository.existsByUnitCode("CT-001")).thenReturn(false);
        when(unitRepository.save(any(CTUnit.class))).thenReturn(testUnit);

        CTUnitDto result = unitService.create(testUnit);

        assertNotNull(result);
        assertEquals("CT-001", result.getUnitCode());
        verify(unitRepository, times(1)).existsByUnitCode("CT-001");
        verify(unitRepository, times(1)).save(any(CTUnit.class));
    }

    @Test
    void testCreate_DuplicateCode() {
        when(unitRepository.existsByUnitCode("CT-001")).thenReturn(true);

        assertThrows(CTBusinessException.class, () -> unitService.create(testUnit));
        verify(unitRepository, times(1)).existsByUnitCode("CT-001");
        verify(unitRepository, never()).save(any(CTUnit.class));
    }

    @Test
    void testUpdate_Success() {
        CTUnit updatedUnit = new CTUnit();
        updatedUnit.setUnitCode("CT-001");
        updatedUnit.setUnitName("Updated Unit");
        updatedUnit.setManufacturer("New Manufacturer");
        updatedUnit.setOperationalStatus(UnitStatus.MAINTENANCE);

        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(unitRepository.existsByUnitCode("CT-001")).thenReturn(false);
        when(unitRepository.save(any(CTUnit.class))).thenReturn(testUnit);

        CTUnitDto result = unitService.update(unitId, updatedUnit);

        assertNotNull(result);
        verify(unitRepository, times(1)).findById(unitId);
        verify(unitRepository, times(1)).save(any(CTUnit.class));
    }

    @Test
    void testUpdate_NotFound() {
        CTUnit updatedUnit = new CTUnit();
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThrows(CTEntityNotFoundException.class, () -> unitService.update(unitId, updatedUnit));
        verify(unitRepository, times(1)).findById(unitId);
        verify(unitRepository, never()).save(any(CTUnit.class));
    }

    @Test
    void testDelete_Success() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        doNothing().when(unitRepository).delete(testUnit);

        assertDoesNotThrow(() -> unitService.delete(unitId));
        verify(unitRepository, times(1)).findById(unitId);
        verify(unitRepository, times(1)).delete(testUnit);
    }

    @Test
    void testDelete_WithAssignedReel() {
        testUnit.setCurrentReelId(reelId);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));

        assertThrows(CTBusinessException.class, () -> unitService.delete(unitId));
        verify(unitRepository, times(1)).findById(unitId);
        verify(unitRepository, never()).delete(any(CTUnit.class));
    }

    @Test
    void testUpdateStatus_Success() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(unitRepository.save(any(CTUnit.class))).thenReturn(testUnit);

        CTUnitDto result = unitService.updateStatus(unitId, UnitStatus.MAINTENANCE);

        assertNotNull(result);
        verify(unitRepository, times(1)).findById(unitId);
        verify(unitRepository, times(1)).save(any(CTUnit.class));
    }

    @Test
    void testUpdateLocation_Success() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(unitRepository.save(any(CTUnit.class))).thenReturn(testUnit);

        CTUnitDto result = unitService.updateLocation(unitId, "New Location", 40.7128, -74.0060);

        assertNotNull(result);
        verify(unitRepository, times(1)).findById(unitId);
        verify(unitRepository, times(1)).save(any(CTUnit.class));
    }

    @Test
    void testAssignReel_Success() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        when(unitRepository.save(any(CTUnit.class))).thenReturn(testUnit);
        when(reelRepository.save(any(CTReel.class))).thenReturn(testReel);

        CTUnitDto result = unitService.assignReel(unitId, reelId);

        assertNotNull(result);
        verify(unitRepository, times(1)).findById(unitId);
        verify(reelRepository, times(1)).findById(reelId);
        verify(unitRepository, times(1)).save(any(CTUnit.class));
        verify(reelRepository, times(1)).save(any(CTReel.class));
    }

    @Test
    void testAssignReel_UnitAlreadyHasReel() {
        testUnit.setCurrentReelId(UUID.randomUUID());
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));

        assertThrows(CTBusinessException.class, () -> unitService.assignReel(unitId, reelId));
        verify(unitRepository, times(1)).findById(unitId);
        verify(reelRepository, never()).findById(any());
    }

    @Test
    void testAssignReel_ReelNotAvailable() {
        testReel.setStatus(ReelStatus.IN_USE);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        assertThrows(CTBusinessException.class, () -> unitService.assignReel(unitId, reelId));
        verify(unitRepository, times(1)).findById(unitId);
        verify(reelRepository, times(1)).findById(reelId);
        verify(unitRepository, never()).save(any(CTUnit.class));
    }

    @Test
    void testDetachReel_Success() {
        testUnit.setCurrentReelId(reelId);
        testReel.setStatus(ReelStatus.IN_USE);
        testReel.setCurrentUnitId(unitId);

        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        when(unitRepository.save(any(CTUnit.class))).thenReturn(testUnit);
        when(reelRepository.save(any(CTReel.class))).thenReturn(testReel);

        CTUnitDto result = unitService.detachReel(unitId);

        assertNotNull(result);
        verify(unitRepository, times(1)).findById(unitId);
        verify(reelRepository, times(1)).findById(reelId);
        verify(unitRepository, times(1)).save(any(CTUnit.class));
        verify(reelRepository, times(1)).save(any(CTReel.class));
    }

    @Test
    void testDetachReel_NoReelAssigned() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));

        assertThrows(CTBusinessException.class, () -> unitService.detachReel(unitId));
        verify(unitRepository, times(1)).findById(unitId);
        verify(reelRepository, never()).findById(any());
    }

    @Test
    void testRecordMaintenance_Success() {
        Long maintenanceDate = System.currentTimeMillis();
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(unitRepository.save(any(CTUnit.class))).thenReturn(testUnit);

        CTUnitDto result = unitService.recordMaintenance(unitId, maintenanceDate, "Routine maintenance");

        assertNotNull(result);
        verify(unitRepository, times(1)).findById(unitId);
        verify(unitRepository, times(1)).save(any(CTUnit.class));
    }

    @Test
    void testGetAvailableUnits_Success() {
        List<CTUnit> units = Arrays.asList(testUnit);
        when(unitRepository.findByTenantIdAndOperationalStatus(tenantId, UnitStatus.OPERATIONAL))
            .thenReturn(units);

        List<CTUnitDto> result = unitService.getAvailableUnits(tenantId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(unitRepository, times(1)).findByTenantIdAndOperationalStatus(tenantId, UnitStatus.OPERATIONAL);
    }

    @Test
    void testCountByStatus_Success() {
        when(unitRepository.countByTenantIdAndStatus(tenantId, UnitStatus.OPERATIONAL)).thenReturn(5L);

        long result = unitService.countByStatus(tenantId, UnitStatus.OPERATIONAL);

        assertEquals(5L, result);
        verify(unitRepository, times(1)).countByTenantIdAndStatus(tenantId, UnitStatus.OPERATIONAL);
    }
}
