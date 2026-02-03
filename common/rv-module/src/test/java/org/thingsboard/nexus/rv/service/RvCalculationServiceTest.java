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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.nexus.rv.config.RvModuleConfiguration;
import org.thingsboard.nexus.rv.exception.RvBusinessException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RvCalculationService.
 * Tests petroleum engineering calculations including OOIP, Archie, IPR, and Decline Analysis.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RvCalculationService Unit Tests")
class RvCalculationServiceTest {

    @Mock
    private RvModuleConfiguration config;

    private RvCalculationService calculationService;

    @BeforeEach
    void setUp() {
        // Setup default configuration values
        when(config.getDefaultTortuosityFactor()).thenReturn(1.0);
        when(config.getDefaultCementationExponent()).thenReturn(2.0);
        when(config.getDefaultSaturationExponent()).thenReturn(2.0);

        calculationService = new RvCalculationService(config);
    }

    // ===========================================
    // OOIP CALCULATION TESTS
    // ===========================================

    @Test
    @DisplayName("OOIP: Cálculo básico con valores típicos")
    void testCalculateOOIP_TypicalValues() {
        // Given: Reservoir with typical properties
        BigDecimal areaAcres = new BigDecimal("1000");        // 1000 acres
        BigDecimal thicknessM = new BigDecimal("30");         // 30 meters (≈98 ft)
        BigDecimal porosity = new BigDecimal("0.20");         // 20%
        BigDecimal waterSat = new BigDecimal("0.25");         // 25%
        BigDecimal bo = new BigDecimal("1.25");               // 1.25 rb/stb

        // When
        BigDecimal ooip = calculationService.calculateOOIP(areaAcres, thicknessM, porosity, waterSat, bo);

        // Then: OOIP = 7758 * A * h(ft) * φ * (1-Sw) / Bo
        // = 7758 * 1000 * (30*3.28084) * 0.20 * (1-0.25) / 1.25
        // = 7758 * 1000 * 98.4252 * 0.20 * 0.75 / 1.25
        // ≈ 91,629,924 STB
        assertNotNull(ooip);
        assertTrue(ooip.compareTo(new BigDecimal("90000000")) > 0, "OOIP should be > 90M STB");
        assertTrue(ooip.compareTo(new BigDecimal("93000000")) < 0, "OOIP should be < 93M STB");
    }

    @Test
    @DisplayName("OOIP: Valores límite - Alta porosidad y baja Sw")
    void testCalculateOOIP_HighPorosityLowWaterSat() {
        // Given: Excellent reservoir quality
        BigDecimal areaAcres = new BigDecimal("500");
        BigDecimal thicknessM = new BigDecimal("20");
        BigDecimal porosity = new BigDecimal("0.30");         // 30% - excellent
        BigDecimal waterSat = new BigDecimal("0.15");         // 15% - low
        BigDecimal bo = new BigDecimal("1.20");

        // When
        BigDecimal ooip = calculationService.calculateOOIP(areaAcres, thicknessM, porosity, waterSat, bo);

        // Then
        assertNotNull(ooip);
        assertTrue(ooip.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("OOIP: Validación - Porosidad fuera de rango (>1)")
    void testCalculateOOIP_InvalidPorosity() {
        // Given: Invalid porosity > 1
        BigDecimal areaAcres = new BigDecimal("1000");
        BigDecimal thicknessM = new BigDecimal("30");
        BigDecimal porosity = new BigDecimal("1.5");          // INVALID
        BigDecimal waterSat = new BigDecimal("0.25");
        BigDecimal bo = new BigDecimal("1.25");

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            calculationService.calculateOOIP(areaAcres, thicknessM, porosity, waterSat, bo);
        });
    }

