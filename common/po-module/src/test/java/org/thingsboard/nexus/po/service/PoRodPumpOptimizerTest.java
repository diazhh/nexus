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
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.nexus.po.dto.RodPumpOptimizationDto;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PoRodPumpOptimizer.
 */
@ExtendWith(MockitoExtension.class)
class PoRodPumpOptimizerTest {

    @Mock
    private PoAssetService assetService;

    @Mock
    private PoAttributeService attributeService;

    @Mock
    private PoModuleConfiguration config;

    @InjectMocks
    private PoRodPumpOptimizer optimizer;

    private UUID tenantId;
    private UUID wellAssetId;
    private UUID rodPumpAssetId;
    private Asset wellAsset;
    private PoModuleConfiguration.RodPumpOptimizerConfig rpConfig;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        wellAssetId = UUID.randomUUID();
        rodPumpAssetId = UUID.randomUUID();

        wellAsset = new Asset();
        wellAsset.setId(new AssetId(wellAssetId));
        wellAsset.setTenantId(TenantId.fromUUID(tenantId));
        wellAsset.setName("TEST-ROD-WELL-001");
        wellAsset.setType("well");

        rpConfig = new PoModuleConfiguration.RodPumpOptimizerConfig();
        rpConfig.setEnabled(true);
        rpConfig.setMinSpmChange(0.5);
        rpConfig.setMaxSpm(15.0);
        rpConfig.setMinSpm(3.0);
        rpConfig.setMaxStrokeLength(144.0);
        rpConfig.setMinStrokeLength(24.0);
        rpConfig.setTargetFillage(85.0);
        rpConfig.setMinFillage(50.0);
        rpConfig.setMaxPeakLoad(25000.0);
        rpConfig.setMaxRodStress(30000.0);
        rpConfig.setCounterbalanceLow(45.0);
        rpConfig.setCounterbalanceHigh(55.0);
    }

    @Nested
    @DisplayName("optimize tests")
    class OptimizeTests {

        @Test
        @DisplayName("Should return null when well not found")
        void shouldReturnNullWhenWellNotFound() {
            when(assetService.getAssetById(any(), any())).thenReturn(Optional.empty());

            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            assertNull(result);
            verify(assetService).getAssetById(tenantId, wellAssetId);
        }

        @Test
        @DisplayName("Should reduce SPM when fillage is low")
        void shouldReduceSpmWhenFillageLow() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    10.0,   // spm (high)
                    86.0,   // stroke_length
                    60.0,   // fillage (low - below 85% target)
                    15000.0,// peak_load
                    3000.0, // min_load
                    50.0,   // counterbalance
                    80.0,   // production
                    20.0,   // power
                    65.0,   // efficiency
                    20000.0,// rod_stress
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertEquals(wellAssetId, result.getWellAssetId());
            assertEquals("TEST-ROD-WELL-001", result.getWellName());
            assertTrue(result.getRecommendedSpm().compareTo(result.getCurrentSpm()) < 0,
                    "Recommended SPM should be lower when fillage is low");
            assertNotNull(result.getDynacardAnalysis());
            assertTrue(result.getDynacardAnalysis().contains("underfilled"));
        }

        @Test
        @DisplayName("Should increase SPM when fillage is high and loads allow")
        void shouldIncreaseSpmWhenFillageHighAndLoadsAllow() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    7.0,    // spm (moderate)
                    86.0,   // stroke_length
                    98.0,   // fillage (high - above 95%)
                    15000.0,// peak_load (below 85% of 25000)
                    3000.0, // min_load
                    50.0,   // counterbalance
                    100.0,  // production
                    18.0,   // power
                    75.0,   // efficiency
                    18000.0,// rod_stress (below 85% of 30000)
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedSpm().compareTo(result.getCurrentSpm()) > 0,
                    "Recommended SPM should be higher when fillage is high and loads allow");
            assertNotNull(result.getDynacardAnalysis());
            assertTrue(result.getDynacardAnalysis().contains("fully filled"));
        }

        @Test
        @DisplayName("Should reduce SPM when rod stress is high")
        void shouldReduceSpmWhenRodStressHigh() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            // Rod stress at 92% of max (30000 * 0.92 = 27600)
            Map<String, Object> attrs = createRodPumpAttributes(
                    10.0,   // spm
                    86.0,   // stroke_length
                    80.0,   // fillage
                    18000.0,// peak_load
                    4000.0, // min_load
                    50.0,   // counterbalance
                    100.0,  // production
                    22.0,   // power
                    72.0,   // efficiency
                    28000.0,// rod_stress (high - above 90% of max)
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedSpm().compareTo(result.getCurrentSpm()) < 0,
                    "Recommended SPM should be lower when rod stress is high");
            assertEquals("ROD_STRESS", result.getLimitingConstraint());
        }

        @Test
        @DisplayName("Should reduce SPM when peak load is high")
        void shouldReduceSpmWhenPeakLoadHigh() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            // Peak load at 93% of max (25000 * 0.93 = 23250)
            Map<String, Object> attrs = createRodPumpAttributes(
                    9.0,    // spm
                    86.0,   // stroke_length
                    82.0,   // fillage
                    23500.0,// peak_load (high - above 90% of max)
                    5000.0, // min_load
                    50.0,   // counterbalance
                    95.0,   // production
                    21.0,   // power
                    70.0,   // efficiency
                    22000.0,// rod_stress
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedSpm().compareTo(result.getCurrentSpm()) < 0,
                    "Recommended SPM should be lower when peak load is high");
            assertEquals("PEAK_LOAD", result.getLimitingConstraint());
        }

        @Test
        @DisplayName("Should respect minimum SPM limit")
        void shouldRespectMinSpmLimit() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    4.0,    // spm (close to min of 3)
                    86.0,   // stroke_length
                    40.0,   // fillage (very low)
                    12000.0,// peak_load
                    3000.0, // min_load
                    50.0,   // counterbalance
                    50.0,   // production
                    15.0,   // power
                    60.0,   // efficiency
                    18000.0,// rod_stress
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedSpm().compareTo(BigDecimal.valueOf(3.0)) >= 0,
                    "Recommended SPM should not go below minimum");
            assertEquals(BigDecimal.valueOf(3.0), result.getMinSpm());
        }

        @Test
        @DisplayName("Should respect maximum SPM limit")
        void shouldRespectMaxSpmLimit() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    14.0,   // spm (close to max of 15)
                    86.0,   // stroke_length
                    98.0,   // fillage (high)
                    14000.0,// peak_load (low)
                    3000.0, // min_load
                    50.0,   // counterbalance
                    120.0,  // production
                    25.0,   // power
                    78.0,   // efficiency
                    16000.0,// rod_stress (low)
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedSpm().compareTo(BigDecimal.valueOf(15.0)) <= 0,
                    "Recommended SPM should not exceed maximum");
            assertEquals(BigDecimal.valueOf(15.0), result.getMaxSpm());
        }

        @Test
        @DisplayName("Should recommend counterbalance adjustment when too low")
        void shouldRecommendCounterbalanceAdjustmentWhenLow() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    8.0,    // spm
                    86.0,   // stroke_length
                    80.0,   // fillage
                    15000.0,// peak_load
                    3000.0, // min_load
                    35.0,   // counterbalance (low - below 45%)
                    90.0,   // production
                    18.0,   // power
                    70.0,   // efficiency
                    20000.0,// rod_stress
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getCounterbalanceRecommendation());
            assertTrue(result.getCounterbalanceRecommendation().contains("Increase"));
        }

        @Test
        @DisplayName("Should recommend counterbalance adjustment when too high")
        void shouldRecommendCounterbalanceAdjustmentWhenHigh() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    8.0,    // spm
                    86.0,   // stroke_length
                    80.0,   // fillage
                    15000.0,// peak_load
                    3000.0, // min_load
                    65.0,   // counterbalance (high - above 55%)
                    90.0,   // production
                    18.0,   // power
                    70.0,   // efficiency
                    20000.0,// rod_stress
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getCounterbalanceRecommendation());
            assertTrue(result.getCounterbalanceRecommendation().contains("Decrease"));
        }

        @Test
        @DisplayName("Should calculate pump displacement and theoretical capacity")
        void shouldCalculatePumpDisplacementAndCapacity() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    8.0,    // spm
                    86.0,   // stroke_length
                    80.0,   // fillage
                    15000.0,// peak_load
                    3000.0, // min_load
                    50.0,   // counterbalance
                    90.0,   // production
                    18.0,   // power
                    70.0,   // efficiency
                    20000.0,// rod_stress
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getPumpDisplacement());
            assertNotNull(result.getTheoreticalCapacity());
            assertNotNull(result.getVolumetricEfficiency());
            assertTrue(result.getPumpDisplacement().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(result.getTheoreticalCapacity().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should maintain optimal fillage range (75-95%)")
        void shouldMaintainOptimalFillageRange() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    8.0,    // spm
                    86.0,   // stroke_length
                    85.0,   // fillage (in optimal range)
                    15000.0,// peak_load
                    3000.0, // min_load
                    50.0,   // counterbalance
                    100.0,  // production
                    18.0,   // power
                    75.0,   // efficiency
                    20000.0,// rod_stress
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getDynacardAnalysis());
            assertTrue(result.getDynacardAnalysis().contains("optimal range"));
        }

        @Test
        @DisplayName("Should use rod pump attributes when rodPumpAssetId provided")
        void shouldUseRodPumpAttributesWhenProvided() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> wellAttrs = createRodPumpAttributes(
                    8.0, 86.0, 80.0, 15000.0, 3000.0, 50.0, 90.0, 18.0, 70.0, 20000.0, 2.25
            );
            Map<String, Object> pumpAttrs = createRodPumpAttributes(
                    9.0, 90.0, 82.0, 16000.0, 3500.0, 52.0, 95.0, 19.0, 72.0, 21000.0, 2.25
            );

            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(wellAttrs);
            when(attributeService.getAttributesAsMap(rodPumpAssetId)).thenReturn(pumpAttrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, rodPumpAssetId);

            // Assert
            assertNotNull(result);
            assertEquals(rodPumpAssetId, result.getRodPumpAssetId());
            verify(attributeService).getAttributesAsMap(rodPumpAssetId);
        }

        @Test
        @DisplayName("Should mark as significant when counterbalance recommendation exists")
        void shouldMarkSignificantWithCounterbalanceRec() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getRodPump()).thenReturn(rpConfig);

            Map<String, Object> attrs = createRodPumpAttributes(
                    8.0,    // spm
                    86.0,   // stroke_length
                    84.5,   // fillage (close to target - minimal SPM change)
                    15000.0,// peak_load
                    3000.0, // min_load
                    38.0,   // counterbalance (low - needs adjustment)
                    90.0,   // production
                    18.0,   // power
                    74.0,   // efficiency
                    20000.0,// rod_stress
                    2.25    // pump_diameter
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            RodPumpOptimizationDto result = optimizer.optimize(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getIsSignificant(),
                    "Should be significant when counterbalance recommendation exists");
        }
    }

    @Nested
    @DisplayName("createRecommendation tests")
    class CreateRecommendationTests {

        @Test
        @DisplayName("Should return null when optimization is not significant")
        void shouldReturnNullWhenNotSignificant() {
            RodPumpOptimizationDto opt = RodPumpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-ROD-WELL-001")
                    .isSignificant(false)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNull(result);
        }

        @Test
        @DisplayName("Should create SPM recommendation for SPEED optimization")
        void shouldCreateSpmRecommendationForSpeedType() {
            RodPumpOptimizationDto opt = RodPumpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-ROD-WELL-001")
                    .currentSpm(BigDecimal.valueOf(8.0))
                    .recommendedSpm(BigDecimal.valueOf(6.5))
                    .currentStrokeLength(BigDecimal.valueOf(86))
                    .recommendedStrokeLength(BigDecimal.valueOf(86))
                    .currentFillage(BigDecimal.valueOf(60))
                    .expectedFillage(BigDecimal.valueOf(78))
                    .expectedProductionIncrease(BigDecimal.valueOf(-5))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(-5.5))
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(8.0))
                    .currentPumpEfficiency(BigDecimal.valueOf(65))
                    .expectedPumpEfficiency(BigDecimal.valueOf(73))
                    .confidence(0.80)
                    .optimizationType("SPEED")
                    .isSignificant(true)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertEquals(tenantId, result.getTenantId());
            assertEquals(wellAssetId, result.getAssetId());
            assertEquals(OptimizationType.ROD_PUMP_SPEED, result.getType());
            assertEquals(BigDecimal.valueOf(8.0), result.getCurrentValue());
            assertEquals(BigDecimal.valueOf(6.5), result.getRecommendedValue());
            assertEquals("SPM", result.getUnit());
            assertTrue(result.getTitle().contains("SPM"));
        }

        @Test
        @DisplayName("Should create stroke recommendation for STROKE optimization")
        void shouldCreateStrokeRecommendationForStrokeType() {
            RodPumpOptimizationDto opt = RodPumpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-ROD-WELL-001")
                    .currentSpm(BigDecimal.valueOf(8.0))
                    .recommendedSpm(BigDecimal.valueOf(8.0))
                    .currentStrokeLength(BigDecimal.valueOf(86))
                    .recommendedStrokeLength(BigDecimal.valueOf(100))
                    .currentFillage(BigDecimal.valueOf(75))
                    .expectedFillage(BigDecimal.valueOf(80))
                    .expectedProductionIncrease(BigDecimal.valueOf(15))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(12.5))
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(5.0))
                    .currentPumpEfficiency(BigDecimal.valueOf(70))
                    .expectedPumpEfficiency(BigDecimal.valueOf(75))
                    .confidence(0.85)
                    .optimizationType("STROKE")
                    .isSignificant(true)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertEquals(BigDecimal.valueOf(86), result.getCurrentValue());
            assertEquals(BigDecimal.valueOf(100), result.getRecommendedValue());
            assertEquals("inches", result.getUnit());
            assertTrue(result.getTitle().contains("stroke"));
        }

        @Test
        @DisplayName("Should set priority based on production increase")
        void shouldSetPriorityBasedOnProductionIncrease() {
            RodPumpOptimizationDto highImpact = RodPumpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("HIGH-IMPACT")
                    .currentSpm(BigDecimal.valueOf(6))
                    .recommendedSpm(BigDecimal.valueOf(9))
                    .expectedProductionIncrease(BigDecimal.valueOf(20))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(15.0)) // >10%
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(8.0))
                    .optimizationType("SPEED")
                    .confidence(0.85)
                    .isSignificant(true)
                    .build();

            RodPumpOptimizationDto lowImpact = RodPumpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("LOW-IMPACT")
                    .currentSpm(BigDecimal.valueOf(8))
                    .recommendedSpm(BigDecimal.valueOf(7.5))
                    .expectedProductionIncrease(BigDecimal.valueOf(-2))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(-1.5)) // <2%
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(2.0))
                    .optimizationType("SPEED")
                    .confidence(0.80)
                    .isSignificant(true)
                    .build();

            RecommendationDto highRec = optimizer.createRecommendation(tenantId, highImpact);
            RecommendationDto lowRec = optimizer.createRecommendation(tenantId, lowImpact);

            assertNotNull(highRec);
            assertNotNull(lowRec);
            assertEquals(1, highRec.getPriority()); // Highest priority
            assertEquals(4, lowRec.getPriority());  // Lower priority
        }

        @Test
        @DisplayName("Should include fillage and efficiency in description")
        void shouldIncludeFillageAndEfficiencyInDescription() {
            RodPumpOptimizationDto opt = RodPumpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-ROD-WELL-001")
                    .currentSpm(BigDecimal.valueOf(10))
                    .recommendedSpm(BigDecimal.valueOf(7))
                    .currentStrokeLength(BigDecimal.valueOf(86))
                    .recommendedStrokeLength(BigDecimal.valueOf(86))
                    .currentFillage(BigDecimal.valueOf(55))
                    .expectedFillage(BigDecimal.valueOf(78))
                    .currentPeakLoad(BigDecimal.valueOf(18000))
                    .currentRodStress(BigDecimal.valueOf(22000))
                    .expectedProductionIncrease(BigDecimal.valueOf(-8))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(-7.0))
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(12.0))
                    .currentPumpEfficiency(BigDecimal.valueOf(60))
                    .expectedPumpEfficiency(BigDecimal.valueOf(72))
                    .confidence(0.75)
                    .optimizationType("SPEED")
                    .dynacardAnalysis("Pump underfilled. Recommend reducing SPM.")
                    .isSignificant(true)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertNotNull(result.getDescription());
            assertTrue(result.getDescription().contains("Fillage"));
            assertTrue(result.getDescription().contains("Efficiency"));
        }
    }

    // Helper methods

    private Map<String, Object> createRodPumpAttributes(
            double spm, double strokeLength, double fillage, double peakLoad,
            double minLoad, double counterbalance, double production, double power,
            double efficiency, double rodStress, double pumpDiameter) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("spm", spm);
        attrs.put("stroke_length", strokeLength);
        attrs.put("fillage", fillage);
        attrs.put("peak_load", peakLoad);
        attrs.put("min_load", minLoad);
        attrs.put("counterbalance", counterbalance);
        attrs.put("current_production_bpd", production);
        attrs.put("power_kw", power);
        attrs.put("pump_efficiency", efficiency);
        attrs.put("rod_stress", rodStress);
        attrs.put("pump_diameter", pumpDiameter);
        return attrs;
    }
}
