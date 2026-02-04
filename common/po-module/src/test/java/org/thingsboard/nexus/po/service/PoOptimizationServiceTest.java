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
package org.thingsboard.nexus.po.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.*;
import org.thingsboard.nexus.po.dto.OptimizationResultDto.OptimizationRunStatus;
import org.thingsboard.nexus.po.exception.PoOptimizationException;
import org.thingsboard.nexus.po.model.PoOptimizationResult;
import org.thingsboard.nexus.po.repository.PoOptimizationResultRepository;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PoOptimizationService.
 */
@ExtendWith(MockitoExtension.class)
class PoOptimizationServiceTest {

    @Mock
    private PoOptimizationResultRepository resultRepository;

    @Mock
    private PoRecommendationService recommendationService;

    @Mock
    private PoAssetService assetService;

    @Mock
    private PoEspFrequencyOptimizer espOptimizer;

    @Mock
    private PoGasLiftOptimizer gasLiftOptimizer;

    @Mock
    private PoPcpSpeedOptimizer pcpOptimizer;

    @Mock
    private PoRodPumpOptimizer rodPumpOptimizer;

    @Mock
    private PoModuleConfiguration config;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PoOptimizationService service;

    private UUID tenantId;
    private UUID wellAssetId;
    private UUID fieldAssetId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        wellAssetId = UUID.randomUUID();
        fieldAssetId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("ESP Frequency Optimization tests")
    class EspFrequencyTests {

        @Test
        @DisplayName("Should successfully run ESP optimization")
        void shouldSuccessfullyRunEspOptimization() {
            // Arrange
            EspOptimizationDto espResult = createEspOptimizationDto(true);
            when(espOptimizer.optimizeFrequency(tenantId, wellAssetId, null)).thenReturn(espResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(wellAssetId)).thenReturn("TEST-WELL");

            RecommendationDto recommendation = RecommendationDto.builder()
                    .assetId(wellAssetId)
                    .type(OptimizationType.ESP_FREQUENCY)
                    .build();
            when(espOptimizer.createRecommendation(eq(tenantId), any())).thenReturn(recommendation);

            // Act
            OptimizationResultDto result = service.optimizeEspFrequency(tenantId, wellAssetId, userId);

            // Assert
            assertNotNull(result);
            assertEquals(OptimizationRunStatus.COMPLETED, result.getRunStatus());
            assertEquals(OptimizationType.ESP_FREQUENCY, result.getType());
            assertNotNull(result.getOptimalValue());
            assertEquals("Hz", result.getOptimalValueUnit());
            assertTrue(result.getConverged());

            verify(espOptimizer).optimizeFrequency(tenantId, wellAssetId, null);
            verify(resultRepository, times(1)).save(any());
            verify(recommendationService).createRecommendation(any());
        }

        @Test
        @DisplayName("Should mark as failed when well not found")
        void shouldMarkFailedWhenWellNotFound() {
            when(espOptimizer.optimizeFrequency(tenantId, wellAssetId, null)).thenReturn(null);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(wellAssetId)).thenReturn(null);

            OptimizationResultDto result = service.optimizeEspFrequency(tenantId, wellAssetId, userId);

            assertEquals(OptimizationRunStatus.FAILED, result.getRunStatus());
            assertNotNull(result.getErrorMessage());
            verify(recommendationService, never()).createRecommendation(any());
        }

        @Test
        @DisplayName("Should not create recommendation when not significant")
        void shouldNotCreateRecommendationWhenNotSignificant() {
            EspOptimizationDto espResult = createEspOptimizationDto(false);
            when(espOptimizer.optimizeFrequency(tenantId, wellAssetId, null)).thenReturn(espResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(wellAssetId)).thenReturn("TEST-WELL");

            OptimizationResultDto result = service.optimizeEspFrequency(tenantId, wellAssetId, userId);

            assertEquals(OptimizationRunStatus.COMPLETED, result.getRunStatus());
            verify(espOptimizer, never()).createRecommendation(any(), any());
            verify(recommendationService, never()).createRecommendation(any());
        }

        @Test
        @DisplayName("Should handle exception during optimization")
        void shouldHandleExceptionDuringOptimization() {
            when(espOptimizer.optimizeFrequency(tenantId, wellAssetId, null))
                    .thenThrow(new RuntimeException("Test error"));
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            assertThrows(PoOptimizationException.class,
                    () -> service.optimizeEspFrequency(tenantId, wellAssetId, userId));

            verify(resultRepository, times(1)).save(argThat(r ->
                    r.getRunStatus() == OptimizationRunStatus.FAILED));
        }

