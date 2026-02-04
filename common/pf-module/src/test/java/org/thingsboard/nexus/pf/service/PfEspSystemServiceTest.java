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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.thingsboard.nexus.pf.dto.PfEspSystemDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PfEspSystemService.
 */
@ExtendWith(MockitoExtension.class)
class PfEspSystemServiceTest {

    @Mock
    private PfAssetService pfAssetService;

    @Mock
    private PfAttributeService pfAttributeService;

    @Mock
    private PfHierarchyService pfHierarchyService;

    @Mock
    private PfAlarmService alarmService;

    @InjectMocks
    private PfEspSystemService espSystemService;

    private UUID tenantId;
    private UUID assetId;
    private UUID wellId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        wellId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createEspSystem tests")
    class CreateEspSystemTests {

        @Test
        @DisplayName("Should create ESP system successfully")
        void shouldCreateEspSystemSuccessfully() {
            PfEspSystemDto dto = createEspDto();
            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_esp_system");

            when(pfAssetService.existsById(wellId)).thenReturn(true);
            when(pfAssetService.createAsset(eq(tenantId), eq("pf_esp_system"), anyString(), anyString()))
                    .thenReturn(mockAsset);

            PfEspSystemDto result = espSystemService.createEspSystem(tenantId, dto);

            assertNotNull(result);
            assertEquals(assetId, result.getAssetId());
            assertEquals(tenantId, result.getTenantId());

            verify(pfAssetService).createAsset(eq(tenantId), eq("pf_esp_system"), anyString(), anyString());
            verify(pfHierarchyService).createInstalledInRelation(tenantId, assetId, wellId);
            verify(pfAttributeService).saveServerAttributes(eq(assetId), anyMap());
        }

