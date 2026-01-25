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
import org.thingsboard.nexus.ct.dto.CTReelDto;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.CTReel;
import org.thingsboard.nexus.ct.model.ReelStatus;
import org.thingsboard.nexus.ct.repository.CTReelRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CTReelServiceTest {

    @Mock
    private CTReelRepository reelRepository;

    @InjectMocks
    private CTReelService reelService;

    private UUID tenantId;
    private UUID reelId;
    private CTReel testReel;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        reelId = UUID.randomUUID();

        testReel = new CTReel();
        testReel.setId(reelId);
        testReel.setTenantId(tenantId);
        testReel.setReelCode("REEL-001");
        testReel.setStatus(ReelStatus.AVAILABLE);
        testReel.setMaterialGrade("Steel");
        testReel.setTubingOdInch(BigDecimal.valueOf(2.375));
        testReel.setWallThicknessInch(BigDecimal.valueOf(0.190));
        testReel.setTotalLengthFt(BigDecimal.valueOf(15000));
        testReel.setAccumulatedFatiguePercent(BigDecimal.valueOf(45.5));
        testReel.setTotalCycles(1500);
        testReel.setCreatedTime(System.currentTimeMillis());
    }

    @Test
    void testGetById_Success() {
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        CTReelDto result = reelService.getById(reelId);

        assertNotNull(result);
        assertEquals(reelId, result.getId());
        assertEquals("REEL-001", result.getReelCode());
        verify(reelRepository, times(1)).findById(reelId);
    }

    @Test
    void testGetById_NotFound() {
        when(reelRepository.findById(reelId)).thenReturn(Optional.empty());

        assertThrows(CTEntityNotFoundException.class, () -> reelService.getById(reelId));
        verify(reelRepository, times(1)).findById(reelId);
    }

    @Test
    void testGetByCode_Success() {
        when(reelRepository.findByReelCode("REEL-001")).thenReturn(Optional.of(testReel));

        CTReelDto result = reelService.getByCode("REEL-001");

        assertNotNull(result);
        assertEquals("REEL-001", result.getReelCode());
        verify(reelRepository, times(1)).findByReelCode("REEL-001");
    }

    @Test
    void testGetByTenant_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<CTReel> reels = Arrays.asList(testReel);
        Page<CTReel> page = new PageImpl<>(reels, pageable, reels.size());

        when(reelRepository.findByTenantId(tenantId, pageable)).thenReturn(page);

        Page<CTReelDto> result = reelService.getByTenant(tenantId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("REEL-001", result.getContent().get(0).getReelCode());
        verify(reelRepository, times(1)).findByTenantId(tenantId, pageable);
    }

    @Test
    void testGetByStatus_Success() {
        List<CTReel> reels = Arrays.asList(testReel);
        when(reelRepository.findByTenantIdAndStatus(tenantId, ReelStatus.AVAILABLE))
            .thenReturn(reels);

        List<CTReelDto> result = reelService.getByStatus(tenantId, ReelStatus.AVAILABLE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ReelStatus.AVAILABLE, result.get(0).getStatus());
        verify(reelRepository, times(1)).findByTenantIdAndStatus(tenantId, ReelStatus.AVAILABLE);
    }

    @Test
    void testCreate_Success() {
        when(reelRepository.existsByReelCode("REEL-001")).thenReturn(false);
        when(reelRepository.save(any(CTReel.class))).thenReturn(testReel);

        CTReelDto result = reelService.create(testReel);

        assertNotNull(result);
        assertEquals("REEL-001", result.getReelCode());
        verify(reelRepository, times(1)).existsByReelCode("REEL-001");
        verify(reelRepository, times(1)).save(any(CTReel.class));
    }

    @Test
    void testCreate_DuplicateCode() {
        when(reelRepository.existsByReelCode("REEL-001")).thenReturn(true);

        assertThrows(CTBusinessException.class, () -> reelService.create(testReel));
        verify(reelRepository, times(1)).existsByReelCode("REEL-001");
        verify(reelRepository, never()).save(any(CTReel.class));
    }

    @Test
    void testUpdateFatigue_Success() {
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        when(reelRepository.save(any(CTReel.class))).thenReturn(testReel);

        CTReelDto result = reelService.updateFatigue(reelId, BigDecimal.valueOf(55.0), 100);

        assertNotNull(result);
        verify(reelRepository, times(1)).findById(reelId);
        verify(reelRepository, times(1)).save(any(CTReel.class));
    }

    @Test
    void testRetireReel_Success() {
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        when(reelRepository.save(any(CTReel.class))).thenReturn(testReel);

        CTReelDto result = reelService.retireReel(reelId, "End of life cycle");

        assertNotNull(result);
        verify(reelRepository, times(1)).findById(reelId);
        verify(reelRepository, times(1)).save(any(CTReel.class));
    }

    @Test
    void testRetireReel_InUse() {
        testReel.setStatus(ReelStatus.IN_USE);
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        assertThrows(CTBusinessException.class, () -> reelService.retireReel(reelId, "Test"));
        verify(reelRepository, times(1)).findById(reelId);
        verify(reelRepository, never()).save(any(CTReel.class));
    }

    @Test
    void testGetAvailableReelsBySize_Success() {
        List<CTReel> reels = Arrays.asList(testReel);
        BigDecimal odInch = BigDecimal.valueOf(2.375);
        BigDecimal maxFatigue = BigDecimal.valueOf(80.0);

        when(reelRepository.findAvailableReelsBySize(tenantId, odInch, maxFatigue))
            .thenReturn(reels);

        List<CTReelDto> result = reelService.getAvailableReelsBySize(tenantId, odInch, maxFatigue);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(reelRepository, times(1)).findAvailableReelsBySize(tenantId, odInch, maxFatigue);
    }

    @Test
    void testGetReelsAboveFatigueThreshold_Success() {
        testReel.setAccumulatedFatiguePercent(BigDecimal.valueOf(85.0));
        List<CTReel> reels = Arrays.asList(testReel);
        BigDecimal threshold = BigDecimal.valueOf(80.0);

        when(reelRepository.findReelsAboveFatigueThreshold(tenantId, threshold))
            .thenReturn(reels);

        List<CTReelDto> result = reelService.getReelsAboveFatigueThreshold(tenantId, threshold);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getAccumulatedFatiguePercent().compareTo(threshold) > 0);
        verify(reelRepository, times(1)).findReelsAboveFatigueThreshold(tenantId, threshold);
    }

    @Test
    void testDelete_Success() {
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        doNothing().when(reelRepository).delete(testReel);

        assertDoesNotThrow(() -> reelService.delete(reelId));
        verify(reelRepository, times(1)).findById(reelId);
        verify(reelRepository, times(1)).delete(testReel);
    }

    @Test
    void testDelete_InUse() {
        testReel.setStatus(ReelStatus.IN_USE);
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        assertThrows(CTBusinessException.class, () -> reelService.delete(reelId));
        verify(reelRepository, times(1)).findById(reelId);
        verify(reelRepository, never()).delete(any(CTReel.class));
    }

    @Test
    void testCountByStatus_Success() {
        when(reelRepository.countByTenantIdAndStatus(tenantId, ReelStatus.AVAILABLE)).thenReturn(10L);

        long result = reelService.countByStatus(tenantId, ReelStatus.AVAILABLE);

        assertEquals(10L, result);
        verify(reelRepository, times(1)).countByTenantIdAndStatus(tenantId, ReelStatus.AVAILABLE);
    }
}
