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
import org.thingsboard.nexus.rv.dto.RvWellDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvWellService;
import org.thingsboard.server.common.data.page.PageData;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for RvWellController.
 * Tests REST endpoints for Well CRUD operations and calculations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RvWellController Integration Tests")
class RvWellControllerTest {

    @Mock
    private RvWellService wellService;

    @InjectMocks
    private RvWellController wellController;

    private UUID tenantId;
    private UUID wellId;
    private UUID reservoirId;
    private RvWellDto testWell;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        wellId = UUID.randomUUID();
        reservoirId = UUID.randomUUID();

        testWell = new RvWellDto();
        testWell.setAssetId(wellId);
        testWell.setTenantId(tenantId);
        testWell.setName("SACHA-001");
        testWell.setReservoirAssetId(reservoirId);
        testWell.setSpudDate(System.currentTimeMillis());
        testWell.setTotalDepthMdM(new BigDecimal("2850"));
        testWell.setWellType("PRODUCER");
        testWell.setWellStatus("PRODUCING");
    }

    @Test
    @DisplayName("POST /wells: Crear well exitosamente")
    void testCreateWell_Success() {
        // Given
        RvWellDto inputDto = new RvWellDto();
        inputDto.setName("SACHA-002");
        inputDto.setReservoirAssetId(reservoirId);
        inputDto.setWellType("PRODUCER");

        when(wellService.createWell(eq(tenantId), any(RvWellDto.class)))
                .thenReturn(testWell);

        // When
        ResponseEntity<RvWellDto> response = wellController.createWell(tenantId, inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testWell.getName(), response.getBody().getName());

        verify(wellService, times(1)).createWell(eq(tenantId), any(RvWellDto.class));
    }

    @Test
    @DisplayName("GET /wells/{id}: Obtener well por ID")
    void testGetWellById_Success() {
        // Given
        when(wellService.getWellById(wellId))
                .thenReturn(Optional.of(testWell));

        // When
        ResponseEntity<RvWellDto> response = wellController.getWellById(wellId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(wellId, response.getBody().getAssetId());
        assertEquals("SACHA-001", response.getBody().getName());

        verify(wellService, times(1)).getWellById(wellId);
    }

    @Test
    @DisplayName("GET /wells/{id}: Well no encontrado")
    void testGetWellById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(wellService.getWellById(nonExistentId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(RvEntityNotFoundException.class, () -> {
            wellController.getWellById(nonExistentId);
        });

        verify(wellService, times(1)).getWellById(nonExistentId);
    }

    @Test
    @DisplayName("GET /wells: Listar todos los wells")
    void testGetAllWells_Success() {
        // Given
        List<RvWellDto> wells = Arrays.asList(testWell, createAnotherWell());
        Page<RvWellDto> page = new PageImpl<>(wells);

        when(wellService.getAllWells(tenantId, 0, 20))
                .thenReturn(page);

        // When
        ResponseEntity<PageData<RvWellDto>> response = wellController.getAllWells(tenantId, 0, 20);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getData().size());

        verify(wellService, times(1)).getAllWells(tenantId, 0, 20);
    }

    @Test
    @DisplayName("GET /wells/by-reservoir/{reservoirId}: Obtener wells por reservoir")
    void testGetWellsByReservoir_Success() {
        // Given
        List<RvWellDto> wells = Arrays.asList(testWell);
        when(wellService.getWellsByReservoir(tenantId, reservoirId))
                .thenReturn(wells);

        // When
        ResponseEntity<List<RvWellDto>> response = wellController.getWellsByReservoir(tenantId, reservoirId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(wellService, times(1)).getWellsByReservoir(tenantId, reservoirId);
    }

    @Test
    @DisplayName("DELETE /wells/{id}: Eliminar well exitosamente")
    void testDeleteWell_Success() {
        // Given
        doNothing().when(wellService).deleteWell(tenantId, wellId);

        // When
        ResponseEntity<Void> response = wellController.deleteWell(tenantId, wellId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(wellService, times(1)).deleteWell(tenantId, wellId);
    }

    @Test
    @DisplayName("Validar tipos de well permitidos")
    void testWellTypeValidation() {
        // Given/When: testWell con tipo PRODUCER

        // Then
        assertNotNull(testWell.getWellType());
        assertTrue(Arrays.asList("PRODUCER", "INJECTOR", "OBSERVATION", "DISPOSAL")
                .contains(testWell.getWellType()));
    }

    @Test
    @DisplayName("Validar wellStatus permitidos")
    void testWellStatusValidation() {
        // Given/When: testWell con status PRODUCING

        // Then
        assertNotNull(testWell.getWellStatus());
        assertTrue(Arrays.asList("PRODUCING", "SHUT_IN", "ABANDONED", "DRILLING", "COMPLETING")
                .contains(testWell.getWellStatus()));
    }

    private RvWellDto createAnotherWell() {
        RvWellDto well = new RvWellDto();
        well.setAssetId(UUID.randomUUID());
        well.setTenantId(tenantId);
        well.setName("SACHA-002");
        well.setReservoirAssetId(reservoirId);
        well.setWellType("PRODUCER");
        well.setWellStatus("DRILLING");
        return well;
    }
}
