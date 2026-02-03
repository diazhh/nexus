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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.thingsboard.nexus.rv.dto.RvReservoirDto;
import org.thingsboard.nexus.rv.exception.RvBusinessException;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvReservoirService;
import org.thingsboard.server.common.data.page.PageData;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for RvReservoirController.
 * Tests REST endpoints for Reservoir CRUD operations and calculations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RvReservoirController Integration Tests")
class RvReservoirControllerTest {

    @Mock
    private RvReservoirService reservoirService;

    @InjectMocks
    private RvReservoirController reservoirController;

    private UUID tenantId;
    private UUID reservoirId;
    private UUID fieldId;
    private RvReservoirDto testReservoir;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        reservoirId = UUID.randomUUID();
        fieldId = UUID.randomUUID();
        objectMapper = new ObjectMapper();

        // Create test reservoir DTO
        testReservoir = new RvReservoirDto();
        testReservoir.setAssetId(reservoirId);
        testReservoir.setTenantId(tenantId);
        testReservoir.setName("Test Reservoir");
        testReservoir.setFieldAssetId(fieldId);
        testReservoir.setFormationName("Hollín");
        testReservoir.setAveragePorosityFrac(new BigDecimal("0.22"));
        testReservoir.setAveragePermeabilityMd(new BigDecimal("150"));
        testReservoir.setNetPayThicknessM(new BigDecimal("25"));
        testReservoir.setInitialPressurePsi(new BigDecimal("3000"));
        testReservoir.setTemperatureF(new BigDecimal("180"));
        testReservoir.setFluidType("BLACK_OIL");
        testReservoir.setApiGravity(new BigDecimal("32"));
        testReservoir.setOoipStb(new BigDecimal("50000000")); // 50 MMbbl
    }

    // ===========================================
    // CREATE TESTS
    // ===========================================

    @Test
    @DisplayName("POST /reservoirs: Crear reservoir exitosamente")
    void testCreateReservoir_Success() {
        // Given
        RvReservoirDto inputDto = new RvReservoirDto();
        inputDto.setName("New Reservoir");
        inputDto.setFieldAssetId(fieldId);

        when(reservoirService.createReservoir(eq(tenantId), any(RvReservoirDto.class)))
                .thenReturn(testReservoir);

        // When
        ResponseEntity<RvReservoirDto> response = reservoirController.createReservoir(tenantId, inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testReservoir.getName(), response.getBody().getName());

        verify(reservoirService, times(1)).createReservoir(eq(tenantId), any(RvReservoirDto.class));
    }

    @Test
    @DisplayName("POST /reservoirs: Validación de campos requeridos")
    void testCreateReservoir_ValidationFailure() {
        // Given: Reservoir with missing required fields
        RvReservoirDto invalidDto = new RvReservoirDto();
        // Missing name and fieldId

        when(reservoirService.createReservoir(eq(tenantId), any(RvReservoirDto.class)))
                .thenThrow(new IllegalArgumentException("Name is required"));

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            reservoirController.createReservoir(tenantId, invalidDto);
        });
    }

    @Test
    @DisplayName("POST /reservoirs: Field no existe")
    void testCreateReservoir_FieldNotFound() {
        // Given
        RvReservoirDto inputDto = new RvReservoirDto();
        inputDto.setName("New Reservoir");
        inputDto.setFieldAssetId(UUID.randomUUID());

        when(reservoirService.createReservoir(eq(tenantId), any(RvReservoirDto.class)))
                .thenThrow(new RvEntityNotFoundException("Field", inputDto.getFieldAssetId()));

        // When/Then
        assertThrows(RvEntityNotFoundException.class, () -> {
            reservoirController.createReservoir(tenantId, inputDto);
        });
    }

    // ===========================================
    // READ TESTS
    // ===========================================

    @Test
    @DisplayName("GET /reservoirs/{id}: Obtener reservoir por ID")
    void testGetReservoirById_Success() {
        // Given
        when(reservoirService.getReservoirById(reservoirId))
                .thenReturn(Optional.of(testReservoir));

        // When
        ResponseEntity<RvReservoirDto> response = reservoirController.getReservoirById(reservoirId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(reservoirId, response.getBody().getAssetId());
        assertEquals("Test Reservoir", response.getBody().getName());

        verify(reservoirService, times(1)).getReservoirById(reservoirId);
    }

    @Test
    @DisplayName("GET /reservoirs/{id}: Reservoir no encontrado")
    void testGetReservoirById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(reservoirService.getReservoirById(nonExistentId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(RvEntityNotFoundException.class, () -> {
            reservoirController.getReservoirById(nonExistentId);
        });

        verify(reservoirService, times(1)).getReservoirById(nonExistentId);
    }

    @Test
    @DisplayName("GET /reservoirs: Listar todos los reservoirs paginados")
    void testGetAllReservoirs_Success() {
        // Given
        List<RvReservoirDto> reservoirs = Arrays.asList(testReservoir, createAnotherReservoir());
        Page<RvReservoirDto> page = new PageImpl<>(reservoirs);

        when(reservoirService.getAllReservoirs(tenantId, 0, 20))
                .thenReturn(page);

        // When
        ResponseEntity<PageData<RvReservoirDto>> response = reservoirController.getAllReservoirs(tenantId, 0, 20);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getData().size());

        verify(reservoirService, times(1)).getAllReservoirs(tenantId, 0, 20);
    }

    @Test
    @DisplayName("GET /reservoirs: Lista vacía")
    void testGetAllReservoirs_EmptyList() {
        // Given
        Page<RvReservoirDto> emptyPage = new PageImpl<>(Collections.emptyList());
        when(reservoirService.getAllReservoirs(tenantId, 0, 20))
                .thenReturn(emptyPage);

        // When
        ResponseEntity<PageData<RvReservoirDto>> response = reservoirController.getAllReservoirs(tenantId, 0, 20);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getData().isEmpty());
    }

    @Test
    @DisplayName("GET /reservoirs/by-field/{fieldId}: Obtener reservoirs por campo")
    void testGetReservoirsByField_Success() {
        // Given
        List<RvReservoirDto> reservoirs = Arrays.asList(testReservoir, createAnotherReservoir());
        when(reservoirService.getReservoirsByField(tenantId, fieldId))
                .thenReturn(reservoirs);

        // When
        ResponseEntity<List<RvReservoirDto>> response = reservoirController.getReservoirsByField(tenantId, fieldId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());

        verify(reservoirService, times(1)).getReservoirsByField(tenantId, fieldId);
    }

    // ===========================================
    // UPDATE TESTS
    // ===========================================

    @Test
    @DisplayName("PUT /reservoirs/{id}: Actualizar reservoir exitosamente")
    void testUpdateReservoir_Success() {
        // Given
        RvReservoirDto updatedDto = new RvReservoirDto();
        updatedDto.setName("Updated Reservoir");
        updatedDto.setAveragePorosityFrac(new BigDecimal("0.25"));

        RvReservoirDto expectedResult = new RvReservoirDto();
        expectedResult.setAssetId(reservoirId);
        expectedResult.setName("Updated Reservoir");
        expectedResult.setAveragePorosityFrac(new BigDecimal("0.25"));

        when(reservoirService.updateReservoir(any(RvReservoirDto.class)))
                .thenReturn(expectedResult);

        // When
        ResponseEntity<RvReservoirDto> response = reservoirController.updateReservoir(reservoirId, updatedDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Reservoir", response.getBody().getName());
        assertEquals(reservoirId, response.getBody().getAssetId());

        verify(reservoirService, times(1)).updateReservoir(any(RvReservoirDto.class));
    }

    @Test
    @DisplayName("PUT /reservoirs/{id}: Actualizar reservoir no encontrado")
    void testUpdateReservoir_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        RvReservoirDto updateDto = new RvReservoirDto();
        updateDto.setName("Updated");

        when(reservoirService.updateReservoir(any(RvReservoirDto.class)))
                .thenThrow(new RvEntityNotFoundException("Reservoir", nonExistentId));

        // When/Then
        assertThrows(RvEntityNotFoundException.class, () -> {
            reservoirController.updateReservoir(nonExistentId, updateDto);
        });
    }

    // ===========================================
    // DELETE TESTS
    // ===========================================

    @Test
    @DisplayName("DELETE /reservoirs/{id}: Eliminar reservoir exitosamente")
    void testDeleteReservoir_Success() {
        // Given
        doNothing().when(reservoirService).deleteReservoir(tenantId, reservoirId);

        // When
        ResponseEntity<Void> response = reservoirController.deleteReservoir(tenantId, reservoirId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(reservoirService, times(1)).deleteReservoir(tenantId, reservoirId);
    }

    @Test
    @DisplayName("DELETE /reservoirs/{id}: No se puede eliminar con zonas asociadas")
    void testDeleteReservoir_HasChildren() {
        // Given
        doThrow(new RvBusinessException(RvBusinessException.INVALID_HIERARCHY,
                "Cannot delete reservoir with 5 associated zones/wells"))
                .when(reservoirService).deleteReservoir(tenantId, reservoirId);

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            reservoirController.deleteReservoir(tenantId, reservoirId);
        });

        verify(reservoirService, times(1)).deleteReservoir(tenantId, reservoirId);
    }

    // ===========================================
    // OOIP CALCULATION TESTS
    // ===========================================

    @Test
    @DisplayName("POST /reservoirs/{id}/calculate-ooip: Calcular OOIP exitosamente")
    void testCalculateOOIP_Success() {
        // Given
        BigDecimal expectedOOIP = new BigDecimal("50000000"); // 50 MMbbl
        when(reservoirService.calculateOOIP(reservoirId))
                .thenReturn(expectedOOIP);

        // When
        ResponseEntity<Map<String, Object>> response = reservoirController.calculateOOIP(reservoirId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(reservoirId, response.getBody().get("reservoirId"));
        assertEquals(expectedOOIP, response.getBody().get("ooip_mmbbl"));
        assertNotNull(response.getBody().get("calculatedAt"));

        verify(reservoirService, times(1)).calculateOOIP(reservoirId);
    }

    @Test
    @DisplayName("POST /reservoirs/{id}/calculate-ooip: Datos insuficientes")
    void testCalculateOOIP_InsufficientData() {
        // Given
        when(reservoirService.calculateOOIP(reservoirId))
                .thenThrow(new RvBusinessException(RvBusinessException.INSUFFICIENT_DATA,
                        "Missing required data for OOIP calculation"));

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            reservoirController.calculateOOIP(reservoirId);
        });

        verify(reservoirService, times(1)).calculateOOIP(reservoirId);
    }

    @Test
    @DisplayName("POST /reservoirs/{id}/calculate-ooip: Reservoir no encontrado")
    void testCalculateOOIP_ReservoirNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(reservoirService.calculateOOIP(nonExistentId))
                .thenThrow(new RvEntityNotFoundException("Reservoir", nonExistentId));

        // When/Then
        assertThrows(RvEntityNotFoundException.class, () -> {
            reservoirController.calculateOOIP(nonExistentId);
        });
    }

    // ===========================================
    // PVT STUDY ASSOCIATION TESTS
    // ===========================================

    @Test
    @DisplayName("POST /reservoirs/{id}/pvt-studies/{pvtStudyId}: Asociar estudio PVT")
    void testAssociatePvtStudy_Success() {
        // Given
        UUID pvtStudyId = UUID.randomUUID();
        doNothing().when(reservoirService).associatePvtStudy(tenantId, reservoirId, pvtStudyId);

        // When
        ResponseEntity<Void> response = reservoirController.associatePvtStudy(tenantId, reservoirId, pvtStudyId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(reservoirService, times(1)).associatePvtStudy(tenantId, reservoirId, pvtStudyId);
    }

    @Test
    @DisplayName("POST /reservoirs/{id}/pvt-studies/{pvtStudyId}: PVT Study no encontrado")
    void testAssociatePvtStudy_PvtStudyNotFound() {
        // Given
        UUID pvtStudyId = UUID.randomUUID();
        doThrow(new RvEntityNotFoundException("PVT Study", pvtStudyId))
                .when(reservoirService).associatePvtStudy(tenantId, reservoirId, pvtStudyId);

        // When/Then
        assertThrows(RvEntityNotFoundException.class, () -> {
            reservoirController.associatePvtStudy(tenantId, reservoirId, pvtStudyId);
        });
    }

    // ===========================================
    // HELPER METHODS
    // ===========================================

    private RvReservoirDto createAnotherReservoir() {
        RvReservoirDto reservoir = new RvReservoirDto();
        reservoir.setAssetId(UUID.randomUUID());
        reservoir.setTenantId(tenantId);
        reservoir.setName("Another Reservoir");
        reservoir.setFieldAssetId(fieldId);
        reservoir.setFormationName("Napo");
        reservoir.setAveragePorosityFrac(new BigDecimal("0.18"));
        return reservoir;
    }
}