        @Test
        @DisplayName("Should throw exception when well not found")
        void shouldThrowExceptionWhenWellNotFound() {
            PfEspSystemDto dto = createEspDto();

            when(pfAssetService.existsById(wellId)).thenReturn(false);

            assertThrows(PfEntityNotFoundException.class, () ->
                    espSystemService.createEspSystem(tenantId, dto));

            verify(pfAssetService, never()).createAsset(any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should calculate run life from installation date")
        void shouldCalculateRunLifeFromInstallationDate() {
            PfEspSystemDto dto = createEspDto();
            dto.setInstallationDate(LocalDate.now().minusDays(100));

            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_esp_system");

            when(pfAssetService.existsById(wellId)).thenReturn(true);
            when(pfAssetService.createAsset(eq(tenantId), eq("pf_esp_system"), anyString(), anyString()))
                    .thenReturn(mockAsset);

            PfEspSystemDto result = espSystemService.createEspSystem(tenantId, dto);

            assertEquals(100, result.getRunLifeDays());
        }
    }

    @Nested
    @DisplayName("getEspSystemById tests")
    class GetEspSystemByIdTests {

        @Test
        @DisplayName("Should return ESP system when found")
        void shouldReturnEspSystemWhenFound() {
            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_esp_system");
            List<AttributeKvEntry> attributes = createMockAttributes();

            when(pfAssetService.getAssetById(assetId)).thenReturn(Optional.of(mockAsset));
            when(pfAttributeService.getServerAttributes(assetId)).thenReturn(attributes);

            Optional<PfEspSystemDto> result = espSystemService.getEspSystemById(assetId);

            assertTrue(result.isPresent());
            assertEquals(assetId, result.get().getAssetId());
            assertEquals("ESP-2000", result.get().getPumpModel());
        }

        @Test
        @DisplayName("Should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(pfAssetService.getAssetById(assetId)).thenReturn(Optional.empty());

            Optional<PfEspSystemDto> result = espSystemService.getEspSystemById(assetId);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty when wrong asset type")
        void shouldReturnEmptyWhenWrongAssetType() {
            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_well");

            when(pfAssetService.getAssetById(assetId)).thenReturn(Optional.of(mockAsset));

            Optional<PfEspSystemDto> result = espSystemService.getEspSystemById(assetId);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("getEspSystemByWell tests")
    class GetEspSystemByWellTests {

        @Test
        @DisplayName("Should return ESP system for well")
        void shouldReturnEspSystemForWell() {
            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_esp_system");
            List<AttributeKvEntry> attributes = createMockAttributes();

            when(pfHierarchyService.getEquipmentInWell(tenantId, wellId)).thenReturn(List.of(assetId));
            when(pfAssetService.getAssetById(assetId)).thenReturn(Optional.of(mockAsset));
            when(pfAttributeService.getServerAttributes(assetId)).thenReturn(attributes);

            Optional<PfEspSystemDto> result = espSystemService.getEspSystemByWell(tenantId, wellId);

            assertTrue(result.isPresent());
            assertEquals(assetId, result.get().getAssetId());
        }

        @Test
        @DisplayName("Should return empty when no equipment in well")
        void shouldReturnEmptyWhenNoEquipmentInWell() {
            when(pfHierarchyService.getEquipmentInWell(tenantId, wellId)).thenReturn(new ArrayList<>());

            Optional<PfEspSystemDto> result = espSystemService.getEspSystemByWell(tenantId, wellId);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("getAllEspSystems tests")
    class GetAllEspSystemsTests {

        @Test
        @DisplayName("Should return paginated ESP systems")
        void shouldReturnPaginatedEspSystems() {
            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_esp_system");
            Page<Asset> assetPage = new PageImpl<>(List.of(mockAsset));

            when(pfAssetService.getAssetsByType(eq(tenantId), eq("pf_esp_system"), eq(0), eq(10)))
                    .thenReturn(assetPage);
            when(pfAttributeService.getServerAttributes(assetId)).thenReturn(createMockAttributes());

            Page<PfEspSystemDto> result = espSystemService.getAllEspSystems(tenantId, 0, 10);

            assertEquals(1, result.getTotalElements());
            assertEquals(assetId, result.getContent().get(0).getAssetId());
        }
    }

    @Nested
    @DisplayName("updateEspSystem tests")
    class UpdateEspSystemTests {

        @Test
        @DisplayName("Should update ESP system")
        void shouldUpdateEspSystem() {
            PfEspSystemDto dto = createEspDto();
            dto.setAssetId(assetId);
            dto.setFrequencyHz(BigDecimal.valueOf(55.0));

            PfEspSystemDto result = espSystemService.updateEspSystem(dto);

            assertNotNull(result);
            assertNotNull(result.getUpdatedTime());
            verify(pfAttributeService).saveServerAttributes(eq(assetId), anyMap());
        }
    }

    @Nested
    @DisplayName("recordPulling tests")
    class RecordPullingTests {

        @Test
        @DisplayName("Should record pulling date")
        void shouldRecordPullingDate() {
            LocalDate pullingDate = LocalDate.now();

            espSystemService.recordPulling(assetId, pullingDate);

            verify(pfAttributeService).saveServerAttributes(eq(assetId), anyMap());
        }
    }

    @Nested
    @DisplayName("getHealthStatus tests")
    class GetHealthStatusTests {

        @Test
        @DisplayName("Should return GOOD health for new ESP")
        void shouldReturnGoodHealthForNewEsp() {
            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_esp_system");
            List<AttributeKvEntry> attributes = createMockAttributesWithRunLife(100);

            when(pfAssetService.getAssetById(assetId)).thenReturn(Optional.of(mockAsset));
            when(pfAttributeService.getServerAttributes(assetId)).thenReturn(attributes);

            PfEspSystemService.EspHealthStatus status = espSystemService.getHealthStatus(assetId);

            assertEquals("GOOD", status.status());
            assertEquals(100, status.healthScore());
        }

        @Test
        @DisplayName("Should return reduced health for old ESP")
        void shouldReturnReducedHealthForOldEsp() {
            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_esp_system");
            List<AttributeKvEntry> attributes = createMockAttributesWithRunLife(400);

            when(pfAssetService.getAssetById(assetId)).thenReturn(Optional.of(mockAsset));
            when(pfAttributeService.getServerAttributes(assetId)).thenReturn(attributes);

            PfEspSystemService.EspHealthStatus status = espSystemService.getHealthStatus(assetId);

            assertEquals("GOOD", status.status());
            assertEquals(90, status.healthScore());
        }

        @Test
        @DisplayName("Should return ATTENTION for very old ESP")
        void shouldReturnAttentionForVeryOldEsp() {
            Asset mockAsset = createMockAsset(assetId, tenantId, "pf_esp_system");
            List<AttributeKvEntry> attributes = createMockAttributesWithRunLife(800);

            when(pfAssetService.getAssetById(assetId)).thenReturn(Optional.of(mockAsset));
            when(pfAttributeService.getServerAttributes(assetId)).thenReturn(attributes);

            PfEspSystemService.EspHealthStatus status = espSystemService.getHealthStatus(assetId);

            assertEquals("ATTENTION", status.status());
            assertEquals(70, status.healthScore());
        }

        @Test
        @DisplayName("Should return UNKNOWN when ESP not found")
        void shouldReturnUnknownWhenEspNotFound() {
            when(pfAssetService.getAssetById(assetId)).thenReturn(Optional.empty());

            PfEspSystemService.EspHealthStatus status = espSystemService.getHealthStatus(assetId);

            assertEquals("UNKNOWN", status.status());
            assertEquals(0, status.healthScore());
        }
    }

    @Nested
    @DisplayName("deleteEspSystem tests")
    class DeleteEspSystemTests {

        @Test
        @DisplayName("Should delete ESP system and relations")
        void shouldDeleteEspSystemAndRelations() {
            espSystemService.deleteEspSystem(tenantId, assetId);

            verify(pfHierarchyService).removeAllRelations(tenantId, assetId);
            verify(pfAssetService).deleteAsset(tenantId, assetId);
        }
    }

    // Helper methods

    private PfEspSystemDto createEspDto() {
        PfEspSystemDto dto = new PfEspSystemDto();
        dto.setWellId(wellId);
        dto.setPumpModel("ESP-2000");
        dto.setPumpSerialNumber("SN-12345");
        dto.setStages(100);
        dto.setRatedHeadFt(BigDecimal.valueOf(5000));
        dto.setRatedFlowBpd(BigDecimal.valueOf(1500));
        dto.setMotorHp(BigDecimal.valueOf(200));
        dto.setMotorVoltage(480);
        dto.setFrequencyHz(BigDecimal.valueOf(60));
        dto.setMaxFrequencyHz(BigDecimal.valueOf(65));
        dto.setMinFrequencyHz(BigDecimal.valueOf(40));
        dto.setMaxCurrentAmps(BigDecimal.valueOf(50));
        return dto;
    }

    private Asset createMockAsset(UUID assetId, UUID tenantId, String type) {
        Asset asset = new Asset();
        asset.setId(new AssetId(assetId));
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setType(type);
        asset.setName("ESP-SN-12345");
        asset.setCreatedTime(System.currentTimeMillis());
        return asset;
    }

    private List<AttributeKvEntry> createMockAttributes() {
        List<AttributeKvEntry> attrs = new ArrayList<>();
        attrs.add(new BaseAttributeKvEntry(new StringDataEntry("pump_model", "ESP-2000"), System.currentTimeMillis()));
        attrs.add(new BaseAttributeKvEntry(new StringDataEntry("pump_serial_number", "SN-12345"), System.currentTimeMillis()));
        attrs.add(new BaseAttributeKvEntry(new LongDataEntry("stages", 100L), System.currentTimeMillis()));
        return attrs;
    }

    private List<AttributeKvEntry> createMockAttributesWithRunLife(int runLifeDays) {
        List<AttributeKvEntry> attrs = createMockAttributes();
        attrs.add(new BaseAttributeKvEntry(new LongDataEntry("run_life_days", (long) runLifeDays), System.currentTimeMillis()));
        attrs.add(new BaseAttributeKvEntry(new StringDataEntry("installation_date",
                LocalDate.now().minusDays(runLifeDays).toString()), System.currentTimeMillis()));
        return attrs;
    }
}
