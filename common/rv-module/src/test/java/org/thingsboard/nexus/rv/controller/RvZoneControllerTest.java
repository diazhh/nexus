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
package org.thingsboard.nexus.rv.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.nexus.rv.dto.RvZoneDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvZoneService;
import org.thingsboard.server.common.data.page.PageData;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for RvZoneController.
 * Tests REST endpoints for Zone CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RvZoneController Integration Tests")
class RvZoneControllerTest {

    @Mock
    private RvZoneService zoneService;

    @InjectMocks
    private RvZoneController zoneController;

    private UUID tenantId;
    private UUID zoneId;
    private UUID reservoirId;
    private RvZoneDto testZone;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        reservoirId = UUID.randomUUID();

        testZone = new RvZoneDto();
        testZone.setAssetId(zoneId);
        testZone.setTenantId(tenantId);
        testZone.setName("Test Zone");
        testZone.setCode("U-INF");
        testZone.setReservoirAssetId(reservoirId);
        testZone.setTopDepthMdM(new BigDecimal("2800"));
        testZone.setBottomDepthMdM(new BigDecimal("2830"));
        testZone.setGrossThicknessM(new BigDecimal("30"));
        testZone.setNetPayThicknessM(new BigDecimal("25"));
        testZone.setNetToGrossRatio(new BigDecimal("0.833"));
        testZone.setPorosityFrac(new BigDecimal("0.22"));
        testZone.setPermeabilityMd(new BigDecimal("150"));
    }

    @Test
    @DisplayName("POST /zones: Crear zone exitosamente")
    void testCreateZone_Success() {
        // Given
        RvZoneDto inputDto = new RvZoneDto();
        inputDto.setName("New Zone");
        inputDto.setReservoirAssetId(reservoirId);

        when(zoneService.createZone(eq(tenantId), any(RvZoneDto.class)))
                .thenReturn(testZone);

        // When
        ResponseEntity<RvZoneDto> response = zoneController.createZone(tenantId, inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testZone.getName(), response.getBody().getName());

        verify(zoneService, times(1)).createZone(eq(tenantId), any(RvZoneDto.class));
    }

    @Test
    @DisplayName("GET /zones/{id}: Obtener zone por ID")
    void testGetZoneById_Success() {
        // Given
        when(zoneService.getZoneById(zoneId))
                .thenReturn(Optional.of(testZone));

        // When
        ResponseEntity<RvZoneDto> response = zoneController.getZoneById(zoneId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(zoneId, response.getBody().getAssetId());

        verify(zoneService, times(1)).getZoneById(zoneId);
    }

    @Test
    @DisplayName("GET /zones/{id}: Zone no encontrado")
    void testGetZoneById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(zoneService.getZoneById(nonExistentId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(RvEntityNotFoundException.class, () -> {
            zoneController.getZoneById(nonExistentId);
        });

        verify(zoneService, times(1)).getZoneById(nonExistentId);
    }

    @Test
    @DisplayName("GET /zones: Listar todos los zones")
    void testGetAllZones_Success() {
        // Given
        List<RvZoneDto> zones = Arrays.asList(testZone);
        Page<RvZoneDto> page = new PageImpl<>(zones);

        when(zoneService.getAllZones(tenantId, 0, 20))
                .thenReturn(page);

        // When
        ResponseEntity<PageData<RvZoneDto>> response = zoneController.getAllZones(tenantId, 0, 20);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());

        verify(zoneService, times(1)).getAllZones(tenantId, 0, 20);
    }

    @Test
    @DisplayName("GET /zones/by-reservoir/{reservoirId}: Obtener zones por reservoir")
    void testGetZonesByReservoir_Success() {
        // Given
        List<RvZoneDto> zones = Arrays.asList(testZone);
        when(zoneService.getZonesByReservoir(tenantId, reservoirId))
                .thenReturn(zones);

        // When
        ResponseEntity<List<RvZoneDto>> response = zoneController.getZonesByReservoir(tenantId, reservoirId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(zoneService, times(1)).getZonesByReservoir(tenantId, reservoirId);
    }

    @Test
    @DisplayName("DELETE /zones/{id}: Eliminar zone exitosamente")
    void testDeleteZone_Success() {
        // Given
        doNothing().when(zoneService).deleteZone(tenantId, zoneId);

        // When
        ResponseEntity<Void> response = zoneController.deleteZone(tenantId, zoneId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(zoneService, times(1)).deleteZone(tenantId, zoneId);
    }

    @Test
    @DisplayName("Validar propiedades petrofísicas de Zone")
    void testZoneProperties() {
        // Given/When: testZone ya creado en setUp

        // Then: Validate calculated properties
        assertNotNull(testZone.getNetToGrossRatio());
        assertEquals(0, testZone.getNetToGrossRatio().compareTo(new BigDecimal("0.833")));

        // Net-to-Gross = Net thickness / Gross thickness
        BigDecimal calculatedNTG = testZone.getNetPayThicknessM()
                .divide(testZone.getGrossThicknessM(), 3, java.math.RoundingMode.HALF_UP);
        assertEquals(0, calculatedNTG.compareTo(testZone.getNetToGrossRatio()));
    }
}
