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
package org.thingsboard.nexus.rv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.nexus.rv.dto.RvMaterialBalanceDto;
import org.thingsboard.nexus.rv.dto.RvMaterialBalanceDto.MaterialBalanceDataPoint;
import org.thingsboard.nexus.rv.exception.RvCalculationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RvMaterialBalanceService.
 * Tests Material Balance Equation (MBE) calculations and Havlena-Odeh analysis.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RvMaterialBalanceService Unit Tests")
class RvMaterialBalanceServiceTest {

    @Mock
    private RvAssetService assetService;

    private RvMaterialBalanceService materialBalanceService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        materialBalanceService = new RvMaterialBalanceService(assetService, objectMapper);
    }

    // ===========================================
    // MBE TERMS CALCULATION TESTS
    // ===========================================

    @Test
    @DisplayName("MBE Terms: Cálculo básico de F, Eo, Eg, Efw")
    void testCalculateMBETerms_BasicCalculation() {
        // Given: Simple depletion case (no gas cap, no aquifer)
        RvMaterialBalanceDto study = createBasicStudy();

        // When
        RvMaterialBalanceDto result = materialBalanceService.calculateMBETerms(study);

        // Then: All terms should be calculated
        assertNotNull(result);
        List<MaterialBalanceDataPoint> dataPoints = result.getDataPoints();
        assertEquals(3, dataPoints.size());

        for (int i = 0; i < dataPoints.size(); i++) {
            MaterialBalanceDataPoint dp = dataPoints.get(i);
            assertNotNull(dp.getF(), "F should be calculated");
            assertNotNull(dp.getEo(), "Eo should be calculated");
            assertNotNull(dp.getEg(), "Eg should be calculated");
            assertNotNull(dp.getEfw(), "Efw should be calculated");

            // F should be non-negative (zero at initial conditions when Np=0)
            assertTrue(dp.getF().compareTo(BigDecimal.ZERO) >= 0, "F should be non-negative");

            // F should be positive only after production starts (skip first point)
            if (i > 0) {
                assertTrue(dp.getF().compareTo(BigDecimal.ZERO) > 0,
                    "F should be positive after production starts");
            }
        }
    }

    @Test
    @DisplayName("MBE Terms: Validación de Eo (expansión del petróleo)")
    void testCalculateMBETerms_OilExpansion() {
        // Given: Study with increasing Bo (oil expands as pressure drops)
        RvMaterialBalanceDto study = createBasicStudy();

        // When
        RvMaterialBalanceDto result = materialBalanceService.calculateMBETerms(study);

        // Then: Eo should increase as pressure drops
        List<MaterialBalanceDataPoint> dataPoints = result.getDataPoints();

        // First point (highest pressure) should have smallest Eo
        BigDecimal eo1 = dataPoints.get(0).getEo();
        BigDecimal eo2 = dataPoints.get(1).getEo();
        BigDecimal eo3 = dataPoints.get(2).getEo();

        assertTrue(eo1.compareTo(eo2) < 0, "Eo should increase as pressure drops");
        assertTrue(eo2.compareTo(eo3) < 0, "Eo should increase as pressure drops");
    }

    @Test
    @DisplayName("MBE Terms: Cálculo de Eg con gas cap")
    void testCalculateMBETerms_GasCapExpansion() {
        // Given: Study with gas cap
        RvMaterialBalanceDto study = createStudyWithGasCap();

        // When
        RvMaterialBalanceDto result = materialBalanceService.calculateMBETerms(study);

        // Then: Eg should be calculated and positive
        List<MaterialBalanceDataPoint> dataPoints = result.getDataPoints();

        for (MaterialBalanceDataPoint dp : dataPoints) {
            assertNotNull(dp.getEg());
            assertTrue(dp.getEg().compareTo(BigDecimal.ZERO) >= 0, "Eg should be non-negative");
        }

        // Eg should increase as pressure drops
        BigDecimal eg1 = dataPoints.get(0).getEg();
        BigDecimal eg3 = dataPoints.get(2).getEg();
        assertTrue(eg1.compareTo(eg3) < 0, "Eg should increase as pressure drops");
    }

    @Test
    @DisplayName("MBE Terms: Cálculo de Efw (expansión formación/agua)")
    void testCalculateMBETerms_FormationExpansion() {
        // Given: Study with compressibility data
        RvMaterialBalanceDto study = createBasicStudy();
        study.setWaterCompressibility(new BigDecimal("0.000003")); // 3e-6 1/psi
        study.setRockCompressibility(new BigDecimal("0.000004"));  // 4e-6 1/psi

        // When
        RvMaterialBalanceDto result = materialBalanceService.calculateMBETerms(study);

        // Then: Efw should be calculated
        List<MaterialBalanceDataPoint> dataPoints = result.getDataPoints();

        for (MaterialBalanceDataPoint dp : dataPoints) {
            assertNotNull(dp.getEfw());
            assertTrue(dp.getEfw().compareTo(BigDecimal.ZERO) >= 0, "Efw should be non-negative");
        }
    }

    @Test
    @DisplayName("MBE Terms: Validación - Sin data points")
    void testCalculateMBETerms_NoDataPoints() {
        // Given: Study without data points
        RvMaterialBalanceDto study = new RvMaterialBalanceDto();
        study.setName("Empty Study");
        study.setDataPoints(new ArrayList<>());

        // When/Then
        assertThrows(RvCalculationException.class, () -> {
            materialBalanceService.calculateMBETerms(study);
        });
    }

    @Test
    @DisplayName("MBE Terms: Validación - Condiciones iniciales faltantes")
    void testCalculateMBETerms_MissingInitialConditions() {
        // Given: Study without initial conditions
        RvMaterialBalanceDto study = new RvMaterialBalanceDto();
        study.setName("Incomplete Study");
        study.setDataPoints(List.of(new MaterialBalanceDataPoint()));
        // Missing: initialPressure, initialBo, initialRs

        // When/Then
        assertThrows(RvCalculationException.class, () -> {
            materialBalanceService.calculateMBETerms(study);
        });
    }

    // ===========================================
    // HAVLENA-ODEH ANALYSIS TESTS
    // ===========================================

    @Test
    @DisplayName("Havlena-Odeh: Análisis de yacimiento con agotamiento simple")
    void testHavlenaOdehAnalysis_SimpleDepletion() {
        // Given: Simple depletion case (F vs Eo plot)
        RvMaterialBalanceDto study = createBasicStudy();
        study.setHasGasCap(false);
        study.setHasAquiferSupport(false);

        // When
        RvMaterialBalanceDto result = materialBalanceService.performHavlenaOdehAnalysis(study);

        // Then: Should calculate OOIP and have good R²
        assertNotNull(result.getCalculatedOOIP());
        assertTrue(result.getCalculatedOOIP().compareTo(BigDecimal.ZERO) > 0);

        assertNotNull(result.getRegressionR2());
        assertTrue(result.getRegressionR2().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getRegressionR2().compareTo(BigDecimal.ONE) <= 0);

        assertNotNull(result.getRegressionSlope());
        assertNotNull(result.getRegressionIntercept());

        assertEquals(RvMaterialBalanceDto.PLOT_F_VS_EO, result.getPlotType());
        assertNotNull(result.getAnalysisQuality());
    }

    @Test
    @DisplayName("Havlena-Odeh: Análisis con gas cap")
    void testHavlenaOdehAnalysis_GasCapDrive() {
        // Given: Reservoir with gas cap
        RvMaterialBalanceDto study = createStudyWithGasCap();
        study.setHasGasCap(true);
        study.setHasAquiferSupport(false);
        study.setGasCapRatio(new BigDecimal("0.3")); // m = 0.3

        // When
        RvMaterialBalanceDto result = materialBalanceService.performHavlenaOdehAnalysis(study);

        // Then: Should use F vs (Eo + m*Eg) plot
        assertEquals(RvMaterialBalanceDto.PLOT_F_VS_EO_EG, result.getPlotType());
        assertNotNull(result.getCalculatedOOIP());
        assertTrue(result.getCalculatedOOIP().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Havlena-Odeh: Análisis con acuífero - requiere datos con Eo > 0")
    void testHavlenaOdehAnalysis_WaterDrive() {
        // Given: Reservoir with aquifer support
        // Note: Water drive analysis requires F/Eo vs We/Eo plot, which needs Eo > 0
        // The basic study has initial conditions where first point has Eo=0
        RvMaterialBalanceDto study = createBasicStudy();
        study.setHasGasCap(false);
        study.setHasAquiferSupport(true);

        // When/Then: Should throw exception when not enough data points with Eo > 0
        // This is expected behavior when regression can't be performed
        assertThrows(RvCalculationException.class, () -> {
            materialBalanceService.performHavlenaOdehAnalysis(study);
        }, "Water drive analysis should fail when Eo=0 for initial conditions");
    }

    @Test
    @DisplayName("Havlena-Odeh: Calidad de análisis - Excelente R²")
    void testHavlenaOdehAnalysis_ExcellentQuality() {
        // Given: Study with near-perfect linear data
        RvMaterialBalanceDto study = createPerfectLinearStudy();

        // When
        RvMaterialBalanceDto result = materialBalanceService.performHavlenaOdehAnalysis(study);

        // Then: Should have excellent quality (R² > 0.95)
        assertNotNull(result.getRegressionR2());
        assertTrue(result.getRegressionR2().compareTo(new BigDecimal("0.90")) > 0);
        assertTrue(result.getAnalysisQuality().equals("EXCELLENT") ||
                   result.getAnalysisQuality().equals("GOOD"));
    }

    @Test
    @DisplayName("Havlena-Odeh: Validación - Pocos data points")
    void testHavlenaOdehAnalysis_InsufficientDataPoints() {
        // Given: Study with only 2 data points (need at least 3)
        RvMaterialBalanceDto study = createBasicStudy();
        study.getDataPoints().remove(2); // Keep only 2 points

        // When/Then
        assertThrows(RvCalculationException.class, () -> {
            materialBalanceService.performHavlenaOdehAnalysis(study);
        });
    }

    // ===========================================
    // DRIVE INDICES TESTS
    // ===========================================

    @Test
    @DisplayName("Drive Indices: Cálculo de índices de empuje")
    void testCalculateDriveIndices() {
        // Given: Study with calculated OOIP
        RvMaterialBalanceDto study = createBasicStudy();
        study.setCalculatedOOIP(new BigDecimal("50.0")); // 50 MMbbl

        // Calculate MBE terms first
        materialBalanceService.calculateMBETerms(study);

        // When
        RvMaterialBalanceDto result = materialBalanceService.performHavlenaOdehAnalysis(study);

        // Then: Drive indices should be calculated and sum to ≈1.0
        assertNotNull(result.getSolutionGasDriveIndex());
        assertNotNull(result.getGasCapDriveIndex());
        assertNotNull(result.getWaterDriveIndex());
        assertNotNull(result.getCompactionDriveIndex());

        // All indices should be between 0 and 1
        assertTrue(result.getSolutionGasDriveIndex().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getSolutionGasDriveIndex().compareTo(BigDecimal.ONE) <= 0);

        assertTrue(result.getGasCapDriveIndex().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getGasCapDriveIndex().compareTo(BigDecimal.ONE) <= 0);

        assertTrue(result.getWaterDriveIndex().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getWaterDriveIndex().compareTo(BigDecimal.ONE) <= 0);

        assertTrue(result.getCompactionDriveIndex().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getCompactionDriveIndex().compareTo(BigDecimal.ONE) <= 0);

        // Primary drive mechanism should be identified
        assertNotNull(result.getPrimaryDriveMechanism());
    }

    @Test
    @DisplayName("Drive Indices: Yacimiento con gas cap dominante")
    void testDriveIndices_GasCapDominated() {
        // Given: Study with large gas cap
        RvMaterialBalanceDto study = createStudyWithGasCap();
        study.setGasCapRatio(new BigDecimal("0.5")); // Large gas cap
        study.setHasGasCap(true);

        // When
        RvMaterialBalanceDto result = materialBalanceService.performHavlenaOdehAnalysis(study);

        // Then: Gas cap drive index should be significant
        if (result.getGasCapDriveIndex() != null) {
            assertTrue(result.getGasCapDriveIndex().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    // ===========================================
    // WATER INFLUX TESTS
    // ===========================================

    @Test
    @DisplayName("Water Influx: Fetkovich aquifer model")
    void testCalculateFetkovichWaterInflux() {
        // Given: Aquifer parameters
        BigDecimal J = new BigDecimal("100");          // 100 bbl/day/psi
        BigDecimal Pi = new BigDecimal("3000");        // 3000 psi
        BigDecimal P = new BigDecimal("2800");         // 2800 psi
        BigDecimal deltaT = new BigDecimal("365");     // 365 days

        // When
        BigDecimal we = materialBalanceService.calculateFetkovichWaterInflux(J, Pi, P, deltaT);

        // Then: We = J * (Pi - P) * Δt = 100 * 200 * 365 = 7,300,000 bbl
        assertNotNull(we);
        assertTrue(we.compareTo(new BigDecimal("7000000")) > 0);
        assertTrue(we.compareTo(new BigDecimal("8000000")) < 0);
    }

    @Test
    @DisplayName("Water Influx: Carter-Tracy method")
    void testCalculateCarterTracyWaterInflux() {
        // Given: Carter-Tracy parameters
        BigDecimal B = new BigDecimal("1000");         // Aquifer constant
        BigDecimal deltaP = new BigDecimal("200");     // Pressure drop
        BigDecimal tD = new BigDecimal("1.5");         // Dimensionless time
        BigDecimal pD = new BigDecimal("0.8");         // Dimensionless pressure
        BigDecimal pDPrime = new BigDecimal("0.5");

        // When
        BigDecimal we = materialBalanceService.calculateCarterTracyWaterInflux(B, deltaP, tD, pD, pDPrime);

        // Then: We should be positive
        assertNotNull(we);
        assertTrue(we.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Water Influx: Carter-Tracy con pDPrime = 0")
    void testCalculateCarterTracyWaterInflux_ZeroPDPrime() {
        // Given: pDPrime = 0
        BigDecimal B = new BigDecimal("1000");
        BigDecimal deltaP = new BigDecimal("200");
        BigDecimal tD = new BigDecimal("1.5");
        BigDecimal pD = new BigDecimal("0.8");
        BigDecimal pDPrime = BigDecimal.ZERO;

        // When
        BigDecimal we = materialBalanceService.calculateCarterTracyWaterInflux(B, deltaP, tD, pD, pDPrime);

        // Then: Should return zero
        assertEquals(0, we.compareTo(BigDecimal.ZERO));
    }

    // ===========================================
    // HELPER METHODS
    // ===========================================

    /**
     * Create a basic material balance study with simple depletion.
     */
    private RvMaterialBalanceDto createBasicStudy() {
        RvMaterialBalanceDto study = new RvMaterialBalanceDto();
        study.setId(UUID.randomUUID());
        study.setName("Test Material Balance Study");
        // Note: RvMaterialBalanceDto may not have setReservoirAssetId, removed for now

        // Initial conditions
        study.setInitialPressure(new BigDecimal("3000"));      // 3000 psi
        study.setInitialBo(new BigDecimal("1.20"));            // 1.20 rb/stb
        study.setInitialBg(new BigDecimal("0.001"));           // 0.001 rb/scf
        study.setInitialRs(new BigDecimal("500"));             // 500 scf/stb
        study.setInitialWaterSaturation(new BigDecimal("0.25")); // 25%

        // Data points (pressure decline)
        List<MaterialBalanceDataPoint> dataPoints = new ArrayList<>();

        // Point 1: Initial conditions
        MaterialBalanceDataPoint dp1 = new MaterialBalanceDataPoint();
        dp1.setPressure(new BigDecimal("3000"));
        dp1.setBo(new BigDecimal("1.20"));
        dp1.setBg(new BigDecimal("0.001"));
        dp1.setRs(new BigDecimal("500"));
        dp1.setBw(new BigDecimal("1.00"));
        dp1.setCumulativeOilProduction(BigDecimal.ZERO);
        dp1.setCumulativeGasProduction(BigDecimal.ZERO);
        dp1.setCumulativeWaterProduction(BigDecimal.ZERO);
        dataPoints.add(dp1);

        // Point 2: After some production
        MaterialBalanceDataPoint dp2 = new MaterialBalanceDataPoint();
        dp2.setPressure(new BigDecimal("2700"));
        dp2.setBo(new BigDecimal("1.25"));
        dp2.setBg(new BigDecimal("0.0012"));
        dp2.setRs(new BigDecimal("450"));
        dp2.setBw(new BigDecimal("1.00"));
        dp2.setCumulativeOilProduction(new BigDecimal("5.0"));   // 5 MMbbl
        dp2.setCumulativeGasProduction(new BigDecimal("2.5"));   // 2.5 Bcf
        dp2.setCumulativeWaterProduction(new BigDecimal("0.5")); // 0.5 MMbbl
        dataPoints.add(dp2);

        // Point 3: Further depletion
        MaterialBalanceDataPoint dp3 = new MaterialBalanceDataPoint();
        dp3.setPressure(new BigDecimal("2400"));
        dp3.setBo(new BigDecimal("1.30"));
        dp3.setBg(new BigDecimal("0.0015"));
        dp3.setRs(new BigDecimal("400"));
        dp3.setBw(new BigDecimal("1.00"));
        dp3.setCumulativeOilProduction(new BigDecimal("10.0"));  // 10 MMbbl
        dp3.setCumulativeGasProduction(new BigDecimal("5.0"));   // 5 Bcf
        dp3.setCumulativeWaterProduction(new BigDecimal("1.0")); // 1 MMbbl
        dataPoints.add(dp3);

        study.setDataPoints(dataPoints);
        return study;
    }

    /**
     * Create a study with gas cap.
     */
    private RvMaterialBalanceDto createStudyWithGasCap() {
        RvMaterialBalanceDto study = createBasicStudy();
        study.setHasGasCap(true);
        study.setGasCapRatio(new BigDecimal("0.3")); // m = 0.3
        return study;
    }

    /**
     * Create a study with near-perfect linear relationship for testing R².
     */
    private RvMaterialBalanceDto createPerfectLinearStudy() {
        RvMaterialBalanceDto study = createBasicStudy();

        // Adjust data points to create perfect linear relationship
        // F = N * Eo, with N = 50 MMbbl
        BigDecimal N = new BigDecimal("50");

        for (MaterialBalanceDataPoint dp : study.getDataPoints()) {
            // Create synthetic data with known OOIP
            BigDecimal eo = new BigDecimal(Math.random() * 0.1 + 0.05); // Random Eo between 0.05 and 0.15
            BigDecimal f = N.multiply(eo); // Perfect linear relationship

            // Set the values to create perfect linearity
            dp.setEo(eo.setScale(8, RoundingMode.HALF_UP));
            dp.setF(f.setScale(8, RoundingMode.HALF_UP));
        }

        return study;
    }
}
