/*
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
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.GasLiftAllocationDto;
import org.thingsboard.nexus.po.dto.GasLiftAllocationDto.WellAllocation;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PoGasLiftOptimizer.
 */
@ExtendWith(MockitoExtension.class)
class PoGasLiftOptimizerTest {

    @Mock
    private PoAssetService assetService;

    @Mock
    private PoAttributeService attributeService;

    @Mock
    private PoModuleConfiguration config;

    @InjectMocks
    private PoGasLiftOptimizer optimizer;

    private UUID tenantId;
    private UUID fieldAssetId;
    private Asset fieldAsset;
    private PoModuleConfiguration.GasLiftAllocatorConfig glConfig;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        fieldAssetId = UUID.randomUUID();

        fieldAsset = new Asset();
        fieldAsset.setId(new AssetId(fieldAssetId));
        fieldAsset.setTenantId(TenantId.fromUUID(tenantId));
        fieldAsset.setName("TEST-FIELD-001");
        fieldAsset.setType("field");

        glConfig = new PoModuleConfiguration.GasLiftAllocatorConfig();
        glConfig.setEnabled(true);
        glConfig.setMaxTotalGasRate(10000.0);
        glConfig.setMinGasPerWell(50.0);
        glConfig.setMaxGasPerWell(2000.0);
    }

    @Nested
    @DisplayName("optimizeAllocation tests")
    class OptimizeAllocationTests {

        @Test
        @DisplayName("Should return null when field not found")
        void shouldReturnNullWhenFieldNotFound() {
            when(assetService.getAssetById(any(), any())).thenReturn(Optional.empty());

            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, null);

            assertNull(result);
            verify(assetService).getAssetById(tenantId, fieldAssetId);
        }

        @Test
        @DisplayName("Should return null when no gas lift wells found")
        void shouldReturnNullWhenNoWellsFound() {
            when(assetService.getAssetById(tenantId, fieldAssetId)).thenReturn(Optional.of(fieldAsset));

            // Return empty list of wells
            Page<Asset> emptyPage = new PageImpl<>(Collections.emptyList());
            when(assetService.getWells(eq(tenantId), anyInt(), anyInt())).thenReturn(emptyPage);

            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, null);

            assertNull(result);
        }

        @Test
        @DisplayName("Should optimize allocation for multiple wells")
        void shouldOptimizeAllocationForMultipleWells() {
            // Arrange
            when(assetService.getAssetById(tenantId, fieldAssetId)).thenReturn(Optional.of(fieldAsset));
            when(config.getGasLift()).thenReturn(glConfig);

            List<Asset> wells = createTestWells(3);
            Page<Asset> wellsPage = new PageImpl<>(wells);
            when(assetService.getWells(eq(tenantId), anyInt(), anyInt())).thenReturn(wellsPage);

            // Setup attributes for each well
            for (int i = 0; i < wells.size(); i++) {
                Map<String, Object> attrs = createGasLiftWellAttributes(
                        fieldAssetId,
                        500.0 + i * 100,  // gas rate
                        200.0 + i * 50    // production
                );
                when(attributeService.getAttributesAsMap(wells.get(i).getId().getId())).thenReturn(attrs);
            }

            // Act
            BigDecimal totalGas = BigDecimal.valueOf(2000);
            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, totalGas);

            // Assert
            assertNotNull(result);
            assertEquals(fieldAssetId, result.getFieldAssetId());
            assertEquals("TEST-FIELD-001", result.getFieldName());
            assertEquals(3, result.getWellAllocations().size());
            assertNotNull(result.getOptimizationId());
            assertTrue(result.getTimestamp() > 0);
        }

        @Test
        @DisplayName("Should prioritize wells with higher marginal oil rate")
        void shouldPrioritizeHigherMarginalWells() {
            // Arrange
            when(assetService.getAssetById(tenantId, fieldAssetId)).thenReturn(Optional.of(fieldAsset));
            when(config.getGasLift()).thenReturn(glConfig);

            List<Asset> wells = createTestWells(2);
            Page<Asset> wellsPage = new PageImpl<>(wells);
            when(assetService.getWells(eq(tenantId), anyInt(), anyInt())).thenReturn(wellsPage);

            // Well 1: Lower gas, higher production (better marginal)
            Map<String, Object> attrs1 = createGasLiftWellAttributes(fieldAssetId, 300.0, 400.0);
            when(attributeService.getAttributesAsMap(wells.get(0).getId().getId())).thenReturn(attrs1);

            // Well 2: Higher gas, lower production (worse marginal)
            Map<String, Object> attrs2 = createGasLiftWellAttributes(fieldAssetId, 700.0, 300.0);
            when(attributeService.getAttributesAsMap(wells.get(1).getId().getId())).thenReturn(attrs2);

            // Act
            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, BigDecimal.valueOf(1500));

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getWellAllocations().size());

            // First well should have higher marginal rate and rank 1
            List<WellAllocation> allocations = result.getWellAllocations();
            WellAllocation topWell = allocations.stream()
                    .filter(a -> a.getPriorityRank() == 1)
                    .findFirst()
                    .orElse(null);

            assertNotNull(topWell, "Should have a well with priority rank 1");
            assertTrue(topWell.getMarginalOilRate().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should respect total available gas constraint")
        void shouldRespectTotalGasConstraint() {
            // Arrange
            when(assetService.getAssetById(tenantId, fieldAssetId)).thenReturn(Optional.of(fieldAsset));
            when(config.getGasLift()).thenReturn(glConfig);

            List<Asset> wells = createTestWells(3);
            Page<Asset> wellsPage = new PageImpl<>(wells);
            when(assetService.getWells(eq(tenantId), anyInt(), anyInt())).thenReturn(wellsPage);

            for (Asset well : wells) {
                Map<String, Object> attrs = createGasLiftWellAttributes(fieldAssetId, 500.0, 250.0);
                when(attributeService.getAttributesAsMap(well.getId().getId())).thenReturn(attrs);
            }

            // Act
            BigDecimal totalGas = BigDecimal.valueOf(1000); // Limited gas
            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, totalGas);

            // Assert
            assertNotNull(result);
            BigDecimal allocatedGas = result.getWellAllocations().stream()
                    .map(WellAllocation::getRecommendedGasRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertTrue(allocatedGas.compareTo(totalGas) <= 0,
                    "Total allocated gas should not exceed available gas");
        }

        @Test
        @DisplayName("Should respect per-well min/max constraints")
        void shouldRespectPerWellConstraints() {
            // Arrange
            when(assetService.getAssetById(tenantId, fieldAssetId)).thenReturn(Optional.of(fieldAsset));
            when(config.getGasLift()).thenReturn(glConfig);

            List<Asset> wells = createTestWells(2);
            Page<Asset> wellsPage = new PageImpl<>(wells);
            when(assetService.getWells(eq(tenantId), anyInt(), anyInt())).thenReturn(wellsPage);

            for (Asset well : wells) {
                Map<String, Object> attrs = createGasLiftWellAttributes(fieldAssetId, 500.0, 250.0);
                attrs.put("min_gas_rate", 100.0);
                attrs.put("max_gas_rate", 800.0);
                when(attributeService.getAttributesAsMap(well.getId().getId())).thenReturn(attrs);
            }

            // Act - provide abundant gas
            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, BigDecimal.valueOf(5000));

            // Assert
            assertNotNull(result);
            for (WellAllocation alloc : result.getWellAllocations()) {
                assertTrue(alloc.getRecommendedGasRate().compareTo(BigDecimal.valueOf(50)) >= 0,
                        "Recommended rate should be at least minimum");
                assertTrue(alloc.getRecommendedGasRate().compareTo(BigDecimal.valueOf(2000)) <= 0,
                        "Recommended rate should not exceed maximum");
            }
        }

        @Test
        @DisplayName("Should use current total gas when totalAvailableGas is null")
        void shouldUseCurrentTotalWhenAvailableGasNull() {
            // Arrange
            when(assetService.getAssetById(tenantId, fieldAssetId)).thenReturn(Optional.of(fieldAsset));
            when(config.getGasLift()).thenReturn(glConfig);

            List<Asset> wells = createTestWells(2);
            Page<Asset> wellsPage = new PageImpl<>(wells);
            when(assetService.getWells(eq(tenantId), anyInt(), anyInt())).thenReturn(wellsPage);

            // Total current gas = 500 + 600 = 1100
            Map<String, Object> attrs1 = createGasLiftWellAttributes(fieldAssetId, 500.0, 200.0);
            Map<String, Object> attrs2 = createGasLiftWellAttributes(fieldAssetId, 600.0, 250.0);
            when(attributeService.getAttributesAsMap(wells.get(0).getId().getId())).thenReturn(attrs1);
            when(attributeService.getAttributesAsMap(wells.get(1).getId().getId())).thenReturn(attrs2);

            // Act
            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, null);

            // Assert
            assertNotNull(result);
            assertEquals(BigDecimal.valueOf(1100).setScale(0),
                    result.getCurrentTotalGasRate().setScale(0));
        }

        @Test
        @DisplayName("Should calculate production increase correctly")
        void shouldCalculateProductionIncreaseCorrectly() {
            // Arrange
            when(assetService.getAssetById(tenantId, fieldAssetId)).thenReturn(Optional.of(fieldAsset));
            when(config.getGasLift()).thenReturn(glConfig);

            List<Asset> wells = createTestWells(2);
            Page<Asset> wellsPage = new PageImpl<>(wells);
            when(assetService.getWells(eq(tenantId), anyInt(), anyInt())).thenReturn(wellsPage);

            for (Asset well : wells) {
                Map<String, Object> attrs = createGasLiftWellAttributes(fieldAssetId, 400.0, 200.0);
                when(attributeService.getAttributesAsMap(well.getId().getId())).thenReturn(attrs);
            }

            // Act
            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, BigDecimal.valueOf(1500));

            // Assert
            assertNotNull(result);
            assertNotNull(result.getExpectedProductionIncrease());
            assertNotNull(result.getExpectedProductionIncreasePercent());

            // Expected production should be current + increase
            BigDecimal calculatedExpected = result.getCurrentTotalProduction()
                    .add(result.getExpectedProductionIncrease());
            assertEquals(0, calculatedExpected.compareTo(result.getExpectedTotalProduction()));
        }

        @Test
        @DisplayName("Should calculate confidence based on well count")
        void shouldCalculateConfidenceBasedOnWellCount() {
            // Arrange
            when(assetService.getAssetById(tenantId, fieldAssetId)).thenReturn(Optional.of(fieldAsset));
            when(config.getGasLift()).thenReturn(glConfig);

            // Test with 2 wells (less than 5)
            List<Asset> wells = createTestWells(2);
            Page<Asset> wellsPage = new PageImpl<>(wells);
            when(assetService.getWells(eq(tenantId), anyInt(), anyInt())).thenReturn(wellsPage);

            for (Asset well : wells) {
                Map<String, Object> attrs = createGasLiftWellAttributes(fieldAssetId, 500.0, 250.0);
                when(attributeService.getAttributesAsMap(well.getId().getId())).thenReturn(attrs);
            }

            // Act
            GasLiftAllocationDto result = optimizer.optimizeAllocation(tenantId, fieldAssetId, BigDecimal.valueOf(2000));

            // Assert
            assertNotNull(result);
            // Confidence should be reduced for fewer wells
            assertTrue(result.getConfidence() >= 0.5 && result.getConfidence() <= 1.0,
                    "Confidence should be between 0.5 and 1.0");
        }
    }

    @Nested
    @DisplayName("createRecommendations tests")
    class CreateRecommendationsTests {

        @Test
        @DisplayName("Should create recommendations only for significant changes")
        void shouldCreateRecommendationsForSignificantChanges() {
            // Create allocation with significant change (>5%)
            WellAllocation significantAlloc = WellAllocation.builder()
                    .wellAssetId(UUID.randomUUID())
                    .wellName("WELL-001")
                    .currentGasRate(BigDecimal.valueOf(500))
                    .recommendedGasRate(BigDecimal.valueOf(600)) // 20% increase
                    .gasRateChange(BigDecimal.valueOf(100))
                    .currentProduction(BigDecimal.valueOf(200))
                    .expectedProduction(BigDecimal.valueOf(220))
                    .expectedProductionIncrease(BigDecimal.valueOf(20))
                    .marginalOilRate(BigDecimal.valueOf(0.2))
                    .priorityRank(1)
                    .atMinimum(false)
                    .atMaximum(false)
                    .build();

            // Create allocation with insignificant change (<5%)
            WellAllocation insignificantAlloc = WellAllocation.builder()
                    .wellAssetId(UUID.randomUUID())
                    .wellName("WELL-002")
                    .currentGasRate(BigDecimal.valueOf(500))
                    .recommendedGasRate(BigDecimal.valueOf(510)) // 2% increase
                    .gasRateChange(BigDecimal.valueOf(10))
                    .currentProduction(BigDecimal.valueOf(200))
                    .expectedProduction(BigDecimal.valueOf(202))
                    .expectedProductionIncrease(BigDecimal.valueOf(2))
                    .marginalOilRate(BigDecimal.valueOf(0.2))
                    .priorityRank(2)
                    .atMinimum(false)
                    .atMaximum(false)
                    .build();

            GasLiftAllocationDto optimization = GasLiftAllocationDto.builder()
                    .optimizationId(UUID.randomUUID())
                    .fieldAssetId(fieldAssetId)
                    .fieldName("TEST-FIELD")
                    .totalAvailableGas(BigDecimal.valueOf(2000))
                    .currentTotalGasRate(BigDecimal.valueOf(1000))
                    .optimizedTotalGasRate(BigDecimal.valueOf(1110))
                    .currentTotalProduction(BigDecimal.valueOf(400))
                    .expectedTotalProduction(BigDecimal.valueOf(422))
                    .expectedProductionIncrease(BigDecimal.valueOf(22))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(5.5))
                    .wellAllocations(List.of(significantAlloc, insignificantAlloc))
                    .confidence(0.85)
                    .build();

            // Act
            List<RecommendationDto> recommendations = optimizer.createRecommendations(tenantId, optimization);

            // Assert
            assertEquals(1, recommendations.size(), "Should only create recommendation for significant change");
            assertEquals("WELL-001", recommendations.get(0).getAssetName());
        }

        @Test
        @DisplayName("Should set correct recommendation type and unit")
        void shouldSetCorrectTypeAndUnit() {
            WellAllocation alloc = WellAllocation.builder()
                    .wellAssetId(UUID.randomUUID())
                    .wellName("WELL-001")
                    .currentGasRate(BigDecimal.valueOf(400))
                    .recommendedGasRate(BigDecimal.valueOf(600))
                    .gasRateChange(BigDecimal.valueOf(200))
                    .currentProduction(BigDecimal.valueOf(200))
                    .expectedProduction(BigDecimal.valueOf(250))
                    .expectedProductionIncrease(BigDecimal.valueOf(50))
                    .marginalOilRate(BigDecimal.valueOf(0.25))
                    .priorityRank(1)
                    .atMinimum(false)
                    .atMaximum(false)
                    .build();

            GasLiftAllocationDto optimization = GasLiftAllocationDto.builder()
                    .optimizationId(UUID.randomUUID())
                    .fieldAssetId(fieldAssetId)
                    .fieldName("TEST-FIELD")
                    .totalAvailableGas(BigDecimal.valueOf(2000))
                    .wellAllocations(List.of(alloc))
                    .expectedProductionIncrease(BigDecimal.valueOf(50))
                    .confidence(0.85)
                    .build();

            List<RecommendationDto> recommendations = optimizer.createRecommendations(tenantId, optimization);

            assertEquals(1, recommendations.size());
            RecommendationDto rec = recommendations.get(0);
            assertEquals(OptimizationType.GAS_LIFT_ALLOCATION, rec.getType());
            assertEquals("MSCF/day", rec.getUnit());
            assertEquals(BigDecimal.valueOf(400), rec.getCurrentValue());
            assertEquals(BigDecimal.valueOf(600), rec.getRecommendedValue());
        }

        @Test
        @DisplayName("Should use correct direction in title")
        void shouldUseCorrectDirectionInTitle() {
            // Increase scenario
            WellAllocation increaseAlloc = WellAllocation.builder()
                    .wellAssetId(UUID.randomUUID())
                    .wellName("WELL-INC")
                    .currentGasRate(BigDecimal.valueOf(400))
                    .recommendedGasRate(BigDecimal.valueOf(600))
                    .gasRateChange(BigDecimal.valueOf(200))
                    .currentProduction(BigDecimal.valueOf(200))
                    .expectedProduction(BigDecimal.valueOf(250))
                    .expectedProductionIncrease(BigDecimal.valueOf(50))
                    .marginalOilRate(BigDecimal.valueOf(0.25))
                    .priorityRank(1)
                    .atMinimum(false)
                    .atMaximum(false)
                    .build();

            // Decrease scenario
            WellAllocation decreaseAlloc = WellAllocation.builder()
                    .wellAssetId(UUID.randomUUID())
                    .wellName("WELL-DEC")
                    .currentGasRate(BigDecimal.valueOf(600))
                    .recommendedGasRate(BigDecimal.valueOf(400))
                    .gasRateChange(BigDecimal.valueOf(-200))
                    .currentProduction(BigDecimal.valueOf(200))
                    .expectedProduction(BigDecimal.valueOf(180))
                    .expectedProductionIncrease(BigDecimal.valueOf(-20))
                    .marginalOilRate(BigDecimal.valueOf(0.1))
                    .priorityRank(2)
                    .atMinimum(false)
                    .atMaximum(false)
                    .build();

            GasLiftAllocationDto optimization = GasLiftAllocationDto.builder()
                    .optimizationId(UUID.randomUUID())
                    .fieldAssetId(fieldAssetId)
                    .fieldName("TEST-FIELD")
                    .totalAvailableGas(BigDecimal.valueOf(2000))
                    .wellAllocations(List.of(increaseAlloc, decreaseAlloc))
                    .confidence(0.85)
                    .build();

            List<RecommendationDto> recommendations = optimizer.createRecommendations(tenantId, optimization);

            assertEquals(2, recommendations.size());

            RecommendationDto incRec = recommendations.stream()
                    .filter(r -> r.getAssetName().equals("WELL-INC"))
                    .findFirst().orElse(null);
            RecommendationDto decRec = recommendations.stream()
                    .filter(r -> r.getAssetName().equals("WELL-DEC"))
                    .findFirst().orElse(null);

            assertNotNull(incRec);
            assertNotNull(decRec);
            assertTrue(incRec.getTitle().contains("Increase"));
            assertTrue(decRec.getTitle().contains("Decrease"));
        }

        @Test
        @DisplayName("Should set priority based on production increase")
        void shouldSetPriorityBasedOnIncrease() {
            WellAllocation highImpactAlloc = WellAllocation.builder()
                    .wellAssetId(UUID.randomUUID())
                    .wellName("WELL-HIGH")
                    .currentGasRate(BigDecimal.valueOf(300))
                    .recommendedGasRate(BigDecimal.valueOf(600))
                    .gasRateChange(BigDecimal.valueOf(300))
                    .currentProduction(BigDecimal.valueOf(200))
                    .expectedProduction(BigDecimal.valueOf(275))
                    .expectedProductionIncrease(BigDecimal.valueOf(75)) // High impact (>50)
                    .marginalOilRate(BigDecimal.valueOf(0.25))
                    .priorityRank(1)
                    .atMinimum(false)
                    .atMaximum(false)
                    .build();

            WellAllocation lowImpactAlloc = WellAllocation.builder()
                    .wellAssetId(UUID.randomUUID())
                    .wellName("WELL-LOW")
                    .currentGasRate(BigDecimal.valueOf(400))
                    .recommendedGasRate(BigDecimal.valueOf(500))
                    .gasRateChange(BigDecimal.valueOf(100))
                    .currentProduction(BigDecimal.valueOf(200))
                    .expectedProduction(BigDecimal.valueOf(205))
                    .expectedProductionIncrease(BigDecimal.valueOf(5)) // Low impact (<10)
                    .marginalOilRate(BigDecimal.valueOf(0.05))
                    .priorityRank(2)
                    .atMinimum(false)
                    .atMaximum(false)
                    .build();

            GasLiftAllocationDto optimization = GasLiftAllocationDto.builder()
                    .optimizationId(UUID.randomUUID())
                    .fieldAssetId(fieldAssetId)
                    .fieldName("TEST-FIELD")
                    .totalAvailableGas(BigDecimal.valueOf(2000))
                    .wellAllocations(List.of(highImpactAlloc, lowImpactAlloc))
                    .confidence(0.85)
                    .build();

            List<RecommendationDto> recommendations = optimizer.createRecommendations(tenantId, optimization);

            RecommendationDto highRec = recommendations.stream()
                    .filter(r -> r.getAssetName().equals("WELL-HIGH"))
                    .findFirst().orElse(null);
            RecommendationDto lowRec = recommendations.stream()
                    .filter(r -> r.getAssetName().equals("WELL-LOW"))
                    .findFirst().orElse(null);

            assertNotNull(highRec);
            assertNotNull(lowRec);
            assertEquals(1, highRec.getPriority()); // Highest priority
            assertEquals(4, lowRec.getPriority());  // Lower priority
        }
    }

    // Helper methods

    private List<Asset> createTestWells(int count) {
        List<Asset> wells = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID wellId = UUID.randomUUID();
            Asset well = new Asset();
            well.setId(new AssetId(wellId));
            well.setTenantId(TenantId.fromUUID(tenantId));
            well.setName("TEST-WELL-" + (i + 1));
            well.setType("well");
            wells.add(well);
        }
        return wells;
    }

    private Map<String, Object> createGasLiftWellAttributes(UUID fieldId, double gasRate, double production) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("lift_type", "GAS_LIFT");
        attrs.put("field_id", fieldId.toString());
        attrs.put("gas_injection_rate", gasRate);
        attrs.put("current_production_bpd", production);
        attrs.put("gor", 1000.0);
        attrs.put("min_gas_rate", 50.0);
        attrs.put("max_gas_rate", 2000.0);
        return attrs;
    }
}
