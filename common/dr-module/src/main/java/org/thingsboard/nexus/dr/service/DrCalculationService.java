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
package org.thingsboard.nexus.dr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.dr.dto.calculation.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Service for drilling engineering calculations.
 * Implements industry-standard formulas for MSE, ECD, Swab/Surge, Kick Tolerance, T&D.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrCalculationService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal PI = BigDecimal.valueOf(Math.PI);

    // ==================== MSE (Mechanical Specific Energy) ====================

    /**
     * Calculate Mechanical Specific Energy (MSE).
     * MSE = (480 × T × N) / (D² × ROP) + (4 × WOB) / (π × D²)
     *
     * Where:
     * - T = Torque (ft-lbs)
     * - N = RPM
     * - D = Bit diameter (inches)
     * - ROP = Rate of Penetration (ft/hr)
     * - WOB = Weight on Bit (lbs)
     *
     * @return MSE in psi
     */
    public MseCalculationResult calculateMse(MseCalculationRequest request) {
        log.debug("Calculating MSE for bit diameter {} in", request.getBitDiameterIn());

        BigDecimal torque = request.getTorqueFtLbs();
        BigDecimal rpm = request.getRpm();
        BigDecimal bitDiameter = request.getBitDiameterIn();
        BigDecimal rop = request.getRopFtHr();
        BigDecimal wob = request.getWobLbs();

        // Validate inputs
        if (rop.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ROP must be greater than zero");
        }
        if (bitDiameter.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bit diameter must be greater than zero");
        }

        // D²
        BigDecimal diameterSquared = bitDiameter.multiply(bitDiameter, MC);

        // Rotational component: (480 × T × N) / (D² × ROP)
        BigDecimal rotationalComponent = BigDecimal.valueOf(480)
                .multiply(torque, MC)
                .multiply(rpm, MC)
                .divide(diameterSquared.multiply(rop, MC), MC);

        // Axial component: (4 × WOB) / (π × D²)
        BigDecimal axialComponent = BigDecimal.valueOf(4)
                .multiply(wob, MC)
                .divide(PI.multiply(diameterSquared, MC), MC);

        BigDecimal mse = rotationalComponent.add(axialComponent, MC);

        // Calculate drilling efficiency if rock strength is provided
        BigDecimal drillingEfficiency = null;
        Boolean isFoundering = null;
        if (request.getEstimatedRockStrengthPsi() != null &&
                request.getEstimatedRockStrengthPsi().compareTo(BigDecimal.ZERO) > 0) {
            drillingEfficiency = request.getEstimatedRockStrengthPsi()
                    .divide(mse, MC)
                    .multiply(BigDecimal.valueOf(100), MC);

            // Foundering detection: MSE > 3× rock strength
            isFoundering = mse.compareTo(request.getEstimatedRockStrengthPsi().multiply(BigDecimal.valueOf(3))) > 0;
        }

        return MseCalculationResult.builder()
                .msePsi(mse.setScale(2, RoundingMode.HALF_UP))
                .rotationalComponentPsi(rotationalComponent.setScale(2, RoundingMode.HALF_UP))
                .axialComponentPsi(axialComponent.setScale(2, RoundingMode.HALF_UP))
                .drillingEfficiencyPercent(drillingEfficiency != null ?
                        drillingEfficiency.setScale(2, RoundingMode.HALF_UP) : null)
                .isFoundering(isFoundering)
                .build();
    }

    // ==================== ECD (Equivalent Circulating Density) ====================

    /**
     * Calculate Equivalent Circulating Density (ECD).
     * ECD = MW + APL / (0.052 × TVD)
     *
     * Where:
     * - MW = Mud Weight (ppg)
     * - APL = Annular Pressure Loss (psi)
     * - TVD = True Vertical Depth (ft)
     *
     * @return ECD in ppg
     */
    public EcdCalculationResult calculateEcd(EcdCalculationRequest request) {
        log.debug("Calculating ECD at TVD {} ft", request.getTvdFt());

        BigDecimal mudWeight = request.getMudWeightPpg();
        BigDecimal tvd = request.getTvdFt();

        if (tvd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("TVD must be greater than zero");
        }

        // Calculate annular pressure loss if flow parameters provided
        BigDecimal annularPressureLoss = request.getAnnularPressureLossPsi();
        if (annularPressureLoss == null && request.getFlowRateGpm() != null) {
            annularPressureLoss = calculateAnnularPressureLoss(request);
        }

        if (annularPressureLoss == null) {
            annularPressureLoss = BigDecimal.ZERO;
        }

        // ECD = MW + APL / (0.052 × TVD)
        BigDecimal ecdIncrement = annularPressureLoss
                .divide(BigDecimal.valueOf(0.052).multiply(tvd, MC), MC);

        BigDecimal ecd = mudWeight.add(ecdIncrement, MC);

        // Calculate hydrostatic pressure
        BigDecimal hydrostaticPressure = mudWeight
                .multiply(BigDecimal.valueOf(0.052), MC)
                .multiply(tvd, MC);

        // Calculate bottom hole pressure
        BigDecimal bottomHolePressure = hydrostaticPressure.add(annularPressureLoss, MC);

        // Check against formation pressures if provided
        Boolean isAboveFracGradient = null;
        Boolean isBelowPorePressure = null;
        BigDecimal marginToFrac = null;
        BigDecimal marginToPore = null;

        if (request.getFracGradientPpg() != null) {
            marginToFrac = request.getFracGradientPpg().subtract(ecd, MC);
            isAboveFracGradient = ecd.compareTo(request.getFracGradientPpg()) > 0;
        }

        if (request.getPorePressurePpg() != null) {
            marginToPore = ecd.subtract(request.getPorePressurePpg(), MC);
            isBelowPorePressure = ecd.compareTo(request.getPorePressurePpg()) < 0;
        }

        return EcdCalculationResult.builder()
                .ecdPpg(ecd.setScale(3, RoundingMode.HALF_UP))
                .ecdIncrementPpg(ecdIncrement.setScale(3, RoundingMode.HALF_UP))
                .annularPressureLossPsi(annularPressureLoss.setScale(2, RoundingMode.HALF_UP))
                .hydrostaticPressurePsi(hydrostaticPressure.setScale(2, RoundingMode.HALF_UP))
                .bottomHolePressurePsi(bottomHolePressure.setScale(2, RoundingMode.HALF_UP))
                .isAboveFracGradient(isAboveFracGradient)
                .isBelowPorePressure(isBelowPorePressure)
                .marginToFracPpg(marginToFrac != null ? marginToFrac.setScale(3, RoundingMode.HALF_UP) : null)
                .marginToPorePpg(marginToPore != null ? marginToPore.setScale(3, RoundingMode.HALF_UP) : null)
                .build();
    }

    /**
     * Simplified annular pressure loss calculation using Bingham plastic model.
     */
    private BigDecimal calculateAnnularPressureLoss(EcdCalculationRequest request) {
        // Simplified APL calculation
        // APL = (PV × V × L) / (1000 × (Dh - Dp)²) + (YP × L) / (200 × (Dh - Dp))
        // This is a simplified version; actual calculations require more parameters

        if (request.getPlasticViscosityCp() == null || request.getYieldPointLbf100sqft() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal pv = request.getPlasticViscosityCp();
        BigDecimal yp = request.getYieldPointLbf100sqft();
        BigDecimal annularLength = request.getTvdFt(); // Simplified: use TVD as length
        BigDecimal holeId = request.getHoleIdIn() != null ? request.getHoleIdIn() : BigDecimal.valueOf(8.5);
        BigDecimal pipeOd = request.getPipeOdIn() != null ? request.getPipeOdIn() : BigDecimal.valueOf(5.0);

        BigDecimal annularGap = holeId.subtract(pipeOd, MC);
        if (annularGap.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Annular velocity (simplified)
        BigDecimal flowRate = request.getFlowRateGpm();
        BigDecimal annularArea = PI.multiply(holeId.pow(2).subtract(pipeOd.pow(2)), MC)
                .divide(BigDecimal.valueOf(4), MC);
        BigDecimal annularVelocity = flowRate.multiply(BigDecimal.valueOf(0.408), MC)
                .divide(annularArea, MC);

        // Simplified pressure loss
        BigDecimal viscousLoss = pv.multiply(annularVelocity, MC).multiply(annularLength, MC)
                .divide(BigDecimal.valueOf(1000).multiply(annularGap.pow(2), MC), MC);

        BigDecimal yieldLoss = yp.multiply(annularLength, MC)
                .divide(BigDecimal.valueOf(200).multiply(annularGap, MC), MC);

        return viscousLoss.add(yieldLoss, MC);
    }

    // ==================== Swab and Surge ====================

    /**
     * Calculate Swab and Surge pressures during tripping operations.
     *
     * Surge (tripping in) increases bottom hole pressure
     * Swab (tripping out) decreases bottom hole pressure
     */
    public SwabSurgeCalculationResult calculateSwabSurge(SwabSurgeCalculationRequest request) {
        log.debug("Calculating Swab/Surge for trip speed {} ft/min", request.getTripSpeedFtMin());

        BigDecimal mudWeight = request.getMudWeightPpg();
        BigDecimal tvd = request.getTvdFt();
        BigDecimal tripSpeed = request.getTripSpeedFtMin();

        // Calculate clinging constant based on annular geometry
        BigDecimal holeId = request.getHoleIdIn();
        BigDecimal pipeOd = request.getPipeOdIn();
        BigDecimal closedPipeOd = request.getClosedEndOdIn() != null ?
                request.getClosedEndOdIn() : pipeOd;

        // K factor for open-ended pipe vs closed
        BigDecimal kFactor = request.getIsOpenEnded() != null && request.getIsOpenEnded() ?
                BigDecimal.valueOf(0.45) : BigDecimal.valueOf(1.0);

        // Annular velocity due to pipe movement
        BigDecimal pipeArea = PI.multiply(pipeOd.pow(2), MC).divide(BigDecimal.valueOf(4), MC);
        BigDecimal holeArea = PI.multiply(holeId.pow(2), MC).divide(BigDecimal.valueOf(4), MC);
        BigDecimal annularArea = holeArea.subtract(pipeArea, MC);

        BigDecimal clingingVelocity = tripSpeed.multiply(pipeArea, MC)
                .divide(annularArea, MC)
                .multiply(kFactor, MC);

        // Pressure change due to clinging effect (simplified)
        BigDecimal pv = request.getPlasticViscosityCp() != null ?
                request.getPlasticViscosityCp() : BigDecimal.valueOf(20);
        BigDecimal yp = request.getYieldPointLbf100sqft() != null ?
                request.getYieldPointLbf100sqft() : BigDecimal.valueOf(10);

        BigDecimal annularGap = holeId.subtract(pipeOd, MC).divide(BigDecimal.valueOf(2), MC);

        // Simplified pressure change calculation
        BigDecimal pressureChange = pv.multiply(clingingVelocity, MC).multiply(tvd, MC)
                .divide(BigDecimal.valueOf(1500).multiply(annularGap.pow(2), MC), MC)
                .add(yp.multiply(tvd, MC).divide(BigDecimal.valueOf(225).multiply(annularGap, MC), MC));

        // Convert to ECD equivalent
        BigDecimal ecdChange = pressureChange
                .divide(BigDecimal.valueOf(0.052).multiply(tvd, MC), MC);

        // Swab reduces pressure, Surge increases
        BigDecimal swabEcd = mudWeight.subtract(ecdChange, MC);
        BigDecimal surgeEcd = mudWeight.add(ecdChange, MC);

        BigDecimal swabPressure = swabEcd.multiply(BigDecimal.valueOf(0.052), MC).multiply(tvd, MC);
        BigDecimal surgePressure = surgeEcd.multiply(BigDecimal.valueOf(0.052), MC).multiply(tvd, MC);

        // Check against formation pressures
        Boolean swabBelowPore = null;
        Boolean surgeAboveFrac = null;
        BigDecimal maxSafeTripSpeed = null;

        if (request.getPorePressurePpg() != null) {
            swabBelowPore = swabEcd.compareTo(request.getPorePressurePpg()) < 0;
        }

        if (request.getFracGradientPpg() != null) {
            surgeAboveFrac = surgeEcd.compareTo(request.getFracGradientPpg()) > 0;

            // Calculate max safe trip speed
            BigDecimal maxEcdIncrease = request.getFracGradientPpg().subtract(mudWeight, MC);
            if (maxEcdIncrease.compareTo(BigDecimal.ZERO) > 0 && ecdChange.compareTo(BigDecimal.ZERO) > 0) {
                maxSafeTripSpeed = tripSpeed.multiply(maxEcdIncrease, MC).divide(ecdChange, MC);
            }
        }

        return SwabSurgeCalculationResult.builder()
                .swabEcdPpg(swabEcd.setScale(3, RoundingMode.HALF_UP))
                .surgeEcdPpg(surgeEcd.setScale(3, RoundingMode.HALF_UP))
                .swabPressurePsi(swabPressure.setScale(2, RoundingMode.HALF_UP))
                .surgePressurePsi(surgePressure.setScale(2, RoundingMode.HALF_UP))
                .pressureChangePsi(pressureChange.setScale(2, RoundingMode.HALF_UP))
                .ecdChangePpg(ecdChange.setScale(3, RoundingMode.HALF_UP))
                .swabBelowPorePressure(swabBelowPore)
                .surgeAboveFracGradient(surgeAboveFrac)
                .maxSafeTripSpeedFtMin(maxSafeTripSpeed != null ?
                        maxSafeTripSpeed.setScale(1, RoundingMode.HALF_UP) : null)
                .build();
    }

    // ==================== Kick Tolerance ====================

    /**
     * Calculate Kick Tolerance - maximum influx volume that can be safely circulated out.
     */
    public KickToleranceCalculationResult calculateKickTolerance(KickToleranceCalculationRequest request) {
        log.debug("Calculating Kick Tolerance at TVD {} ft", request.getTvdFt());

        BigDecimal mudWeight = request.getMudWeightPpg();
        BigDecimal tvd = request.getTvdFt();
        BigDecimal fracGradient = request.getFracGradientPpg();
        BigDecimal porePressure = request.getPorePressurePpg();
        BigDecimal annularCapacityBblFt = request.getAnnularCapacityBblFt();
        BigDecimal shoeDepthTvdFt = request.getShoeDepthTvdFt();

        // Available kick margin (ppg)
        BigDecimal kickMargin = fracGradient.subtract(mudWeight, MC);

        // Influx gradient (assume gas = 0.1 ppg for worst case)
        BigDecimal influxGradient = request.getInfluxGradientPpg() != null ?
                request.getInfluxGradientPpg() : BigDecimal.valueOf(0.1);

        // Height of influx that would fracture formation at shoe
        // (Frac at shoe - MW) × 0.052 × Shoe TVD = (MW - Influx Grad) × 0.052 × Height
        BigDecimal fracAtShoe = fracGradient; // Simplified: same frac gradient at shoe
        BigDecimal availablePressure = fracAtShoe.subtract(mudWeight, MC)
                .multiply(BigDecimal.valueOf(0.052), MC)
                .multiply(shoeDepthTvdFt, MC);

        BigDecimal gradientDiff = mudWeight.subtract(influxGradient, MC);
        BigDecimal maxInfluxHeight = BigDecimal.ZERO;
        if (gradientDiff.compareTo(BigDecimal.ZERO) > 0) {
            maxInfluxHeight = availablePressure
                    .divide(BigDecimal.valueOf(0.052).multiply(gradientDiff, MC), MC);
        }

        // Max influx volume
        BigDecimal maxInfluxVolume = maxInfluxHeight.multiply(annularCapacityBblFt, MC);

        // MAASP (Maximum Allowable Annular Surface Pressure)
        BigDecimal maasp = fracAtShoe.subtract(mudWeight, MC)
                .multiply(BigDecimal.valueOf(0.052), MC)
                .multiply(shoeDepthTvdFt, MC);

        // MASICP (Maximum Allowable Shut-In Casing Pressure)
        BigDecimal masicp = maasp; // Simplified: same as MAASP for initial shut-in

        // Required kill mud weight
        BigDecimal killMudWeight = porePressure.add(BigDecimal.valueOf(0.5), MC); // Add safety margin

        return KickToleranceCalculationResult.builder()
                .kickToleranceBbl(maxInfluxVolume.setScale(2, RoundingMode.HALF_UP))
                .maxInfluxHeightFt(maxInfluxHeight.setScale(2, RoundingMode.HALF_UP))
                .kickMarginPpg(kickMargin.setScale(3, RoundingMode.HALF_UP))
                .maaspPsi(maasp.setScale(2, RoundingMode.HALF_UP))
                .masicpPsi(masicp.setScale(2, RoundingMode.HALF_UP))
                .requiredKillMudWeightPpg(killMudWeight.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    // ==================== Torque & Drag ====================

    /**
     * Calculate Torque and Drag for drilling/tripping operations.
     * Uses simplified soft-string model.
     */
    public TorqueDragCalculationResult calculateTorqueDrag(TorqueDragCalculationRequest request) {
        log.debug("Calculating Torque & Drag for operation: {}", request.getOperation());

        BigDecimal frictionCoefficient = request.getFrictionCoefficient() != null ?
                request.getFrictionCoefficient() : BigDecimal.valueOf(0.25);
        BigDecimal mudWeight = request.getMudWeightPpg();
        BigDecimal stringWeightLbsPerFt = request.getStringWeightLbsPerFt();
        BigDecimal totalDepthFt = request.getTotalDepthFt();
        BigDecimal avgInclinationDeg = request.getAvgInclinationDeg();

        // Buoyancy factor
        BigDecimal steelDensity = BigDecimal.valueOf(65.5); // ppg
        BigDecimal buoyancyFactor = BigDecimal.ONE.subtract(
                mudWeight.divide(steelDensity, MC), MC);

        // Buoyed string weight
        BigDecimal buoyedWeightPerFt = stringWeightLbsPerFt.multiply(buoyancyFactor, MC);
        BigDecimal totalBuoyedWeight = buoyedWeightPerFt.multiply(totalDepthFt, MC);

        // Convert inclination to radians
        double incRad = Math.toRadians(avgInclinationDeg.doubleValue());

        // Axial (drag) force components
        BigDecimal normalForce = totalBuoyedWeight.multiply(
                BigDecimal.valueOf(Math.sin(incRad)), MC);
        BigDecimal axialForce = totalBuoyedWeight.multiply(
                BigDecimal.valueOf(Math.cos(incRad)), MC);

        // Drag force
        BigDecimal dragForce = normalForce.multiply(frictionCoefficient, MC);

        // Hook load calculations
        BigDecimal rotatingHookLoad = axialForce;
        BigDecimal trippingInHookLoad = axialForce.subtract(dragForce, MC);
        BigDecimal trippingOutHookLoad = axialForce.add(dragForce, MC);
        BigDecimal slidingHookLoad = axialForce.add(dragForce, MC); // Same as trip out for sliding

        // Torque calculation
        // Torque = μ × N × r, where N = normal force, r = tool joint radius
        BigDecimal pipeOd = request.getPipeOdIn() != null ? request.getPipeOdIn() : BigDecimal.valueOf(5.0);
        BigDecimal radius = pipeOd.divide(BigDecimal.valueOf(24), MC); // Convert to feet
        BigDecimal torque = frictionCoefficient.multiply(normalForce, MC).multiply(radius, MC);

        // Determine which values to highlight based on operation
        BigDecimal operationHookLoad;
        String operation = request.getOperation() != null ? request.getOperation().toUpperCase() : "ROTATING";
        switch (operation) {
            case "TRIPPING_IN":
                operationHookLoad = trippingInHookLoad;
                break;
            case "TRIPPING_OUT":
                operationHookLoad = trippingOutHookLoad;
                break;
            case "SLIDING":
                operationHookLoad = slidingHookLoad;
                break;
            default:
                operationHookLoad = rotatingHookLoad;
        }

        return TorqueDragCalculationResult.builder()
                .rotatingHookLoadLbs(rotatingHookLoad.setScale(0, RoundingMode.HALF_UP))
                .trippingInHookLoadLbs(trippingInHookLoad.setScale(0, RoundingMode.HALF_UP))
                .trippingOutHookLoadLbs(trippingOutHookLoad.setScale(0, RoundingMode.HALF_UP))
                .slidingHookLoadLbs(slidingHookLoad.setScale(0, RoundingMode.HALF_UP))
                .surfaceTorqueFtLbs(torque.setScale(0, RoundingMode.HALF_UP))
                .dragForceLbs(dragForce.setScale(0, RoundingMode.HALF_UP))
                .buoyancyFactor(buoyancyFactor.setScale(4, RoundingMode.HALF_UP))
                .totalBuoyedStringWeightLbs(totalBuoyedWeight.setScale(0, RoundingMode.HALF_UP))
                .operationHookLoadLbs(operationHookLoad.setScale(0, RoundingMode.HALF_UP))
                .operation(operation)
                .build();
    }

    // ==================== DLS (Dog Leg Severity) ====================

    /**
     * Calculate Dog Leg Severity between two survey points.
     * DLS = (180/π) × arccos[cos(I2-I1) - sin(I1)×sin(I2)×(1-cos(A2-A1))] × (100/ΔMD)
     */
    public DlsCalculationResult calculateDls(DlsCalculationRequest request) {
        log.debug("Calculating DLS between depths {} and {} ft",
                request.getMd1Ft(), request.getMd2Ft());

        double inc1 = Math.toRadians(request.getInclination1Deg().doubleValue());
        double inc2 = Math.toRadians(request.getInclination2Deg().doubleValue());
        double azi1 = Math.toRadians(request.getAzimuth1Deg().doubleValue());
        double azi2 = Math.toRadians(request.getAzimuth2Deg().doubleValue());
        double md1 = request.getMd1Ft().doubleValue();
        double md2 = request.getMd2Ft().doubleValue();

        double courseLength = md2 - md1;
        if (courseLength <= 0) {
            throw new IllegalArgumentException("MD2 must be greater than MD1");
        }

        // Calculate dogleg angle
        double cosD = Math.cos(inc2 - inc1) -
                Math.sin(inc1) * Math.sin(inc2) * (1 - Math.cos(azi2 - azi1));
        double dogleg = Math.acos(Math.max(-1, Math.min(1, cosD)));

        // DLS in degrees per 100 feet
        double dlsDegPer100ft = Math.toDegrees(dogleg) * 100.0 / courseLength;

        // Inclination change
        double incChange = Math.toDegrees(inc2 - inc1);

        // Azimuth change (handling wrap-around)
        double aziChange = Math.toDegrees(azi2 - azi1);
        if (aziChange > 180) aziChange -= 360;
        if (aziChange < -180) aziChange += 360;

        // Build-up or drop rate
        double buildRate = incChange * 100.0 / courseLength;

        // Turn rate
        double turnRate = aziChange * 100.0 / courseLength;

        // Severity classification
        String severity;
        if (dlsDegPer100ft < 1.0) {
            severity = "LOW";
        } else if (dlsDegPer100ft < 3.0) {
            severity = "MODERATE";
        } else if (dlsDegPer100ft < 6.0) {
            severity = "HIGH";
        } else {
            severity = "SEVERE";
        }

        return DlsCalculationResult.builder()
                .dlsDegPer100ft(BigDecimal.valueOf(dlsDegPer100ft).setScale(3, RoundingMode.HALF_UP))
                .doglegAngleDeg(BigDecimal.valueOf(Math.toDegrees(dogleg)).setScale(3, RoundingMode.HALF_UP))
                .courseLengthFt(BigDecimal.valueOf(courseLength).setScale(2, RoundingMode.HALF_UP))
                .inclinationChangeDeg(BigDecimal.valueOf(incChange).setScale(3, RoundingMode.HALF_UP))
                .azimuthChangeDeg(BigDecimal.valueOf(aziChange).setScale(3, RoundingMode.HALF_UP))
                .buildRateDegPer100ft(BigDecimal.valueOf(buildRate).setScale(3, RoundingMode.HALF_UP))
                .turnRateDegPer100ft(BigDecimal.valueOf(turnRate).setScale(3, RoundingMode.HALF_UP))
                .severity(severity)
                .build();
    }

    // ==================== Hydraulics ====================

    /**
     * Calculate bit hydraulics (HSI, Impact Force, Jet Velocity).
     */
    public BitHydraulicsCalculationResult calculateBitHydraulics(BitHydraulicsCalculationRequest request) {
        log.debug("Calculating bit hydraulics for TFA {} sq.in", request.getTotalFlowAreaSqIn());

        BigDecimal flowRate = request.getFlowRateGpm();
        BigDecimal mudWeight = request.getMudWeightPpg();
        BigDecimal tfa = request.getTotalFlowAreaSqIn();
        BigDecimal bitDiameter = request.getBitDiameterIn();

        // Jet velocity (ft/s)
        // Vn = 0.3208 × Q / TFA
        BigDecimal jetVelocity = BigDecimal.valueOf(0.3208)
                .multiply(flowRate, MC)
                .divide(tfa, MC);

        // Bit pressure drop (psi)
        // ΔPbit = (MW × Q²) / (12042 × TFA²)
        BigDecimal bitPressureDrop = mudWeight.multiply(flowRate.pow(2), MC)
                .divide(BigDecimal.valueOf(12042).multiply(tfa.pow(2), MC), MC);

        // Bit hydraulic horsepower (HHP)
        // HHP = Q × ΔP / 1714
        BigDecimal bitHhp = flowRate.multiply(bitPressureDrop, MC)
                .divide(BigDecimal.valueOf(1714), MC);

        // Hydraulic Horsepower per Square Inch (HSI)
        // HSI = HHP / Bit Area
        BigDecimal bitArea = PI.multiply(bitDiameter.pow(2), MC).divide(BigDecimal.valueOf(4), MC);
        BigDecimal hsi = bitHhp.divide(bitArea, MC);

        // Impact Force (lbs)
        // IF = 0.01823 × MW × Q × Vn
        BigDecimal impactForce = BigDecimal.valueOf(0.01823)
                .multiply(mudWeight, MC)
                .multiply(flowRate, MC)
                .multiply(jetVelocity, MC);

        // Percent pressure drop at bit (simplified - assume 50-65% is optimal)
        BigDecimal sppPsi = request.getStandpipePressurePsi();
        BigDecimal percentAtBit = sppPsi != null && sppPsi.compareTo(BigDecimal.ZERO) > 0 ?
                bitPressureDrop.divide(sppPsi, MC).multiply(BigDecimal.valueOf(100), MC) : null;

        return BitHydraulicsCalculationResult.builder()
                .jetVelocityFtSec(jetVelocity.setScale(2, RoundingMode.HALF_UP))
                .bitPressureDropPsi(bitPressureDrop.setScale(2, RoundingMode.HALF_UP))
                .bitHydraulicHorsePower(bitHhp.setScale(2, RoundingMode.HALF_UP))
                .hsiHpPerSqIn(hsi.setScale(3, RoundingMode.HALF_UP))
                .impactForceLbs(impactForce.setScale(0, RoundingMode.HALF_UP))
                .percentPressureAtBit(percentAtBit != null ?
                        percentAtBit.setScale(1, RoundingMode.HALF_UP) : null)
                .build();
    }
}