    @Test
    @DisplayName("OOIP: Validación - Saturación de agua negativa")
    void testCalculateOOIP_NegativeWaterSat() {
        // Given: Invalid negative water saturation
        BigDecimal areaAcres = new BigDecimal("1000");
        BigDecimal thicknessM = new BigDecimal("30");
        BigDecimal porosity = new BigDecimal("0.20");
        BigDecimal waterSat = new BigDecimal("-0.1");         // INVALID
        BigDecimal bo = new BigDecimal("1.25");

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            calculationService.calculateOOIP(areaAcres, thicknessM, porosity, waterSat, bo);
        });
    }

    @Test
    @DisplayName("OOIP: Validación - Bo negativo o cero")
    void testCalculateOOIP_InvalidBo() {
        // Given: Invalid Bo <= 0
        BigDecimal areaAcres = new BigDecimal("1000");
        BigDecimal thicknessM = new BigDecimal("30");
        BigDecimal porosity = new BigDecimal("0.20");
        BigDecimal waterSat = new BigDecimal("0.25");
        BigDecimal bo = new BigDecimal("0");                  // INVALID

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            calculationService.calculateOOIP(areaAcres, thicknessM, porosity, waterSat, bo);
        });
    }

    // ===========================================
    // ARCHIE EQUATION TESTS
    // ===========================================

    @Test
    @DisplayName("Archie: Cálculo de Sw con valores típicos de arenisca limpia")
    void testCalculateSwArchie_CleanSand() {
        // Given: Clean sand properties
        BigDecimal porosity = new BigDecimal("0.25");         // 25%
        BigDecimal rw = new BigDecimal("0.05");               // 0.05 ohm-m (saline water)
        BigDecimal rt = new BigDecimal("10.0");               // 10 ohm-m (high resistivity)
        BigDecimal a = new BigDecimal("1.0");
        BigDecimal m = new BigDecimal("2.0");
        BigDecimal n = new BigDecimal("2.0");

        // When
        BigDecimal sw = calculationService.calculateSwArchie(porosity, rw, rt, a, m, n);

        // Then: Sw = (1*0.05 / (0.25^2 * 10))^0.5 = (0.05/0.625)^0.5 ≈ 0.283
        assertNotNull(sw);
        assertTrue(sw.compareTo(new BigDecimal("0.20")) > 0);
        assertTrue(sw.compareTo(new BigDecimal("0.35")) < 0);
    }

    @Test
    @DisplayName("Archie: Alta resistividad implica baja Sw (zona de petróleo)")
    void testCalculateSwArchie_OilZone() {
        // Given: High resistivity (oil zone)
        BigDecimal porosity = new BigDecimal("0.20");
        BigDecimal rw = new BigDecimal("0.08");
        BigDecimal rt = new BigDecimal("50.0");               // Very high Rt
        BigDecimal a = new BigDecimal("1.0");
        BigDecimal m = new BigDecimal("2.0");
        BigDecimal n = new BigDecimal("2.0");

        // When
        BigDecimal sw = calculationService.calculateSwArchie(porosity, rw, rt, a, m, n);

        // Then: Low Sw expected
        assertNotNull(sw);
        assertTrue(sw.compareTo(new BigDecimal("0.30")) < 0);  // Sw < 30%
    }

    @Test
    @DisplayName("Archie: Baja resistividad implica alta Sw (zona de agua)")
    void testCalculateSwArchie_WaterZone() {
        // Given: Low resistivity (water zone)
        BigDecimal porosity = new BigDecimal("0.22");
        BigDecimal rw = new BigDecimal("0.05");
        BigDecimal rt = new BigDecimal("1.0");                // Low Rt
        BigDecimal a = new BigDecimal("1.0");
        BigDecimal m = new BigDecimal("2.0");
        BigDecimal n = new BigDecimal("2.0");

        // When
        BigDecimal sw = calculationService.calculateSwArchie(porosity, rw, rt, a, m, n);

        // Then: High Sw expected
        assertNotNull(sw);
        assertTrue(sw.compareTo(new BigDecimal("0.40")) > 0);  // Sw > 40%
    }

    @Test
    @DisplayName("Archie: Uso de valores por defecto (a, m, n)")
    void testCalculateSwArchie_DefaultParameters() {
        // Given: Parameters a, m, n are null (should use defaults)
        BigDecimal porosity = new BigDecimal("0.20");
        BigDecimal rw = new BigDecimal("0.05");
        BigDecimal rt = new BigDecimal("10.0");

        // When
        BigDecimal sw = calculationService.calculateSwArchie(porosity, rw, rt, null, null, null);

        // Then: Should use defaults from config (1.0, 2.0, 2.0)
        assertNotNull(sw);
        assertTrue(sw.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(sw.compareTo(BigDecimal.ONE) <= 0);
    }

    @Test
    @DisplayName("Archie: Validación - Porosidad fuera de rango")
    void testCalculateSwArchie_InvalidPorosity() {
        // Given: Invalid porosity
        BigDecimal porosity = new BigDecimal("1.5");          // INVALID
        BigDecimal rw = new BigDecimal("0.05");
        BigDecimal rt = new BigDecimal("10.0");

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            calculationService.calculateSwArchie(porosity, rw, rt, null, null, null);
        });
    }

    // ===========================================
    // VSHALE (LARIONOV) TESTS
    // ===========================================

    @Test
    @DisplayName("Vsh Larionov: Arena limpia (GR bajo)")
    void testCalculateVshLarionov_CleanSand() {
        // Given: Clean sand (GR close to GRclean)
        BigDecimal grLog = new BigDecimal("25");
        BigDecimal grClean = new BigDecimal("20");
        BigDecimal grShale = new BigDecimal("100");

        // When
        BigDecimal vsh = calculationService.calculateVshLarionov(grLog, grClean, grShale);

        // Then: Low Vsh expected
        assertNotNull(vsh);
        assertTrue(vsh.compareTo(new BigDecimal("0.15")) < 0);  // Vsh < 15%
    }

    @Test
    @DisplayName("Vsh Larionov: Arcilla pura (GR alto)")
    void testCalculateVshLarionov_Shale() {
        // Given: Shale (GR close to GRshale)
        BigDecimal grLog = new BigDecimal("95");
        BigDecimal grClean = new BigDecimal("20");
        BigDecimal grShale = new BigDecimal("100");

        // When
        BigDecimal vsh = calculationService.calculateVshLarionov(grLog, grClean, grShale);

        // Then: High Vsh expected
        assertNotNull(vsh);
        assertTrue(vsh.compareTo(new BigDecimal("0.80")) > 0);  // Vsh > 80%
    }

    // ===========================================
    // PVT CORRELATIONS TESTS
    // ===========================================

    @Test
    @DisplayName("Standing Pb: Cálculo de presión de burbuja")
    void testCalculatePbStanding() {
        // Given: Typical black oil properties
        BigDecimal rs = new BigDecimal("500");                // 500 scf/stb
        BigDecimal gasGravity = new BigDecimal("0.75");
        BigDecimal temperature = new BigDecimal("200");       // 200°F
        BigDecimal apiGravity = new BigDecimal("35");

        // When
        BigDecimal pb = calculationService.calculatePbStanding(rs, gasGravity, temperature, apiGravity);

        // Then: Typical Pb for black oil
        assertNotNull(pb);
        assertTrue(pb.compareTo(new BigDecimal("1000")) > 0); // > 1000 psia
        assertTrue(pb.compareTo(new BigDecimal("5000")) < 0); // < 5000 psia
    }

    @Test
    @DisplayName("Standing Bo: Cálculo de factor volumétrico")
    void testCalculateBoStanding() {
        // Given: Typical properties
        BigDecimal rs = new BigDecimal("500");
        BigDecimal gasGravity = new BigDecimal("0.75");
        BigDecimal oilGravity = new BigDecimal("0.85");       // From API
        BigDecimal temperature = new BigDecimal("200");

        // When
        BigDecimal bo = calculationService.calculateBoStanding(rs, gasGravity, oilGravity, temperature);

        // Then: Bo typically between 1.0 and 2.0 rb/stb
        assertNotNull(bo);
        assertTrue(bo.compareTo(new BigDecimal("1.0")) >= 0);
        assertTrue(bo.compareTo(new BigDecimal("2.0")) <= 0);
    }

    @Test
    @DisplayName("Beggs-Robinson: Cálculo de viscosidad de petróleo muerto")
    void testCalculateViscosityBeggsRobinson() {
        // Given: Light oil at reservoir temperature
        BigDecimal apiGravity = new BigDecimal("35");         // Light oil
        BigDecimal temperature = new BigDecimal("180");       // 180°F

        // When
        BigDecimal viscosity = calculationService.calculateViscosityBeggsRobinson(apiGravity, temperature);

        // Then: Light oil has low viscosity
        assertNotNull(viscosity);
        assertTrue(viscosity.compareTo(new BigDecimal("0.1")) > 0);   // > 0.1 cp
        assertTrue(viscosity.compareTo(new BigDecimal("10")) < 0);    // < 10 cp
    }

    // ===========================================
    // VOGEL IPR TESTS
    // ===========================================

    @Test
    @DisplayName("Vogel IPR: Cálculo de rate con Pwf = 0 (AOF)")
    void testCalculateIprVogel_AOF() {
        // Given: Pwf = 0 (Absolute Open Flow)
        BigDecimal qmax = new BigDecimal("1000");             // 1000 bpd
        BigDecimal reservoirP = new BigDecimal("2500");       // 2500 psi
        BigDecimal flowingP = BigDecimal.ZERO;

        // When
        BigDecimal rate = calculationService.calculateIprVogel(qmax, reservoirP, flowingP);

        // Then: q = qmax when Pwf = 0
        assertNotNull(rate);
        assertEquals(0, rate.compareTo(qmax));
    }

    @Test
    @DisplayName("Vogel IPR: Cálculo de rate con Pwf = Pr (shut-in)")
    void testCalculateIprVogel_ShutIn() {
        // Given: Pwf = Pr (well shut-in)
        BigDecimal qmax = new BigDecimal("1000");
        BigDecimal reservoirP = new BigDecimal("2500");
        BigDecimal flowingP = new BigDecimal("2500");         // Same as Pr

        // When
        BigDecimal rate = calculationService.calculateIprVogel(qmax, reservoirP, flowingP);

        // Then: q = 0 when Pwf = Pr
        assertNotNull(rate);
        assertTrue(rate.compareTo(new BigDecimal("10")) < 0);  // Close to zero
    }

    @Test
    @DisplayName("Vogel IPR: Cálculo con Pwf intermedio")
    void testCalculateIprVogel_TypicalFlowing() {
        // Given: Typical flowing conditions
        BigDecimal qmax = new BigDecimal("1000");
        BigDecimal reservoirP = new BigDecimal("2500");
        BigDecimal flowingP = new BigDecimal("1500");         // 60% of Pr

        // When
        BigDecimal rate = calculationService.calculateIprVogel(qmax, reservoirP, flowingP);

        // Then: Rate should be between 0 and qmax
        assertNotNull(rate);
        assertTrue(rate.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(rate.compareTo(qmax) < 0);
        // With Pwf/Pr = 0.6, q ≈ 1000 * (1 - 0.2*0.6 - 0.8*0.36) ≈ 592 bpd
        assertTrue(rate.compareTo(new BigDecimal("500")) > 0);
        assertTrue(rate.compareTo(new BigDecimal("700")) < 0);
    }

    @Test
    @DisplayName("Vogel IPR: Validación - Pwf > Pr")
    void testCalculateIprVogel_InvalidPressures() {
        // Given: Flowing pressure > reservoir pressure (INVALID)
        BigDecimal qmax = new BigDecimal("1000");
        BigDecimal reservoirP = new BigDecimal("2500");
        BigDecimal flowingP = new BigDecimal("3000");         // INVALID

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            calculationService.calculateIprVogel(qmax, reservoirP, flowingP);
        });
    }

    @Test
    @DisplayName("PI: Cálculo de índice de productividad")
    void testCalculateProductivityIndex() {
        // Given: Well test data
        BigDecimal testRate = new BigDecimal("500");          // 500 bpd
        BigDecimal reservoirP = new BigDecimal("2500");       // 2500 psi
        BigDecimal flowingP = new BigDecimal("2000");         // 2000 psi

        // When
        BigDecimal pi = calculationService.calculateProductivityIndex(testRate, reservoirP, flowingP);

        // Then: PI = 500 / (2500 - 2000) = 1.0 bpd/psi
        assertNotNull(pi);
        assertEquals(0, pi.compareTo(BigDecimal.ONE));
    }

    @Test
    @DisplayName("PI: Validación - Drawdown negativo o cero")
    void testCalculateProductivityIndex_InvalidDrawdown() {
        // Given: Pwf >= Pr (no drawdown)
        BigDecimal testRate = new BigDecimal("500");
        BigDecimal reservoirP = new BigDecimal("2500");
        BigDecimal flowingP = new BigDecimal("2500");         // Same as Pr

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            calculationService.calculateProductivityIndex(testRate, reservoirP, flowingP);
        });
    }

    // ===========================================
    // ARPS DECLINE TESTS
    // ===========================================

    @Test
    @DisplayName("Arps: Declinación exponencial (b=0)")
    void testCalculateArpsDecline_Exponential() {
        // Given: Exponential decline
        BigDecimal qi = new BigDecimal("1000");               // 1000 bpd initial
        BigDecimal di = new BigDecimal("0.05");               // 5% per year (converted to 1/day)
        BigDecimal b = BigDecimal.ZERO;                       // Exponential
        BigDecimal time = new BigDecimal("365");              // 1 year

        // When
        BigDecimal qt = calculationService.calculateArpsDecline(qi, di, b, time);

        // Then: q(t) = qi * e^(-Di*t) = 1000 * e^(-0.05*365) ≈ 0 (very small)
        // With Di = 0.05/day, after 365 days: e^(-18.25) ≈ 0
        assertNotNull(qt);
        assertTrue(qt.compareTo(qi) < 0);
    }

    @Test
    @DisplayName("Arps: Declinación hiperbólica (0 < b < 1)")
    void testCalculateArpsDecline_Hyperbolic() {
        // Given: Hyperbolic decline
        BigDecimal qi = new BigDecimal("1000");
        BigDecimal di = new BigDecimal("0.0001");             // 0.01% per day
        BigDecimal b = new BigDecimal("0.5");                 // Hyperbolic
        BigDecimal time = new BigDecimal("365");

        // When
        BigDecimal qt = calculationService.calculateArpsDecline(qi, di, b, time);

        // Then: Should decline but not to zero
        assertNotNull(qt);
        assertTrue(qt.compareTo(qi) < 0);
        assertTrue(qt.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Arps: Declinación armónica (b=1)")
    void testCalculateArpsDecline_Harmonic() {
        // Given: Harmonic decline
        BigDecimal qi = new BigDecimal("1000");
        BigDecimal di = new BigDecimal("0.0005");             // 0.05% per day
        BigDecimal b = BigDecimal.ONE;                        // Harmonic
        BigDecimal time = new BigDecimal("365");

        // When
        BigDecimal qt = calculationService.calculateArpsDecline(qi, di, b, time);

        // Then: q(t) = qi / (1 + Di*t) = 1000 / (1 + 0.0005*365) ≈ 845 bpd
        assertNotNull(qt);
        assertTrue(qt.compareTo(new BigDecimal("800")) > 0);
        assertTrue(qt.compareTo(new BigDecimal("900")) < 0);
    }

    @Test
    @DisplayName("Arps: Tiempo cero devuelve qi")
    void testCalculateArpsDecline_TimeZero() {
        // Given: Time = 0
        BigDecimal qi = new BigDecimal("1000");
        BigDecimal di = new BigDecimal("0.05");
        BigDecimal b = new BigDecimal("0.5");
        BigDecimal time = BigDecimal.ZERO;

        // When
        BigDecimal qt = calculationService.calculateArpsDecline(qi, di, b, time);

        // Then: q(0) = qi
        assertNotNull(qt);
        assertEquals(0, qt.compareTo(qi));
    }

    @Test
    @DisplayName("Arps Cumulative: Producción acumulada exponencial")
    void testCalculateArpsCumulative_Exponential() {
        // Given: Exponential decline
        BigDecimal qi = new BigDecimal("1000");
        BigDecimal di = new BigDecimal("0.001");              // 0.1% per day
        BigDecimal b = BigDecimal.ZERO;
        BigDecimal time = new BigDecimal("365");

        // When
        BigDecimal np = calculationService.calculateArpsCumulative(qi, di, b, time);

        // Then: Np = (qi/Di) * (1 - e^(-Di*t))
        assertNotNull(np);
        assertTrue(np.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Arps Cumulative: Producción acumulada armónica")
    void testCalculateArpsCumulative_Harmonic() {
        // Given: Harmonic decline
        BigDecimal qi = new BigDecimal("1000");
        BigDecimal di = new BigDecimal("0.001");
        BigDecimal b = BigDecimal.ONE;
        BigDecimal time = new BigDecimal("365");

        // When
        BigDecimal np = calculationService.calculateArpsCumulative(qi, di, b, time);

        // Then: Np = (qi/Di) * ln(1 + Di*t)
        assertNotNull(np);
        assertTrue(np.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Arps Cumulative: Producción acumulada hiperbólica")
    void testCalculateArpsCumulative_Hyperbolic() {
        // Given: Hyperbolic decline
        BigDecimal qi = new BigDecimal("1000");
        BigDecimal di = new BigDecimal("0.001");
        BigDecimal b = new BigDecimal("0.5");
        BigDecimal time = new BigDecimal("365");

        // When
        BigDecimal np = calculationService.calculateArpsCumulative(qi, di, b, time);

        // Then
        assertNotNull(np);
        assertTrue(np.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Arps: Validación - Di negativo")
    void testCalculateArpsDecline_NegativeDecline() {
        // Given: Negative decline rate (INVALID)
        BigDecimal qi = new BigDecimal("1000");
        BigDecimal di = new BigDecimal("-0.05");              // INVALID
        BigDecimal b = BigDecimal.ZERO;
        BigDecimal time = new BigDecimal("365");

        // When/Then
        assertThrows(RvBusinessException.class, () -> {
            calculationService.calculateArpsDecline(qi, di, b, time);
        });
    }
}
