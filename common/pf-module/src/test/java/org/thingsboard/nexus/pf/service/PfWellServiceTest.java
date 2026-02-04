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
package org.thingsboard.nexus.pf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.thingsboard.nexus.pf.dto.LiftSystemType;
import org.thingsboard.nexus.pf.dto.PfWellDto;
import org.thingsboard.nexus.pf.dto.WellStatus;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PfWellService.
 */
@ExtendWith(MockitoExtension.class)
class PfWellServiceTest {

    @Mock
    private PfAssetService pfAssetService;

    @Mock
    private PfAttributeService pfAttributeService;

    @Mock
    private PfHierarchyService pfHierarchyService;

    @InjectMocks
    private PfWellService wellService;

    private UUID tenantId;
    private UUID wellAssetId;
    private UUID wellpadId;
    private Asset wellAsset;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        wellAssetId = UUID.randomUUID();
        wellpadId = UUID.randomUUID();

        wellAsset = new Asset();
        wellAsset.setId(new AssetId(wellAssetId));
        wellAsset.setTenantId(TenantId.fromUUID(tenantId));
        wellAsset.setName("TEST-WELL-001");
        wellAsset.setType(PfWellDto.ASSET_TYPE);
        wellAsset.setCreatedTime(System.currentTimeMillis());
    }

    @Nested
    @DisplayName("createWell tests")
    class CreateWellTests {

        @Test
        @DisplayName("Should create well successfully")
        void shouldCreateWellSuccessfully() {
            PfWellDto dto = createWellDto();
            when(pfAssetService.existsById(wellpadId)).thenReturn(true);
            when(pfAssetService.createAsset(eq(tenantId), eq(PfWellDto.ASSET_TYPE), anyString(), anyString()))
                    .thenReturn(wellAsset);

            PfWellDto result = wellService.createWell(tenantId, dto);

            assertNotNull(result);
            assertEquals(wellAssetId, result.getAssetId());
            assertEquals(tenantId, result.getTenantId());
            verify(pfAssetService).createAsset(eq(tenantId), eq(PfWellDto.ASSET_TYPE), anyString(), anyString());
            verify(pfAttributeService).saveServerAttributes(eq(wellAssetId), anyMap());
            verify(pfHierarchyService).setParentChild(tenantId, wellpadId, wellAssetId);
        }

        @Test
        @DisplayName("Should create well without wellpad")
        void shouldCreateWellWithoutWellpad() {
            PfWellDto dto = createWellDto();
            dto.setWellpadId(null);
            when(pfAssetService.createAsset(eq(tenantId), eq(PfWellDto.ASSET_TYPE), anyString(), anyString()))
                    .thenReturn(wellAsset);

            PfWellDto result = wellService.createWell(tenantId, dto);

            assertNotNull(result);
            verify(pfHierarchyService, never()).setParentChild(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when wellpad not found")
        void shouldThrowExceptionWhenWellpadNotFound() {
            PfWellDto dto = createWellDto();
            when(pfAssetService.existsById(wellpadId)).thenReturn(false);

            assertThrows(PfEntityNotFoundException.class, () -> wellService.createWell(tenantId, dto));
            verify(pfAssetService, never()).createAsset(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should save all well attributes")
        void shouldSaveAllWellAttributes() {
            PfWellDto dto = createWellDto();
            dto.setApiNumber("42-001-12345");
            dto.setStatus(WellStatus.PRODUCING);
            dto.setLiftSystemType(LiftSystemType.ESP);
            dto.setLatitude(BigDecimal.valueOf(29.5));
            dto.setLongitude(BigDecimal.valueOf(-95.5));
            dto.setMeasuredDepthFt(BigDecimal.valueOf(10000));
            dto.setTrueVerticalDepthFt(BigDecimal.valueOf(9500));
            dto.setSpudDate(LocalDate.of(2020, 1, 15));
            dto.setFirstProductionDate(LocalDate.of(2020, 6, 1));
            dto.setCurrentProductionBpd(BigDecimal.valueOf(500));

            when(pfAssetService.existsById(wellpadId)).thenReturn(true);
            when(pfAssetService.createAsset(eq(tenantId), eq(PfWellDto.ASSET_TYPE), anyString(), anyString()))
                    .thenReturn(wellAsset);

            wellService.createWell(tenantId, dto);

            ArgumentCaptor<Map<String, Object>> attrsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(pfAttributeService).saveServerAttributes(eq(wellAssetId), attrsCaptor.capture());

            Map<String, Object> savedAttrs = attrsCaptor.getValue();
            assertEquals("42-001-12345", savedAttrs.get(PfWellDto.ATTR_API_NUMBER));
            assertEquals("PRODUCING", savedAttrs.get(PfWellDto.ATTR_STATUS));
            assertEquals("ESP", savedAttrs.get(PfWellDto.ATTR_LIFT_SYSTEM_TYPE));
        }
    }

    @Nested
    @DisplayName("getWellById tests")
    class GetWellByIdTests {

        @Test
        @DisplayName("Should get well by ID")
        void shouldGetWellById() {
            when(pfAssetService.getAssetById(wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(pfAttributeService.getServerAttributes(wellAssetId)).thenReturn(createAttributeEntries());
            when(pfHierarchyService.getParent(tenantId, wellAssetId)).thenReturn(wellpadId);

            Optional<PfWellDto> result = wellService.getWellById(wellAssetId);

            assertTrue(result.isPresent());
            assertEquals(wellAssetId, result.get().getAssetId());
            assertEquals("TEST-WELL-001", result.get().getName());
            assertEquals(wellpadId, result.get().getWellpadId());
        }

        @Test
        @DisplayName("Should return empty when well not found")
        void shouldReturnEmptyWhenNotFound() {
            when(pfAssetService.getAssetById(wellAssetId)).thenReturn(Optional.empty());

            Optional<PfWellDto> result = wellService.getWellById(wellAssetId);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty when asset is not a well")
        void shouldReturnEmptyWhenNotWell() {
            wellAsset.setType("NOT_A_WELL");
            when(pfAssetService.getAssetById(wellAssetId)).thenReturn(Optional.of(wellAsset));

            Optional<PfWellDto> result = wellService.getWellById(wellAssetId);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should load all well attributes")
        void shouldLoadAllWellAttributes() {
            when(pfAssetService.getAssetById(wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(pfAttributeService.getServerAttributes(wellAssetId)).thenReturn(createFullAttributeEntries());
            when(pfHierarchyService.getParent(tenantId, wellAssetId)).thenReturn(wellpadId);

            Optional<PfWellDto> result = wellService.getWellById(wellAssetId);

            assertTrue(result.isPresent());
            PfWellDto well = result.get();
            assertEquals("42-001-12345", well.getApiNumber());
            assertEquals(WellStatus.PRODUCING, well.getStatus());
            assertEquals(LiftSystemType.ESP, well.getLiftSystemType());
            assertEquals(BigDecimal.valueOf(29.5), well.getLatitude());
            assertEquals(BigDecimal.valueOf(-95.5), well.getLongitude());
        }
    }

    @Nested
    @DisplayName("getAllWells tests")
    class GetAllWellsTests {

        @Test
        @DisplayName("Should get all wells for tenant")
        void shouldGetAllWellsForTenant() {
            List<Asset> assets = List.of(wellAsset, createWellAsset("WELL-002"));
            Page<Asset> assetPage = new PageImpl<>(assets);
            when(pfAssetService.getAssetsByType(eq(tenantId), eq(PfWellDto.ASSET_TYPE), anyInt(), anyInt()))
                    .thenReturn(assetPage);
            when(pfAttributeService.getServerAttributes(any())).thenReturn(new ArrayList<>());

            Page<PfWellDto> result = wellService.getAllWells(tenantId, 0, 20);

            assertEquals(2, result.getContent().size());
            verify(pfAssetService).getAssetsByType(tenantId, PfWellDto.ASSET_TYPE, 0, 20);
        }

        @Test
        @DisplayName("Should return empty page when no wells")
        void shouldReturnEmptyPageWhenNoWells() {
            Page<Asset> emptyPage = new PageImpl<>(new ArrayList<>());
            when(pfAssetService.getAssetsByType(eq(tenantId), eq(PfWellDto.ASSET_TYPE), anyInt(), anyInt()))
                    .thenReturn(emptyPage);

            Page<PfWellDto> result = wellService.getAllWells(tenantId, 0, 20);

            assertTrue(result.getContent().isEmpty());
        }
    }

    @Nested
    @DisplayName("getWellsByWellpad tests")
    class GetWellsByWellpadTests {

        @Test
        @DisplayName("Should get wells by wellpad")
        void shouldGetWellsByWellpad() {
            UUID well1Id = UUID.randomUUID();
            UUID well2Id = UUID.randomUUID();

            Asset well1 = createWellAsset("WELL-001", well1Id);
            Asset well2 = createWellAsset("WELL-002", well2Id);

            when(pfHierarchyService.getChildren(tenantId, wellpadId)).thenReturn(List.of(well1Id, well2Id));
            when(pfAssetService.getAssetById(well1Id)).thenReturn(Optional.of(well1));
            when(pfAssetService.getAssetById(well2Id)).thenReturn(Optional.of(well2));
            when(pfAttributeService.getServerAttributes(any())).thenReturn(new ArrayList<>());
            when(pfHierarchyService.getParent(any(), any())).thenReturn(wellpadId);

            List<PfWellDto> result = wellService.getWellsByWellpad(tenantId, wellpadId);

            assertEquals(2, result.size());
            verify(pfHierarchyService).getChildren(tenantId, wellpadId);
        }

        @Test
        @DisplayName("Should return empty list when no wells in wellpad")
        void shouldReturnEmptyListWhenNoWells() {
            when(pfHierarchyService.getChildren(tenantId, wellpadId)).thenReturn(new ArrayList<>());

            List<PfWellDto> result = wellService.getWellsByWellpad(tenantId, wellpadId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getWellsByStatus tests")
    class GetWellsByStatusTests {

        @Test
        @DisplayName("Should filter wells by status")
        void shouldFilterWellsByStatus() {
            Asset producingWell = createWellAsset("WELL-PRODUCING");
            Asset shutInWell = createWellAsset("WELL-SHUTIN");

            List<Asset> assets = List.of(producingWell, shutInWell);
            Page<Asset> assetPage = new PageImpl<>(assets);
            when(pfAssetService.getAssetsByType(eq(tenantId), eq(PfWellDto.ASSET_TYPE), anyInt(), anyInt()))
                    .thenReturn(assetPage);

            // First well is PRODUCING
            List<AttributeKvEntry> producingAttrs = List.of(
                    new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_STATUS, "PRODUCING"), 0)
            );
            when(pfAttributeService.getServerAttributes(producingWell.getId().getId())).thenReturn(producingAttrs);

            // Second well is SHUT_IN
            List<AttributeKvEntry> shutInAttrs = List.of(
                    new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_STATUS, "SHUT_IN"), 0)
            );
            when(pfAttributeService.getServerAttributes(shutInWell.getId().getId())).thenReturn(shutInAttrs);

            List<PfWellDto> result = wellService.getWellsByStatus(tenantId, WellStatus.PRODUCING, 0, 20);

            assertEquals(1, result.size());
            assertEquals(WellStatus.PRODUCING, result.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("updateWell tests")
    class UpdateWellTests {

        @Test
        @DisplayName("Should update well")
        void shouldUpdateWell() {
            PfWellDto dto = createWellDto();
            dto.setAssetId(wellAssetId);
            dto.setName("UPDATED-WELL");
            when(pfAssetService.getAssetById(wellAssetId)).thenReturn(Optional.of(wellAsset));

            PfWellDto result = wellService.updateWell(dto);

            assertNotNull(result);
            assertNotNull(result.getUpdatedTime());
            verify(pfAssetService).updateAsset(any());
            verify(pfAttributeService).saveServerAttributes(eq(wellAssetId), anyMap());
        }

        @Test
        @DisplayName("Should not update asset if name unchanged")
        void shouldNotUpdateAssetIfNameUnchanged() {
            PfWellDto dto = createWellDto();
            dto.setAssetId(wellAssetId);
            dto.setName("TEST-WELL-001"); // Same as wellAsset.getName()
            when(pfAssetService.getAssetById(wellAssetId)).thenReturn(Optional.of(wellAsset));

            wellService.updateWell(dto);

            verify(pfAssetService, never()).updateAsset(any());
        }
    }

    @Nested
    @DisplayName("updateWellStatus tests")
    class UpdateWellStatusTests {

        @Test
        @DisplayName("Should update well status")
        void shouldUpdateWellStatus() {
            wellService.updateWellStatus(wellAssetId, WellStatus.SHUT_IN);

            ArgumentCaptor<Map<String, Object>> attrsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(pfAttributeService).saveServerAttributes(eq(wellAssetId), attrsCaptor.capture());

            Map<String, Object> savedAttrs = attrsCaptor.getValue();
            assertEquals("SHUT_IN", savedAttrs.get(PfWellDto.ATTR_STATUS));
        }
    }

    @Nested
    @DisplayName("updateProductionRate tests")
    class UpdateProductionRateTests {

        @Test
        @DisplayName("Should update production rate")
        void shouldUpdateProductionRate() {
            BigDecimal newRate = BigDecimal.valueOf(750);

            wellService.updateProductionRate(wellAssetId, newRate);

            ArgumentCaptor<Map<String, Object>> attrsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(pfAttributeService).saveServerAttributes(eq(wellAssetId), attrsCaptor.capture());

            Map<String, Object> savedAttrs = attrsCaptor.getValue();
            assertEquals(newRate, savedAttrs.get(PfWellDto.ATTR_CURRENT_PRODUCTION_BPD));
        }
    }

    @Nested
    @DisplayName("deleteWell tests")
    class DeleteWellTests {

        @Test
        @DisplayName("Should delete well and its relations")
        void shouldDeleteWellAndRelations() {
            wellService.deleteWell(tenantId, wellAssetId);

            verify(pfHierarchyService).removeAllRelations(tenantId, wellAssetId);
            verify(pfAssetService).deleteAsset(tenantId, wellAssetId);
        }
    }

    @Nested
    @DisplayName("countWellsByStatus tests")
    class CountWellsByStatusTests {

        @Test
        @DisplayName("Should count wells by status")
        void shouldCountWellsByStatus() {
            Asset well1 = createWellAsset("WELL-1");
            Asset well2 = createWellAsset("WELL-2");
            Asset well3 = createWellAsset("WELL-3");

            Page<Asset> assetPage = new PageImpl<>(List.of(well1, well2, well3));
            when(pfAssetService.getAssetsByType(eq(tenantId), eq(PfWellDto.ASSET_TYPE), anyInt(), anyInt()))
                    .thenReturn(assetPage);

            // 2 producing, 1 shut_in
            when(pfAttributeService.getServerAttributes(well1.getId().getId()))
                    .thenReturn(List.of(new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_STATUS, "PRODUCING"), 0)));
            when(pfAttributeService.getServerAttributes(well2.getId().getId()))
                    .thenReturn(List.of(new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_STATUS, "PRODUCING"), 0)));
            when(pfAttributeService.getServerAttributes(well3.getId().getId()))
                    .thenReturn(List.of(new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_STATUS, "SHUT_IN"), 0)));

            Map<WellStatus, Long> result = wellService.countWellsByStatus(tenantId);

            assertEquals(2L, result.get(WellStatus.PRODUCING));
            assertEquals(1L, result.get(WellStatus.SHUT_IN));
            assertEquals(0L, result.get(WellStatus.INACTIVE));
        }
    }

    // Helper methods

    private PfWellDto createWellDto() {
        PfWellDto dto = new PfWellDto();
        dto.setName("TEST-WELL-001");
        dto.setWellpadId(wellpadId);
        dto.setStatus(WellStatus.PRODUCING);
        dto.setLiftSystemType(LiftSystemType.ESP);
        return dto;
    }

    private Asset createWellAsset(String name) {
        return createWellAsset(name, UUID.randomUUID());
    }

    private Asset createWellAsset(String name, UUID id) {
        Asset asset = new Asset();
        asset.setId(new AssetId(id));
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setName(name);
        asset.setType(PfWellDto.ASSET_TYPE);
        asset.setCreatedTime(System.currentTimeMillis());
        return asset;
    }

    private List<AttributeKvEntry> createAttributeEntries() {
        List<AttributeKvEntry> entries = new ArrayList<>();
        entries.add(new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_STATUS, "PRODUCING"), 0));
        return entries;
    }

    private List<AttributeKvEntry> createFullAttributeEntries() {
        List<AttributeKvEntry> entries = new ArrayList<>();
        entries.add(new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_API_NUMBER, "42-001-12345"), 0));
        entries.add(new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_STATUS, "PRODUCING"), 0));
        entries.add(new BaseAttributeKvEntry(new StringDataEntry(PfWellDto.ATTR_LIFT_SYSTEM_TYPE, "ESP"), 0));
        entries.add(new BaseAttributeKvEntry(new DoubleDataEntry(PfWellDto.ATTR_LATITUDE, 29.5), 0));
        entries.add(new BaseAttributeKvEntry(new DoubleDataEntry(PfWellDto.ATTR_LONGITUDE, -95.5), 0));
        entries.add(new BaseAttributeKvEntry(new DoubleDataEntry(PfWellDto.ATTR_CURRENT_PRODUCTION_BPD, 500.0), 0));
        return entries;
    }
}