        @Test
        @DisplayName("Should optimize all ESP wells")
        void shouldOptimizeAllEspWells() {
            List<Asset> espAssets = createAssetList(3);
            Page<Asset> espPage = new PageImpl<>(espAssets);
            when(assetService.getEspSystems(eq(tenantId), anyInt(), anyInt())).thenReturn(espPage);

            EspOptimizationDto espResult = createEspOptimizationDto(false);
            when(espOptimizer.optimizeFrequency(eq(tenantId), any(), eq(null))).thenReturn(espResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(any())).thenReturn("TEST-WELL");

            List<OptimizationResultDto> results = service.optimizeAllEspWells(tenantId, userId);

            assertEquals(3, results.size());
            verify(espOptimizer, times(3)).optimizeFrequency(eq(tenantId), any(), eq(null));
        }
    }

    @Nested
    @DisplayName("Gas Lift Optimization tests")
    class GasLiftTests {

        @Test
        @DisplayName("Should successfully run gas lift optimization")
        void shouldSuccessfullyRunGasLiftOptimization() {
            GasLiftAllocationDto glResult = createGasLiftAllocationDto();
            when(gasLiftOptimizer.optimizeAllocation(tenantId, fieldAssetId, null)).thenReturn(glResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(fieldAssetId)).thenReturn("TEST-FIELD");
            when(gasLiftOptimizer.createRecommendations(eq(tenantId), any())).thenReturn(Collections.emptyList());

            OptimizationResultDto result = service.optimizeGasLiftAllocation(tenantId, fieldAssetId, null, userId);

            assertNotNull(result);
            assertEquals(OptimizationRunStatus.COMPLETED, result.getRunStatus());
            assertEquals(OptimizationType.GAS_LIFT_ALLOCATION, result.getType());
            assertEquals("BPD", result.getOptimalValueUnit());

            verify(gasLiftOptimizer).optimizeAllocation(tenantId, fieldAssetId, null);
        }

        @Test
        @DisplayName("Should pass totalAvailableGas to optimizer")
        void shouldPassTotalAvailableGasToOptimizer() {
            BigDecimal totalGas = BigDecimal.valueOf(5000);
            GasLiftAllocationDto glResult = createGasLiftAllocationDto();
            when(gasLiftOptimizer.optimizeAllocation(tenantId, fieldAssetId, totalGas)).thenReturn(glResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(fieldAssetId)).thenReturn("TEST-FIELD");
            when(gasLiftOptimizer.createRecommendations(eq(tenantId), any())).thenReturn(Collections.emptyList());

            service.optimizeGasLiftAllocation(tenantId, fieldAssetId, totalGas, userId);

            verify(gasLiftOptimizer).optimizeAllocation(tenantId, fieldAssetId, totalGas);
        }

        @Test
        @DisplayName("Should create multiple recommendations from gas lift optimization")
        void shouldCreateMultipleRecommendations() {
            GasLiftAllocationDto glResult = createGasLiftAllocationDto();
            when(gasLiftOptimizer.optimizeAllocation(tenantId, fieldAssetId, null)).thenReturn(glResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(fieldAssetId)).thenReturn("TEST-FIELD");

            List<RecommendationDto> recommendations = List.of(
                    RecommendationDto.builder().assetId(UUID.randomUUID()).build(),
                    RecommendationDto.builder().assetId(UUID.randomUUID()).build()
            );
            when(gasLiftOptimizer.createRecommendations(eq(tenantId), any())).thenReturn(recommendations);

            service.optimizeGasLiftAllocation(tenantId, fieldAssetId, null, userId);

            verify(recommendationService, times(2)).createRecommendation(any());
        }
    }

    @Nested
    @DisplayName("PCP Speed Optimization tests")
    class PcpSpeedTests {

        @Test
        @DisplayName("Should successfully run PCP optimization")
        void shouldSuccessfullyRunPcpOptimization() {
            PcpOptimizationDto pcpResult = createPcpOptimizationDto(true);
            when(pcpOptimizer.optimizeSpeed(tenantId, wellAssetId, null)).thenReturn(pcpResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(wellAssetId)).thenReturn("TEST-PCP-WELL");

            RecommendationDto recommendation = RecommendationDto.builder()
                    .assetId(wellAssetId)
                    .type(OptimizationType.PCP_SPEED)
                    .build();
            when(pcpOptimizer.createRecommendation(eq(tenantId), any())).thenReturn(recommendation);

            OptimizationResultDto result = service.optimizePcpSpeed(tenantId, wellAssetId, userId);

            assertNotNull(result);
            assertEquals(OptimizationRunStatus.COMPLETED, result.getRunStatus());
            assertEquals(OptimizationType.PCP_SPEED, result.getType());
            assertEquals("RPM", result.getOptimalValueUnit());

            verify(pcpOptimizer).optimizeSpeed(tenantId, wellAssetId, null);
            verify(recommendationService).createRecommendation(any());
        }

        @Test
        @DisplayName("Should optimize all PCP wells")
        void shouldOptimizeAllPcpWells() {
            List<Asset> pcpAssets = createAssetList(2);
            Page<Asset> pcpPage = new PageImpl<>(pcpAssets);
            when(assetService.getPcpSystems(eq(tenantId), anyInt(), anyInt())).thenReturn(pcpPage);

            PcpOptimizationDto pcpResult = createPcpOptimizationDto(false);
            when(pcpOptimizer.optimizeSpeed(eq(tenantId), any(), eq(null))).thenReturn(pcpResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(any())).thenReturn("TEST-WELL");

            List<OptimizationResultDto> results = service.optimizeAllPcpWells(tenantId, userId);

            assertEquals(2, results.size());
            verify(pcpOptimizer, times(2)).optimizeSpeed(eq(tenantId), any(), eq(null));
        }
    }

    @Nested
    @DisplayName("Rod Pump Optimization tests")
    class RodPumpTests {

        @Test
        @DisplayName("Should successfully run rod pump optimization")
        void shouldSuccessfullyRunRodPumpOptimization() {
            RodPumpOptimizationDto rpResult = createRodPumpOptimizationDto(true);
            when(rodPumpOptimizer.optimize(tenantId, wellAssetId, null)).thenReturn(rpResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(wellAssetId)).thenReturn("TEST-ROD-WELL");

            RecommendationDto recommendation = RecommendationDto.builder()
                    .assetId(wellAssetId)
                    .type(OptimizationType.ROD_PUMP_SPEED)
                    .build();
            when(rodPumpOptimizer.createRecommendation(eq(tenantId), any())).thenReturn(recommendation);

            OptimizationResultDto result = service.optimizeRodPump(tenantId, wellAssetId, userId);

            assertNotNull(result);
            assertEquals(OptimizationRunStatus.COMPLETED, result.getRunStatus());
            assertEquals(OptimizationType.ROD_PUMP_SPEED, result.getType());
            assertEquals("SPM", result.getOptimalValueUnit());

            verify(rodPumpOptimizer).optimize(tenantId, wellAssetId, null);
            verify(recommendationService).createRecommendation(any());
        }

        @Test
        @DisplayName("Should optimize all rod pump wells")
        void shouldOptimizeAllRodPumpWells() {
            List<Asset> rpAssets = createAssetList(4);
            Page<Asset> rpPage = new PageImpl<>(rpAssets);
            when(assetService.getRodPumpSystems(eq(tenantId), anyInt(), anyInt())).thenReturn(rpPage);

            RodPumpOptimizationDto rpResult = createRodPumpOptimizationDto(false);
            when(rodPumpOptimizer.optimize(eq(tenantId), any(), eq(null))).thenReturn(rpResult);
            when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetService.getAssetName(any())).thenReturn("TEST-WELL");

            List<OptimizationResultDto> results = service.optimizeAllRodPumpWells(tenantId, userId);

            assertEquals(4, results.size());
            verify(rodPumpOptimizer, times(4)).optimize(eq(tenantId), any(), eq(null));
        }
    }

    @Nested
    @DisplayName("Result Query tests")
    class ResultQueryTests {

        @Test
        @DisplayName("Should get result by ID")
        void shouldGetResultById() {
            UUID resultId = UUID.randomUUID();
            PoOptimizationResult entity = createResultEntity(resultId);
            when(resultRepository.findById(resultId)).thenReturn(Optional.of(entity));
            when(assetService.getAssetName(entity.getAssetId())).thenReturn("TEST-WELL");

            Optional<OptimizationResultDto> result = service.getResult(resultId);

            assertTrue(result.isPresent());
            assertEquals(resultId, result.get().getId());
        }

        @Test
        @DisplayName("Should return empty when result not found")
        void shouldReturnEmptyWhenNotFound() {
            UUID resultId = UUID.randomUUID();
            when(resultRepository.findById(resultId)).thenReturn(Optional.empty());

            Optional<OptimizationResultDto> result = service.getResult(resultId);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should get results for tenant")
        void shouldGetResultsForTenant() {
            List<PoOptimizationResult> entities = List.of(
                    createResultEntity(UUID.randomUUID()),
                    createResultEntity(UUID.randomUUID())
            );
            Page<PoOptimizationResult> page = new PageImpl<>(entities);
            when(resultRepository.findByTenantIdOrderByTimestampDesc(eq(tenantId), any()))
                    .thenReturn(page);
            when(assetService.getAssetName(any())).thenReturn("TEST");

            Page<OptimizationResultDto> results = service.getResults(tenantId, 0, 20);

            assertEquals(2, results.getContent().size());
        }

        @Test
        @DisplayName("Should get results for asset")
        void shouldGetResultsForAsset() {
            List<PoOptimizationResult> entities = List.of(createResultEntity(UUID.randomUUID()));
            Page<PoOptimizationResult> page = new PageImpl<>(entities);
            when(resultRepository.findByAssetIdOrderByTimestampDesc(eq(wellAssetId), any()))
                    .thenReturn(page);
            when(assetService.getAssetName(any())).thenReturn("TEST");

            Page<OptimizationResultDto> results = service.getResultsForAsset(wellAssetId, 0, 20);

            assertEquals(1, results.getContent().size());
        }

        @Test
        @DisplayName("Should get latest result for asset")
        void shouldGetLatestResultForAsset() {
            PoOptimizationResult entity = createResultEntity(UUID.randomUUID());
            when(resultRepository.findFirstByAssetIdOrderByTimestampDesc(wellAssetId))
                    .thenReturn(Optional.of(entity));
            when(assetService.getAssetName(any())).thenReturn("TEST");

            Optional<OptimizationResultDto> result = service.getLatestResult(wellAssetId);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Should get latest result for asset and type")
        void shouldGetLatestResultForAssetAndType() {
            PoOptimizationResult entity = createResultEntity(UUID.randomUUID());
            when(resultRepository.findFirstByAssetIdAndOptimizationTypeOrderByTimestampDesc(
                    wellAssetId, OptimizationType.ESP_FREQUENCY))
                    .thenReturn(Optional.of(entity));
            when(assetService.getAssetName(any())).thenReturn("TEST");

            Optional<OptimizationResultDto> result = service.getLatestResult(wellAssetId, OptimizationType.ESP_FREQUENCY);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Should get results by type")
        void shouldGetResultsByType() {
            List<PoOptimizationResult> entities = List.of(createResultEntity(UUID.randomUUID()));
            Page<PoOptimizationResult> page = new PageImpl<>(entities);
            when(resultRepository.findByTenantIdAndOptimizationTypeOrderByTimestampDesc(
                    eq(tenantId), eq(OptimizationType.PCP_SPEED), any()))
                    .thenReturn(page);
            when(assetService.getAssetName(any())).thenReturn("TEST");

            Page<OptimizationResultDto> results = service.getResultsByType(tenantId, OptimizationType.PCP_SPEED, 0, 20);

            assertEquals(1, results.getContent().size());
        }

        @Test
        @DisplayName("Should get results in time range")
        void shouldGetResultsInTimeRange() {
            long from = System.currentTimeMillis() - 3600000;
            long to = System.currentTimeMillis();
            List<PoOptimizationResult> entities = List.of(createResultEntity(UUID.randomUUID()));
            when(resultRepository.findByTenantIdAndTimestampBetween(tenantId, from, to))
                    .thenReturn(entities);
            when(assetService.getAssetName(any())).thenReturn("TEST");

            List<OptimizationResultDto> results = service.getResultsInTimeRange(tenantId, from, to);

            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should count by status")
        void shouldCountByStatus() {
            when(resultRepository.countByTenantIdAndRunStatus(tenantId, OptimizationRunStatus.COMPLETED))
                    .thenReturn(10L);

            long count = service.countByStatus(tenantId, OptimizationRunStatus.COMPLETED);

            assertEquals(10L, count);
        }

        @Test
        @DisplayName("Should count by type")
        void shouldCountByType() {
            when(resultRepository.countByTenantIdAndOptimizationType(tenantId, OptimizationType.GAS_LIFT_ALLOCATION))
                    .thenReturn(5L);

            long count = service.countByType(tenantId, OptimizationType.GAS_LIFT_ALLOCATION);

            assertEquals(5L, count);
        }
    }

    // Helper methods

    private EspOptimizationDto createEspOptimizationDto(boolean isSignificant) {
        return EspOptimizationDto.builder()
                .wellAssetId(wellAssetId)
                .wellName("TEST-WELL")
                .currentFrequency(BigDecimal.valueOf(50))
                .recommendedFrequency(BigDecimal.valueOf(54))
                .frequencyChange(BigDecimal.valueOf(4))
                .expectedProductionIncrease(BigDecimal.valueOf(40))
                .expectedProductionIncreasePercent(BigDecimal.valueOf(8))
                .expectedEfficiencyImprovement(BigDecimal.valueOf(3))
                .confidence(0.85)
                .isSignificant(isSignificant)
                .build();
    }

    private GasLiftAllocationDto createGasLiftAllocationDto() {
        return GasLiftAllocationDto.builder()
                .optimizationId(UUID.randomUUID())
                .fieldAssetId(fieldAssetId)
                .fieldName("TEST-FIELD")
                .totalAvailableGas(BigDecimal.valueOf(5000))
                .currentTotalGasRate(BigDecimal.valueOf(4500))
                .optimizedTotalGasRate(BigDecimal.valueOf(4800))
                .currentTotalProduction(BigDecimal.valueOf(1000))
                .expectedTotalProduction(BigDecimal.valueOf(1100))
                .expectedProductionIncrease(BigDecimal.valueOf(100))
                .expectedProductionIncreasePercent(BigDecimal.valueOf(10))
                .efficiencyImprovement(BigDecimal.valueOf(5))
                .wellAllocations(Collections.emptyList())
                .confidence(0.80)
                .build();
    }

    private PcpOptimizationDto createPcpOptimizationDto(boolean isSignificant) {
        return PcpOptimizationDto.builder()
                .wellAssetId(wellAssetId)
                .wellName("TEST-PCP-WELL")
                .currentRpm(BigDecimal.valueOf(200))
                .recommendedRpm(BigDecimal.valueOf(240))
                .rpmChange(BigDecimal.valueOf(40))
                .expectedProductionIncrease(BigDecimal.valueOf(50))
                .expectedProductionIncreasePercent(BigDecimal.valueOf(12))
                .expectedEfficiencyImprovement(BigDecimal.valueOf(4))
                .currentTorque(BigDecimal.valueOf(55))
                .expectedTorque(BigDecimal.valueOf(60))
                .rodWearFactor(BigDecimal.valueOf(0.4))
                .statorWearFactor(BigDecimal.valueOf(0.35))
                .confidence(0.85)
                .isSignificant(isSignificant)
                .build();
    }

    private RodPumpOptimizationDto createRodPumpOptimizationDto(boolean isSignificant) {
        return RodPumpOptimizationDto.builder()
                .wellAssetId(wellAssetId)
                .wellName("TEST-ROD-WELL")
                .currentSpm(BigDecimal.valueOf(8))
                .recommendedSpm(BigDecimal.valueOf(6.5))
                .spmChange(BigDecimal.valueOf(-1.5))
                .currentStrokeLength(BigDecimal.valueOf(86))
                .recommendedStrokeLength(BigDecimal.valueOf(86))
                .strokeLengthChange(BigDecimal.ZERO)
                .currentFillage(BigDecimal.valueOf(60))
                .expectedFillage(BigDecimal.valueOf(78))
                .expectedProductionIncrease(BigDecimal.valueOf(-5))
                .expectedProductionIncreasePercent(BigDecimal.valueOf(-5))
                .expectedEfficiencyImprovement(BigDecimal.valueOf(10))
                .currentPeakLoad(BigDecimal.valueOf(15000))
                .expectedPeakLoad(BigDecimal.valueOf(12000))
                .currentRodStress(BigDecimal.valueOf(20000))
                .expectedRodStress(BigDecimal.valueOf(16000))
                .currentPumpEfficiency(BigDecimal.valueOf(65))
                .expectedPumpEfficiency(BigDecimal.valueOf(75))
                .optimizationType("SPEED")
                .confidence(0.80)
                .isSignificant(isSignificant)
                .build();
    }

    private PoOptimizationResult createResultEntity(UUID id) {
        return PoOptimizationResult.builder()
                .id(id)
                .tenantId(tenantId)
                .assetId(wellAssetId)
                .assetType("well")
                .optimizationType(OptimizationType.ESP_FREQUENCY)
                .runStatus(OptimizationRunStatus.COMPLETED)
                .algorithm("TEST_ALGORITHM")
                .algorithmVersion("1.0.0")
                .optimalValue(BigDecimal.valueOf(54))
                .optimalValueUnit("Hz")
                .converged(true)
                .computationTimeMs(1500L)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private List<Asset> createAssetList(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    UUID assetId = UUID.randomUUID();
                    Asset asset = new Asset();
                    asset.setId(new AssetId(assetId));
                    asset.setTenantId(TenantId.fromUUID(tenantId));
                    asset.setName("ASSET-" + i);
                    return asset;
                })
                .toList();
    }
}
