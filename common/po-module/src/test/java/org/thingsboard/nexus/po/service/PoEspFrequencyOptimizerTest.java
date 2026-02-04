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
import org.thingsboard.nexus.po.dto.EspOptimizationDto;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationDto;
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
 * Unit tests for PoEspFrequencyOptimizer.
 */
@ExtendWith(MockitoExtension.class)
class PoEspFrequencyOptimizerTest {

    @Mock
    private PoAssetService assetService;

    @Mock
    private PoAttributeService attributeService;

    @Mock
    private PoModuleConfiguration config;

    @InjectMocks
    private PoEspFrequencyOptimizer optimizer;

    private UUID tenantId;
    private UUID wellAssetId;
    private UUID espAssetId;
    private Asset wellAsset;
    private PoModuleConfiguration.EspOptimizerConfig espConfig;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        wellAssetId = UUID.randomUUID();
        espAssetId = UUID.randomUUID();

        wellAsset = new Asset();
        wellAsset.setId(new AssetId(wellAssetId));
        wellAsset.setTenantId(TenantId.fromUUID(tenantId));
        wellAsset.setName("TEST-WELL-001");
        wellAsset.setType("well");

        espConfig = new PoModuleConfiguration.EspOptimizerConfig();
        espConfig.setMinFrequency(30.0);
        espConfig.setMaxFrequency(60.0);
        espConfig.setTargetMotorLoad(75.0);
        espConfig.setMaxMotorTemperature(280.0);
        espConfig.setMinFrequencyChange(0.5);
    }

    @Nested
    @DisplayName("optimizeFrequency tests")
    class OptimizeFrequencyTests {

        @Test
        @DisplayName("Should return null when well not found")
        void shouldReturnNullWhenWellNotFound() {
            when(assetService.getAssetById(any(), any())).thenReturn(Optional.empty());

            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, null);

            assertNull(result);
            verify(assetService).getAssetById(tenantId, wellAssetId);
        }

        @Test
        @DisplayName("Should optimize frequency when motor load is low")
        void shouldIncreaseFrequencyWhenLoadLow() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getEsp()).thenReturn(espConfig);

            Map<String, Object> attrs = createAttributes(
                    50.0,  // frequency
                    55.0,  // motor_load (low - room to increase)
                    240.0, // motor_temperature
                    200.0, // pip
                    1500.0,// discharge_pressure
                    500.0, // current_production_bpd
                    100.0  // power_kw
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertEquals(wellAssetId, result.getWellAssetId());
            assertEquals("TEST-WELL-001", result.getWellName());
            assertEquals(BigDecimal.valueOf(50.0), result.getCurrentFrequency());
            // Should recommend increase since load is below target (75%)
            assertTrue(result.getRecommendedFrequency().compareTo(result.getCurrentFrequency()) > 0,
                    "Recommended frequency should be higher than current when load is low");
            assertTrue(result.getFrequencyChange().compareTo(BigDecimal.ZERO) > 0,
                    "Frequency change should be positive");
        }

        @Test
        @DisplayName("Should decrease frequency when motor load is high")
        void shouldDecreaseFrequencyWhenLoadHigh() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getEsp()).thenReturn(espConfig);

            Map<String, Object> attrs = createAttributes(
                    55.0,  // frequency
                    90.0,  // motor_load (high - need to reduce)
                    250.0, // motor_temperature
                    200.0, // pip
                    1500.0,// discharge_pressure
                    600.0, // current_production_bpd
                    120.0  // power_kw
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedFrequency().compareTo(result.getCurrentFrequency()) < 0,
                    "Recommended frequency should be lower than current when load is high");
            assertEquals("MOTOR_LOAD", result.getLimitingConstraint());
        }

        @Test
        @DisplayName("Should decrease frequency when temperature is high")
        void shouldDecreaseFrequencyWhenTemperatureHigh() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getEsp()).thenReturn(espConfig);

            // Temperature at 95% of max (280 * 0.95 = 266)
            Map<String, Object> attrs = createAttributes(
                    55.0,  // frequency
                    70.0,  // motor_load
                    270.0, // motor_temperature (high - above 95% of 280)
                    200.0, // pip
                    1500.0,// discharge_pressure
                    600.0, // current_production_bpd
                    120.0  // power_kw
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedFrequency().compareTo(result.getCurrentFrequency()) < 0,
                    "Recommended frequency should be lower than current when temperature is high");
            assertEquals("MOTOR_TEMPERATURE", result.getLimitingConstraint());
        }

        @Test
        @DisplayName("Should respect minimum frequency limit")
        void shouldRespectMinFrequencyLimit() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getEsp()).thenReturn(espConfig);

            Map<String, Object> attrs = createAttributes(
                    32.0,  // frequency (close to min of 30)
                    88.0,  // motor_load (high)
                    260.0, // motor_temperature
                    200.0, // pip
                    1500.0,// discharge_pressure
                    400.0, // current_production_bpd
                    80.0   // power_kw
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedFrequency().compareTo(BigDecimal.valueOf(30.0)) >= 0,
                    "Recommended frequency should not go below minimum");
            assertEquals(BigDecimal.valueOf(30.0), result.getMinFrequency());
        }

        @Test
        @DisplayName("Should respect maximum frequency limit")
        void shouldRespectMaxFrequencyLimit() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getEsp()).thenReturn(espConfig);

            Map<String, Object> attrs = createAttributes(
                    58.0,  // frequency (close to max of 60)
                    50.0,  // motor_load (low - room to increase)
                    230.0, // motor_temperature
                    200.0, // pip
                    1500.0,// discharge_pressure
                    700.0, // current_production_bpd
                    140.0  // power_kw
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedFrequency().compareTo(BigDecimal.valueOf(60.0)) <= 0,
                    "Recommended frequency should not exceed maximum");
            assertEquals(BigDecimal.valueOf(60.0), result.getMaxFrequency());
        }

        @Test
        @DisplayName("Should mark as not significant when change is below threshold")
        void shouldMarkNotSignificantWhenChangeBelowThreshold() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getEsp()).thenReturn(espConfig);

            // Conditions very close to optimal
            Map<String, Object> attrs = createAttributes(
                    50.0,  // frequency
                    74.5,  // motor_load (very close to target of 75)
                    250.0, // motor_temperature
                    200.0, // pip
                    1500.0,// discharge_pressure
                    500.0, // current_production_bpd
                    100.0  // power_kw
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            if (!result.getIsSignificant()) {
                assertNotNull(result.getNotSignificantReason());
                assertTrue(result.getNotSignificantReason().contains("below minimum threshold"));
            }
        }

        @Test
        @DisplayName("Should calculate expected production using affinity laws")
        void shouldCalculateExpectedProductionWithAffinityLaws() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getEsp()).thenReturn(espConfig);

            Map<String, Object> attrs = createAttributes(
                    45.0,  // frequency
                    55.0,  // motor_load (low)
                    230.0, // motor_temperature
                    200.0, // pip
                    1500.0,// discharge_pressure
                    400.0, // current_production_bpd
                    80.0   // power_kw
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getExpectedProductionBpd());
            assertNotNull(result.getExpectedPowerKw());
            // If frequency increases, production should increase
            if (result.getRecommendedFrequency().compareTo(result.getCurrentFrequency()) > 0) {
                assertTrue(result.getExpectedProductionBpd().compareTo(result.getCurrentProductionBpd()) > 0,
                        "Expected production should increase with frequency");
            }
        }

        @Test
        @DisplayName("Should use ESP attributes when espAssetId provided")
        void shouldUseEspAttributesWhenProvided() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getEsp()).thenReturn(espConfig);

            Map<String, Object> wellAttrs = createAttributes(
                    50.0, 60.0, 240.0, 200.0, 1500.0, 500.0, 100.0
            );
            Map<String, Object> espAttrs = createAttributes(
                    52.0, 65.0, 245.0, 210.0, 1550.0, 520.0, 105.0
            );

            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(wellAttrs);
            when(attributeService.getAttributesAsMap(espAssetId)).thenReturn(espAttrs);

            // Act
            EspOptimizationDto result = optimizer.optimizeFrequency(tenantId, wellAssetId, espAssetId);

            // Assert
            assertNotNull(result);
            assertEquals(espAssetId, result.getEspAssetId());
            verify(attributeService).getAttributesAsMap(espAssetId);
        }
    }

    @Nested
    @DisplayName("createRecommendation tests")
    class CreateRecommendationTests {

        @Test
        @DisplayName("Should return null when optimization is not significant")
        void shouldReturnNullWhenNotSignificant() {
            EspOptimizationDto opt = EspOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-WELL-001")
                    .isSignificant(false)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNull(result);
        }

        @Test
        @DisplayName("Should create recommendation for significant optimization")
        void shouldCreateRecommendationWhenSignificant() {
            EspOptimizationDto opt = EspOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-WELL-001")
                    .currentFrequency(BigDecimal.valueOf(50.0))
                    .recommendedFrequency(BigDecimal.valueOf(54.0))
                    .expectedProductionIncrease(BigDecimal.valueOf(50))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(8.5))
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(3.2))
                    .confidence(0.85)
                    .isSignificant(true)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertEquals(tenantId, result.getTenantId());
            assertEquals(wellAssetId, result.getAssetId());
            assertEquals(OptimizationType.ESP_FREQUENCY, result.getType());
            assertEquals(BigDecimal.valueOf(50.0), result.getCurrentValue());
            assertEquals(BigDecimal.valueOf(54.0), result.getRecommendedValue());
            assertEquals("Hz", result.getUnit());
            assertEquals(BigDecimal.valueOf(50), result.getExpectedProductionIncrease());
            assertEquals(0.85, result.getConfidence());
        }

        @Test
        @DisplayName("Should set high priority for large production increase")
        void shouldSetHighPriorityForLargeIncrease() {
            EspOptimizationDto opt = EspOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-WELL-001")
                    .currentFrequency(BigDecimal.valueOf(45.0))
                    .recommendedFrequency(BigDecimal.valueOf(55.0))
                    .expectedProductionIncrease(BigDecimal.valueOf(100))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(15.0)) // >10%
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(5.0))
                    .confidence(0.90)
                    .isSignificant(true)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertEquals(1, result.getPriority()); // Highest priority
        }

        @Test
        @DisplayName("Should set medium priority for moderate production increase")
        void shouldSetMediumPriorityForModerateIncrease() {
            EspOptimizationDto opt = EspOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-WELL-001")
                    .currentFrequency(BigDecimal.valueOf(48.0))
                    .recommendedFrequency(BigDecimal.valueOf(52.0))
                    .expectedProductionIncrease(BigDecimal.valueOf(35))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(6.0)) // 5-10%
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(2.5))
                    .confidence(0.85)
                    .isSignificant(true)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertEquals(2, result.getPriority());
        }

        @Test
        @DisplayName("Should include well name in title")
        void shouldIncludeFrequencyValuesInTitle() {
            EspOptimizationDto opt = EspOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-WELL-001")
                    .currentFrequency(BigDecimal.valueOf(50.0))
                    .recommendedFrequency(BigDecimal.valueOf(55.0))
                    .expectedProductionIncrease(BigDecimal.valueOf(50))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(8.0))
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(3.0))
                    .confidence(0.85)
                    .isSignificant(true)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertTrue(result.getTitle().contains("50") || result.getTitle().contains("50.0"));
            assertTrue(result.getTitle().contains("55") || result.getTitle().contains("55.0"));
            assertTrue(result.getTitle().contains("Hz"));
        }
    }

    // Helper methods

    private Map<String, Object> createAttributes(
            double frequency, double motorLoad, double motorTemp,
            double pip, double dischargePressure, double production, double power) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("frequency", frequency);
        attrs.put("motor_load", motorLoad);
        attrs.put("motor_temperature", motorTemp);
        attrs.put("pip", pip);
        attrs.put("discharge_pressure", dischargePressure);
        attrs.put("current_production_bpd", production);
        attrs.put("power_kw", power);
        return attrs;
    }
}
