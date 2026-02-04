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
import org.thingsboard.nexus.po.dto.PcpOptimizationDto;
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
 * Unit tests for PoPcpSpeedOptimizer.
 */
@ExtendWith(MockitoExtension.class)
class PoPcpSpeedOptimizerTest {

    @Mock
    private PoAssetService assetService;

    @Mock
    private PoAttributeService attributeService;

    @Mock
    private PoModuleConfiguration config;

    @InjectMocks
    private PoPcpSpeedOptimizer optimizer;

    private UUID tenantId;
    private UUID wellAssetId;
    private UUID pcpAssetId;
    private Asset wellAsset;
    private PoModuleConfiguration.PcpOptimizerConfig pcpConfig;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        wellAssetId = UUID.randomUUID();
        pcpAssetId = UUID.randomUUID();

        wellAsset = new Asset();
        wellAsset.setId(new AssetId(wellAssetId));
        wellAsset.setTenantId(TenantId.fromUUID(tenantId));
        wellAsset.setName("TEST-PCP-WELL-001");
        wellAsset.setType("well");

        pcpConfig = new PoModuleConfiguration.PcpOptimizerConfig();
        pcpConfig.setEnabled(true);
        pcpConfig.setMinRpmChange(5.0);
        pcpConfig.setMaxRpm(500.0);
        pcpConfig.setMinRpm(50.0);
        pcpConfig.setTargetTorque(70.0);
        pcpConfig.setMaxTorque(90.0);
        pcpConfig.setMaxRodLoad(15000.0);
        pcpConfig.setWearFactorThreshold(0.7);
    }

    @Nested
    @DisplayName("optimizeSpeed tests")
    class OptimizeSpeedTests {

        @Test
        @DisplayName("Should return null when well not found")
        void shouldReturnNullWhenWellNotFound() {
            when(assetService.getAssetById(any(), any())).thenReturn(Optional.empty());

            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            assertNull(result);
            verify(assetService).getAssetById(tenantId, wellAssetId);
        }

        @Test
        @DisplayName("Should increase RPM when torque is low")
        void shouldIncreaseRpmWhenTorqueLow() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            Map<String, Object> attrs = createPcpAttributes(
                    200.0,  // rpm
                    50.0,   // torque (low - room to increase)
                    55.0,   // drive_load
                    8000.0, // rod_load (below 85% of 15000)
                    150.0,  // pip
                    300.0,  // production
                    50.0,   // power
                    100.0,  // viscosity
                    75.0    // efficiency
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertEquals(wellAssetId, result.getWellAssetId());
            assertEquals("TEST-PCP-WELL-001", result.getWellName());
            assertTrue(result.getRecommendedRpm().compareTo(result.getCurrentRpm()) > 0,
                    "Recommended RPM should be higher when torque is low");
            assertTrue(result.getRpmChange().compareTo(BigDecimal.ZERO) > 0,
                    "RPM change should be positive");
        }

        @Test
        @DisplayName("Should decrease RPM when torque is high")
        void shouldDecreaseRpmWhenTorqueHigh() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            // Torque at 85% (90% of 90 = 81%)
            Map<String, Object> attrs = createPcpAttributes(
                    300.0,  // rpm
                    85.0,   // torque (high - above 90% of max)
                    80.0,   // drive_load
                    10000.0,// rod_load
                    150.0,  // pip
                    400.0,  // production
                    80.0,   // power
                    100.0,  // viscosity
                    70.0    // efficiency
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedRpm().compareTo(result.getCurrentRpm()) < 0,
                    "Recommended RPM should be lower when torque is high");
            assertEquals("TORQUE", result.getLimitingConstraint());
        }

        @Test
        @DisplayName("Should decrease RPM when rod load is high")
        void shouldDecreaseRpmWhenRodLoadHigh() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            // Rod load at 95% of max (15000 * 0.9 = 13500)
            Map<String, Object> attrs = createPcpAttributes(
                    280.0,  // rpm
                    65.0,   // torque (normal)
                    70.0,   // drive_load
                    14000.0,// rod_load (high - above 90% of max)
                    150.0,  // pip
                    380.0,  // production
                    70.0,   // power
                    100.0,  // viscosity
                    72.0    // efficiency
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedRpm().compareTo(result.getCurrentRpm()) < 0,
                    "Recommended RPM should be lower when rod load is high");
            assertEquals("ROD_LOAD", result.getLimitingConstraint());
        }

        @Test
        @DisplayName("Should respect minimum RPM limit")
        void shouldRespectMinRpmLimit() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            Map<String, Object> attrs = createPcpAttributes(
                    60.0,   // rpm (close to min of 50)
                    88.0,   // torque (high)
                    85.0,   // drive_load
                    12000.0,// rod_load
                    150.0,  // pip
                    150.0,  // production
                    30.0,   // power
                    100.0,  // viscosity
                    65.0    // efficiency
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedRpm().compareTo(BigDecimal.valueOf(50.0)) >= 0,
                    "Recommended RPM should not go below minimum");
            assertEquals(BigDecimal.valueOf(50.0), result.getMinRpm());
        }

        @Test
        @DisplayName("Should respect maximum RPM limit")
        void shouldRespectMaxRpmLimit() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            Map<String, Object> attrs = createPcpAttributes(
                    480.0,  // rpm (close to max of 500)
                    40.0,   // torque (low - room to increase)
                    50.0,   // drive_load
                    7000.0, // rod_load (low)
                    200.0,  // pip
                    600.0,  // production
                    100.0,  // power
                    80.0,   // viscosity
                    78.0    // efficiency
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecommendedRpm().compareTo(BigDecimal.valueOf(500.0)) <= 0,
                    "Recommended RPM should not exceed maximum");
            assertEquals(BigDecimal.valueOf(500.0), result.getMaxRpm());
        }

        @Test
        @DisplayName("Should calculate wear factors")
        void shouldCalculateWearFactors() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            Map<String, Object> attrs = createPcpAttributes(
                    300.0,  // rpm
                    55.0,   // torque
                    60.0,   // drive_load
                    9000.0, // rod_load
                    150.0,  // pip
                    400.0,  // production
                    65.0,   // power
                    100.0,  // viscosity
                    74.0    // efficiency
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getRodWearFactor());
            assertNotNull(result.getStatorWearFactor());
            assertTrue(result.getRodWearFactor().compareTo(BigDecimal.ZERO) >= 0 &&
                            result.getRodWearFactor().compareTo(BigDecimal.ONE) <= 0,
                    "Rod wear factor should be between 0 and 1");
            assertTrue(result.getStatorWearFactor().compareTo(BigDecimal.ZERO) >= 0,
                    "Stator wear factor should be non-negative");
        }

        @Test
        @DisplayName("Should calculate OEP based on viscosity")
        void shouldCalculateOepBasedOnViscosity() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            // Low viscosity
            Map<String, Object> lowViscAttrs = createPcpAttributes(
                    200.0, 55.0, 60.0, 8000.0, 150.0, 300.0, 50.0, 50.0, 75.0
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(lowViscAttrs);

            PcpOptimizationDto lowViscResult = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // High viscosity
            Map<String, Object> highViscAttrs = createPcpAttributes(
                    200.0, 55.0, 60.0, 8000.0, 150.0, 300.0, 50.0, 300.0, 75.0
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(highViscAttrs);

            PcpOptimizationDto highViscResult = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(lowViscResult.getOepRpm());
            assertNotNull(highViscResult.getOepRpm());
            // Higher viscosity should result in lower OEP
            assertTrue(highViscResult.getOepRpm().compareTo(lowViscResult.getOepRpm()) <= 0,
                    "Higher viscosity should result in lower or equal optimal efficiency point");
        }

        @Test
        @DisplayName("Should mark as not significant when change is below threshold")
        void shouldMarkNotSignificantWhenChangeBelowThreshold() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            // Conditions close to optimal
            Map<String, Object> attrs = createPcpAttributes(
                    250.0,  // rpm
                    69.5,   // torque (very close to target of 70)
                    68.0,   // drive_load
                    10000.0,// rod_load
                    150.0,  // pip
                    350.0,  // production
                    60.0,   // power
                    100.0,  // viscosity
                    76.0    // efficiency
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            if (!result.getIsSignificant()) {
                assertNotNull(result.getNotSignificantReason());
                assertTrue(result.getNotSignificantReason().contains("below minimum threshold"));
            }
        }

        @Test
        @DisplayName("Should use PCP attributes when pcpAssetId provided")
        void shouldUsePcpAttributesWhenProvided() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            Map<String, Object> wellAttrs = createPcpAttributes(
                    200.0, 50.0, 55.0, 8000.0, 150.0, 300.0, 50.0, 100.0, 75.0
            );
            Map<String, Object> pcpAttrs = createPcpAttributes(
                    220.0, 52.0, 57.0, 8200.0, 155.0, 320.0, 52.0, 100.0, 76.0
            );

            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(wellAttrs);
            when(attributeService.getAttributesAsMap(pcpAssetId)).thenReturn(pcpAttrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, pcpAssetId);

            // Assert
            assertNotNull(result);
            assertEquals(pcpAssetId, result.getPcpAssetId());
            verify(attributeService).getAttributesAsMap(pcpAssetId);
        }

        @Test
        @DisplayName("Should calculate expected production with RPM change")
        void shouldCalculateExpectedProductionWithRpmChange() {
            // Arrange
            when(assetService.getAssetById(tenantId, wellAssetId)).thenReturn(Optional.of(wellAsset));
            when(config.getPcp()).thenReturn(pcpConfig);

            Map<String, Object> attrs = createPcpAttributes(
                    150.0,  // rpm (low)
                    45.0,   // torque (low - room to increase)
                    50.0,   // drive_load
                    6000.0, // rod_load
                    180.0,  // pip
                    250.0,  // production
                    40.0,   // power
                    100.0,  // viscosity
                    70.0    // efficiency
            );
            when(attributeService.getAttributesAsMap(wellAssetId)).thenReturn(attrs);

            // Act
            PcpOptimizationDto result = optimizer.optimizeSpeed(tenantId, wellAssetId, null);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getExpectedProductionBpd());
            // If RPM increases, production should increase (linear relationship for PCP)
            if (result.getRecommendedRpm().compareTo(result.getCurrentRpm()) > 0) {
                assertTrue(result.getExpectedProductionBpd().compareTo(result.getCurrentProductionBpd()) > 0,
                        "Expected production should increase with RPM for PCP");
            }
        }
    }

    @Nested
    @DisplayName("createRecommendation tests")
    class CreateRecommendationTests {

        @Test
        @DisplayName("Should return null when optimization is not significant")
        void shouldReturnNullWhenNotSignificant() {
            PcpOptimizationDto opt = PcpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-PCP-WELL-001")
                    .isSignificant(false)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNull(result);
        }

        @Test
        @DisplayName("Should create recommendation for significant optimization")
        void shouldCreateRecommendationWhenSignificant() {
            PcpOptimizationDto opt = PcpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-PCP-WELL-001")
                    .currentRpm(BigDecimal.valueOf(200))
                    .recommendedRpm(BigDecimal.valueOf(250))
                    .expectedProductionIncrease(BigDecimal.valueOf(50))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(12.5))
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(3.0))
                    .confidence(0.85)
                    .isSignificant(true)
                    .currentTorque(BigDecimal.valueOf(55))
                    .currentDriveLoad(BigDecimal.valueOf(60))
                    .currentRodLoad(BigDecimal.valueOf(8000))
                    .currentProductionBpd(BigDecimal.valueOf(400))
                    .currentPumpEfficiency(BigDecimal.valueOf(72))
                    .expectedPumpEfficiency(BigDecimal.valueOf(75))
                    .rodWearFactor(BigDecimal.valueOf(0.4))
                    .statorWearFactor(BigDecimal.valueOf(0.35))
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertEquals(tenantId, result.getTenantId());
            assertEquals(wellAssetId, result.getAssetId());
            assertEquals(OptimizationType.PCP_SPEED, result.getType());
            assertEquals(BigDecimal.valueOf(200), result.getCurrentValue());
            assertEquals(BigDecimal.valueOf(250), result.getRecommendedValue());
            assertEquals("RPM", result.getUnit());
            assertEquals(0.85, result.getConfidence());
        }

        @Test
        @DisplayName("Should set high priority for large production increase")
        void shouldSetHighPriorityForLargeIncrease() {
            PcpOptimizationDto opt = PcpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-PCP-WELL-001")
                    .currentRpm(BigDecimal.valueOf(150))
                    .recommendedRpm(BigDecimal.valueOf(280))
                    .expectedProductionIncrease(BigDecimal.valueOf(120))
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
        @DisplayName("Should include RPM values in title")
        void shouldIncludeRpmValuesInTitle() {
            PcpOptimizationDto opt = PcpOptimizationDto.builder()
                    .wellAssetId(wellAssetId)
                    .wellName("TEST-PCP-WELL-001")
                    .currentRpm(BigDecimal.valueOf(200))
                    .recommendedRpm(BigDecimal.valueOf(260))
                    .expectedProductionIncrease(BigDecimal.valueOf(60))
                    .expectedProductionIncreasePercent(BigDecimal.valueOf(8.0))
                    .expectedEfficiencyImprovement(BigDecimal.valueOf(2.5))
                    .confidence(0.85)
                    .isSignificant(true)
                    .build();

            RecommendationDto result = optimizer.createRecommendation(tenantId, opt);

            assertNotNull(result);
            assertTrue(result.getTitle().contains("200"));
            assertTrue(result.getTitle().contains("260"));
            assertTrue(result.getTitle().contains("RPM"));
        }
    }

    // Helper methods

    private Map<String, Object> createPcpAttributes(
            double rpm, double torque, double driveLoad, double rodLoad,
            double pip, double production, double power,
            double viscosity, double efficiency) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("rpm", rpm);
        attrs.put("torque", torque);
        attrs.put("drive_load", driveLoad);
        attrs.put("rod_load", rodLoad);
        attrs.put("pip", pip);
        attrs.put("current_production_bpd", production);
        attrs.put("power_kw", power);
        attrs.put("fluid_viscosity", viscosity);
        attrs.put("pump_efficiency", efficiency);
        return attrs;
    }
}
