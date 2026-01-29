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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.rv.config.RvModuleConfiguration;
import org.thingsboard.nexus.rv.exception.RvBusinessException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Service for petroleum engineering calculations.
 * Implements formulas for OOIP, Sw, Vsh, PVT correlations, IPR, and decline analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvCalculationService {

    private final RvModuleConfiguration config;
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    // ===========================================
    // VOLUMETRICS - OOIP Calculation
    // ===========================================

    /**
     * Calculate Original Oil In Place (OOIP) using volumetric method.
     * Formula: OOIP = 7758 * A * h * φ * (1 - Sw) / Bo
     *
     * @param areaAcres     Area in acres
     * @param thicknessM    Net pay thickness in meters (converted to feet)
     * @param porosity      Porosity (fraction 0-1)
     * @param waterSat      Water saturation (fraction 0-1)
     * @param boRbStb       Formation volume factor (rb/stb)
     * @return OOIP in STB
     */
    public BigDecimal calculateOOIP(BigDecimal areaAcres, BigDecimal thicknessM,
                                    BigDecimal porosity, BigDecimal waterSat, BigDecimal boRbStb) {
        log.debug("Calculating OOIP: A={} acres, h={} m, φ={}, Sw={}, Bo={}",
                areaAcres, thicknessM, porosity, waterSat, boRbStb);

        validateRange("Porosity", porosity, BigDecimal.ZERO, BigDecimal.ONE);
        validateRange("Water Saturation", waterSat, BigDecimal.ZERO, BigDecimal.ONE);
        validatePositive("Bo", boRbStb);

        // Convert meters to feet
        BigDecimal thicknessFt = thicknessM.multiply(new BigDecimal("3.28084"), MC);

        // OOIP = 7758 * A * h * φ * (1 - Sw) / Bo
        BigDecimal constant = new BigDecimal("7758");
        BigDecimal oilSat = BigDecimal.ONE.subtract(waterSat);

        BigDecimal ooip = constant
                .multiply(areaAcres, MC)
                .multiply(thicknessFt, MC)
                .multiply(porosity, MC)
                .multiply(oilSat, MC)
                .divide(boRbStb, MC);

        log.info("OOIP calculated: {} STB", ooip);
        return ooip;
    }

    // ===========================================
    // PETROPHYSICS - Archie Equation
    // ===========================================

    /**
     * Calculate Water Saturation using Archie equation.
     * Formula: Sw = (a * Rw / (φ^m * Rt))^(1/n)
     *
     * @param porosity  Porosity (fraction)
     * @param rw        Formation water resistivity (ohm-m)
     * @param rt        True formation resistivity (ohm-m)
     * @param a         Tortuosity factor (typically 1.0)
     * @param m         Cementation exponent (typically 2.0)
     * @param n         Saturation exponent (typically 2.0)
     * @return Water saturation (fraction 0-1)
     */
    public BigDecimal calculateSwArchie(BigDecimal porosity, BigDecimal rw, BigDecimal rt,
                                        BigDecimal a, BigDecimal m, BigDecimal n) {
        log.debug("Calculating Sw (Archie): φ={}, Rw={}, Rt={}, a={}, m={}, n={}",
                porosity, rw, rt, a, m, n);

        validateRange("Porosity", porosity, BigDecimal.ZERO, BigDecimal.ONE);
        validatePositive("Rw", rw);
        validatePositive("Rt", rt);

        // Use defaults if not provided
        if (a == null) a = BigDecimal.valueOf(config.getDefaultTortuosityFactor());
        if (m == null) m = BigDecimal.valueOf(config.getDefaultCementationExponent());
        if (n == null) n = BigDecimal.valueOf(config.getDefaultSaturationExponent());

        // φ^m
        double phiPowM = Math.pow(porosity.doubleValue(), m.doubleValue());

        // a * Rw / (φ^m * Rt)
        double ratio = a.doubleValue() * rw.doubleValue() / (phiPowM * rt.doubleValue());

        // Sw = ratio^(1/n)
        double sw = Math.pow(ratio, 1.0 / n.doubleValue());

        // Clamp to [0, 1]
        sw = Math.max(0.0, Math.min(1.0, sw));

        BigDecimal result = BigDecimal.valueOf(sw).setScale(4, RoundingMode.HALF_UP);
        log.info("Sw calculated: {}", result);
        return result;
    }

    /**
     * Calculate Shale Volume using Larionov equation (Tertiary rocks).
     * Formula: Vsh = 0.083 * (2^(3.7 * IGR) - 1)
     *
     * @param grLog     Gamma ray log reading
     * @param grClean   Gamma ray in clean sand (minimum)
     * @param grShale   Gamma ray in shale (maximum)
     * @return Shale volume (fraction 0-1)
     */
    public BigDecimal calculateVshLarionov(BigDecimal grLog, BigDecimal grClean, BigDecimal grShale) {
        log.debug("Calculating Vsh (Larionov): GR={}, GRclean={}, GRshale={}",
                grLog, grClean, grShale);

        // IGR = (GRlog - GRclean) / (GRshale - GRclean)
        BigDecimal igr = grLog.subtract(grClean)
                .divide(grShale.subtract(grClean), MC);

        // Clamp IGR to [0, 1]
        double igrVal = Math.max(0.0, Math.min(1.0, igr.doubleValue()));

        // Vsh = 0.083 * (2^(3.7*IGR) - 1)
        double vsh = 0.083 * (Math.pow(2, 3.7 * igrVal) - 1);

        // Clamp to [0, 1]
        vsh = Math.max(0.0, Math.min(1.0, vsh));

        BigDecimal result = BigDecimal.valueOf(vsh).setScale(4, RoundingMode.HALF_UP);
        log.info("Vsh calculated: {}", result);
        return result;
    }

    // ===========================================
    // PVT CORRELATIONS
    // ===========================================

    /**
     * Calculate Bubble Point Pressure using Standing correlation.
     * Formula: Pb = 18.2 * ((Rs/γg)^0.83 * 10^(0.00091*T - 0.0125*API) - 1.4)
     *
     * @param rs            Solution GOR (scf/stb)
     * @param gasGravity    Gas specific gravity (air = 1)
     * @param temperature   Temperature (°F)
     * @param apiGravity    API gravity
     * @return Bubble point pressure (psia)
     */
    public BigDecimal calculatePbStanding(BigDecimal rs, BigDecimal gasGravity,
                                          BigDecimal temperature, BigDecimal apiGravity) {
        log.debug("Calculating Pb (Standing): Rs={}, γg={}, T={}, API={}",
                rs, gasGravity, temperature, apiGravity);

        double rsVal = rs.doubleValue();
        double ggVal = gasGravity.doubleValue();
        double tVal = temperature.doubleValue();
        double apiVal = apiGravity.doubleValue();

        // (Rs/γg)^0.83
        double term1 = Math.pow(rsVal / ggVal, 0.83);

        // 10^(0.00091*T - 0.0125*API)
        double exponent = 0.00091 * tVal - 0.0125 * apiVal;
        double term2 = Math.pow(10, exponent);

        // Pb = 18.2 * (term1 * term2 - 1.4)
        double pb = 18.2 * (term1 * term2 - 1.4);

        BigDecimal result = BigDecimal.valueOf(pb).setScale(2, RoundingMode.HALF_UP);
        log.info("Pb calculated: {} psia", result);
        return result;
    }

    /**
     * Calculate Oil Formation Volume Factor using Standing correlation.
     * Formula: Bo = 0.9759 + 0.00012 * (Rs * (γg/γo)^0.5 + 1.25*T)^1.2
     *
     * @param rs            Solution GOR (scf/stb)
     * @param gasGravity    Gas specific gravity
     * @param oilGravity    Oil specific gravity (API converted)
     * @param temperature   Temperature (°F)
     * @return Formation volume factor (rb/stb)
     */
    public BigDecimal calculateBoStanding(BigDecimal rs, BigDecimal gasGravity,
                                          BigDecimal oilGravity, BigDecimal temperature) {
        log.debug("Calculating Bo (Standing): Rs={}, γg={}, γo={}, T={}",
                rs, gasGravity, oilGravity, temperature);

        double rsVal = rs.doubleValue();
        double ggVal = gasGravity.doubleValue();
        double goVal = oilGravity.doubleValue();
        double tVal = temperature.doubleValue();

        // (γg/γo)^0.5
        double gravityRatio = Math.sqrt(ggVal / goVal);

        // Rs * gravityRatio + 1.25*T
        double term = rsVal * gravityRatio + 1.25 * tVal;

        // Bo = 0.9759 + 0.00012 * term^1.2
        double bo = 0.9759 + 0.00012 * Math.pow(term, 1.2);

        BigDecimal result = BigDecimal.valueOf(bo).setScale(4, RoundingMode.HALF_UP);
        log.info("Bo calculated: {} rb/stb", result);
        return result;
    }

    /**
     * Calculate Dead Oil Viscosity using Beggs-Robinson correlation.
     * Formula: μod = 10^(10^(3.0324 - 0.02023*API) * T^-1.163) - 1
     *
     * @param apiGravity    API gravity
     * @param temperature   Temperature (°F)
     * @return Dead oil viscosity (cp)
     */
    public BigDecimal calculateViscosityBeggsRobinson(BigDecimal apiGravity, BigDecimal temperature) {
        log.debug("Calculating μod (Beggs-Robinson): API={}, T={}", apiGravity, temperature);

        double apiVal = apiGravity.doubleValue();
        double tVal = temperature.doubleValue();

        // x = T^-1.163
        double x = Math.pow(tVal, -1.163);

        // y = 10^(3.0324 - 0.02023*API)
        double y = Math.pow(10, 3.0324 - 0.02023 * apiVal);

        // μod = 10^(y*x) - 1
        double muOd = Math.pow(10, y * x) - 1;

        BigDecimal result = BigDecimal.valueOf(muOd).setScale(4, RoundingMode.HALF_UP);
        log.info("Dead oil viscosity calculated: {} cp", result);
        return result;
    }

    // ===========================================
    // IPR - Inflow Performance
    // ===========================================

    /**
     * Calculate flow rate using Vogel equation (saturated reservoirs).
     * Formula: q = qmax * (1 - 0.2*(Pwf/Pr) - 0.8*(Pwf/Pr)^2)
     *
     * @param qmax          Maximum flow rate at Pwf=0 (bpd)
     * @param reservoirP    Reservoir pressure (psi)
     * @param flowingP      Flowing bottomhole pressure (psi)
     * @return Oil rate (bpd)
     */
    public BigDecimal calculateIprVogel(BigDecimal qmax, BigDecimal reservoirP, BigDecimal flowingP) {
        log.debug("Calculating IPR (Vogel): qmax={}, Pr={}, Pwf={}",
                qmax, reservoirP, flowingP);

        validatePositive("qmax", qmax);
        validatePositive("Reservoir Pressure", reservoirP);

        if (flowingP.compareTo(reservoirP) > 0) {
            throw new RvBusinessException(RvBusinessException.PRESSURE_OUT_OF_RANGE,
                    "Flowing pressure cannot exceed reservoir pressure");
        }

        double qmaxVal = qmax.doubleValue();
        double prVal = reservoirP.doubleValue();
        double pwfVal = flowingP.doubleValue();

        double ratio = pwfVal / prVal;
        double q = qmaxVal * (1 - 0.2 * ratio - 0.8 * ratio * ratio);

        BigDecimal result = BigDecimal.valueOf(q).setScale(2, RoundingMode.HALF_UP);
        log.info("IPR flow rate calculated: {} bpd", result);
        return result;
    }

    /**
     * Calculate Productivity Index (J) from test data.
     * Formula: J = q / (Pr - Pwf)
     *
     * @param testRate      Test flow rate (bpd)
     * @param reservoirP    Reservoir pressure (psi)
     * @param flowingP      Flowing bottomhole pressure during test (psi)
     * @return Productivity index (bpd/psi)
     */
    public BigDecimal calculateProductivityIndex(BigDecimal testRate, BigDecimal reservoirP, BigDecimal flowingP) {
        log.debug("Calculating PI: q={}, Pr={}, Pwf={}", testRate, reservoirP, flowingP);

        BigDecimal drawdown = reservoirP.subtract(flowingP);
        if (drawdown.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RvBusinessException(RvBusinessException.PRESSURE_OUT_OF_RANGE,
                    "Drawdown must be positive");
        }

        BigDecimal pi = testRate.divide(drawdown, MC);

        log.info("Productivity Index calculated: {} bpd/psi", pi);
        return pi.setScale(4, RoundingMode.HALF_UP);
    }

    // ===========================================
    // DECLINE ANALYSIS - Arps
    // ===========================================

    /**
     * Calculate production rate at time t using Arps decline.
     *
     * @param qi            Initial rate (bpd)
     * @param di            Initial decline rate (1/day or 1/month)
     * @param b             Decline exponent (0=exponential, 0<b<1=hyperbolic, 1=harmonic)
     * @param time          Time (days or months, matching di units)
     * @return Rate at time t (bpd)
     */
    public BigDecimal calculateArpsDecline(BigDecimal qi, BigDecimal di, BigDecimal b, BigDecimal time) {
        log.debug("Calculating Arps decline: qi={}, Di={}, b={}, t={}", qi, di, b, time);

        validatePositive("Initial rate", qi);
        validatePositive("Decline rate", di);

        double qiVal = qi.doubleValue();
        double diVal = di.doubleValue();
        double bVal = b.doubleValue();
        double tVal = time.doubleValue();

        double qt;

        if (bVal == 0) {
            // Exponential: q(t) = qi * e^(-Di*t)
            qt = qiVal * Math.exp(-diVal * tVal);
        } else if (bVal == 1) {
            // Harmonic: q(t) = qi / (1 + Di*t)
            qt = qiVal / (1 + diVal * tVal);
        } else {
            // Hyperbolic: q(t) = qi / (1 + b*Di*t)^(1/b)
            qt = qiVal / Math.pow(1 + bVal * diVal * tVal, 1 / bVal);
        }

        BigDecimal result = BigDecimal.valueOf(qt).setScale(2, RoundingMode.HALF_UP);
        log.info("Arps decline rate at t={}: {} bpd", time, result);
        return result;
    }

    /**
     * Calculate cumulative production using Arps decline.
     *
     * @param qi            Initial rate (bpd)
     * @param di            Initial decline rate (1/day)
     * @param b             Decline exponent
     * @param time          Time (days)
     * @return Cumulative production (bbl)
     */
    public BigDecimal calculateArpsCumulative(BigDecimal qi, BigDecimal di, BigDecimal b, BigDecimal time) {
        log.debug("Calculating Arps cumulative: qi={}, Di={}, b={}, t={}", qi, di, b, time);

        double qiVal = qi.doubleValue();
        double diVal = di.doubleValue();
        double bVal = b.doubleValue();
        double tVal = time.doubleValue();

        double np;

        if (bVal == 0) {
            // Exponential: Np = (qi/Di) * (1 - e^(-Di*t))
            np = (qiVal / diVal) * (1 - Math.exp(-diVal * tVal));
        } else if (bVal == 1) {
            // Harmonic: Np = (qi/Di) * ln(1 + Di*t)
            np = (qiVal / diVal) * Math.log(1 + diVal * tVal);
        } else {
            // Hyperbolic: Np = qi / ((1-b)*Di) * (1 - (1+b*Di*t)^(1-1/b))
            double qt = qiVal / Math.pow(1 + bVal * diVal * tVal, 1 / bVal);
            np = (qiVal / ((1 - bVal) * diVal)) * (1 - Math.pow(qt / qiVal, 1 - bVal));
        }

        BigDecimal result = BigDecimal.valueOf(np).setScale(0, RoundingMode.HALF_UP);
        log.info("Arps cumulative at t={}: {} bbl", time, result);
        return result;
    }

    // ===========================================
    // VALIDATION HELPERS
    // ===========================================

    private void validateRange(String name, BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new RvBusinessException(RvBusinessException.CALCULATION_ERROR,
                    String.format("%s must be between %s and %s, got %s", name, min, max, value));
        }
    }

    private void validatePositive(String name, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RvBusinessException(RvBusinessException.CALCULATION_ERROR,
                    String.format("%s must be positive, got %s", name, value));
        }
    }
}
