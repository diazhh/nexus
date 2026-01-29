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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.rv.dto.RvMaterialBalanceDto;
import org.thingsboard.nexus.rv.dto.RvMaterialBalanceDto.MaterialBalanceDataPoint;
import org.thingsboard.nexus.rv.exception.RvCalculationException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service for Material Balance calculations using the Havlena-Odeh method.
 *
 * The General Material Balance Equation (MBE):
 * F = N[Eo + m*Eg + Efw] + We
 *
 * Where:
 * F = Underground withdrawal = Np*Bo + (Gp - Np*Rs)*Bg + Wp*Bw - Wi*Bw - Gi*Bg
 * Eo = Oil expansion = (Bo - Boi) + (Rsi - Rs)*Bg
 * Eg = Gas cap expansion = Boi*(Bg/Bgi - 1)
 * Efw = Formation and water expansion = (1 + m)*Boi*[(cw*Swi + cf)/(1-Swi)]*ΔP
 * We = Water influx
 * N = Original oil in place (OOIP)
 * m = Gas cap ratio = GBgi/(NBoi)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvMaterialBalanceService {

    private final RvAssetService assetService;
    private final ObjectMapper objectMapper;

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 8;

    /**
     * Calculate Material Balance terms for each data point.
     * This prepares the data for Havlena-Odeh analysis.
     */
    public RvMaterialBalanceDto calculateMBETerms(RvMaterialBalanceDto study) {
        log.info("Calculating MBE terms for study: {}", study.getName());

        if (study.getDataPoints() == null || study.getDataPoints().isEmpty()) {
            throw new RvCalculationException("No data points provided for material balance calculation");
        }

        validateInitialConditions(study);

        BigDecimal Boi = study.getInitialBo();
        BigDecimal Bgi = study.getInitialBg();
        BigDecimal Rsi = study.getInitialRs();
        BigDecimal Pi = study.getInitialPressure();
        BigDecimal Swi = study.getInitialWaterSaturation();
        BigDecimal cw = study.getWaterCompressibility();
        BigDecimal cf = study.getRockCompressibility();
        BigDecimal m = study.getGasCapRatio() != null ? study.getGasCapRatio() : BigDecimal.ZERO;

        for (MaterialBalanceDataPoint dp : study.getDataPoints()) {
            // Get PVT properties at this pressure
            BigDecimal Bo = dp.getBo();
            BigDecimal Bg = dp.getBg();
            BigDecimal Rs = dp.getRs();
            BigDecimal Bw = dp.getBw() != null ? dp.getBw() : BigDecimal.ONE;

            BigDecimal Np = dp.getCumulativeOilProduction() != null ? dp.getCumulativeOilProduction() : BigDecimal.ZERO;
            BigDecimal Gp = dp.getCumulativeGasProduction() != null ? dp.getCumulativeGasProduction() : BigDecimal.ZERO;
            BigDecimal Wp = dp.getCumulativeWaterProduction() != null ? dp.getCumulativeWaterProduction() : BigDecimal.ZERO;
            BigDecimal Wi = dp.getCumulativeWaterInjection() != null ? dp.getCumulativeWaterInjection() : BigDecimal.ZERO;
            BigDecimal Gi = dp.getCumulativeGasInjection() != null ? dp.getCumulativeGasInjection() : BigDecimal.ZERO;

            // Calculate F - Underground withdrawal
            // F = Np*Bo + (Gp - Np*Rs)*Bg + Wp*Bw - Wi*Bw - Gi*Bg
            // Note: Gp is in Bcf, convert to MMscf for consistency with Bo units
            BigDecimal GpMmscf = Gp.multiply(new BigDecimal("1000")); // Bcf to MMscf
            BigDecimal F = Np.multiply(Bo)
                    .add(GpMmscf.subtract(Np.multiply(Rs)).multiply(Bg))
                    .add(Wp.multiply(Bw))
                    .subtract(Wi.multiply(Bw))
                    .subtract(Gi.multiply(new BigDecimal("1000")).multiply(Bg));
            dp.setF(F.setScale(SCALE, RoundingMode.HALF_UP));

            // Calculate Eo - Oil expansion
            // Eo = (Bo - Boi) + (Rsi - Rs)*Bg
            BigDecimal Eo = Bo.subtract(Boi)
                    .add(Rsi.subtract(Rs).multiply(Bg));
            dp.setEo(Eo.setScale(SCALE, RoundingMode.HALF_UP));

            // Calculate Eg - Gas cap expansion
            // Eg = Boi * (Bg/Bgi - 1)
            BigDecimal Eg = BigDecimal.ZERO;
            if (Bgi != null && Bgi.compareTo(BigDecimal.ZERO) > 0) {
                Eg = Boi.multiply(Bg.divide(Bgi, MC).subtract(BigDecimal.ONE));
            }
            dp.setEg(Eg.setScale(SCALE, RoundingMode.HALF_UP));

            // Calculate Efw - Formation and water expansion
            // Efw = (1 + m)*Boi*[(cw*Swi + cf)/(1-Swi)]*ΔP
            BigDecimal deltaP = Pi.subtract(dp.getPressure());
            BigDecimal Efw = BigDecimal.ZERO;
            if (cw != null && cf != null && Swi != null) {
                BigDecimal numerator = cw.multiply(Swi).add(cf);
                BigDecimal denominator = BigDecimal.ONE.subtract(Swi);
                if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                    Efw = BigDecimal.ONE.add(m)
                            .multiply(Boi)
                            .multiply(numerator.divide(denominator, MC))
                            .multiply(deltaP);
                }
            }
            dp.setEfw(Efw.setScale(SCALE, RoundingMode.HALF_UP));

            log.debug("Data point at P={}: F={}, Eo={}, Eg={}, Efw={}",
                    dp.getPressure(), dp.getF(), dp.getEo(), dp.getEg(), dp.getEfw());
        }

        return study;
    }

    /**
     * Perform Havlena-Odeh analysis to determine OOIP and drive mechanisms.
     *
     * For undersaturated reservoir (P > Pb), no gas cap, no water influx:
     * F = N * (Eo + Efw)
     * Plot F vs (Eo + Efw): slope = N
     *
     * For saturated reservoir with gas cap:
     * F = N * [Eo + m*Eg + Efw]
     * Plot F vs [Eo + m*Eg + Efw]: slope = N
     *
     * For water drive reservoir:
     * F = N*Eo + We
     * Plot F/Eo vs We/Eo: slope = 1, intercept = N
     */
    public RvMaterialBalanceDto performHavlenaOdehAnalysis(RvMaterialBalanceDto study) {
        log.info("Performing Havlena-Odeh analysis for study: {}", study.getName());

        // First calculate MBE terms
        calculateMBETerms(study);

        List<MaterialBalanceDataPoint> dataPoints = study.getDataPoints();
        if (dataPoints.size() < 3) {
            throw new RvCalculationException("At least 3 data points required for Havlena-Odeh analysis");
        }

        BigDecimal m = study.getGasCapRatio() != null ? study.getGasCapRatio() : BigDecimal.ZERO;
        boolean hasGasCap = study.getHasGasCap() != null && study.getHasGasCap();
        boolean hasAquifer = study.getHasAquiferSupport() != null && study.getHasAquiferSupport();

        // Determine plot type based on reservoir characteristics
        String plotType;
        List<BigDecimal> xValues = new ArrayList<>();
        List<BigDecimal> yValues = new ArrayList<>();

        if (!hasGasCap && !hasAquifer) {
            // Simple depletion: F vs Eo (+ Efw)
            plotType = RvMaterialBalanceDto.PLOT_F_VS_EO;
            for (MaterialBalanceDataPoint dp : dataPoints) {
                BigDecimal x = dp.getEo().add(dp.getEfw());
                BigDecimal y = dp.getF();
                dp.setXAxis(x);
                dp.setYAxis(y);
                if (x.compareTo(BigDecimal.ZERO) > 0) {
                    xValues.add(x);
                    yValues.add(y);
                }
            }
        } else if (hasGasCap && !hasAquifer) {
            // Gas cap drive: F vs (Eo + m*Eg + Efw)
            plotType = RvMaterialBalanceDto.PLOT_F_VS_EO_EG;
            for (MaterialBalanceDataPoint dp : dataPoints) {
                BigDecimal x = dp.getEo().add(m.multiply(dp.getEg())).add(dp.getEfw());
                BigDecimal y = dp.getF();
                dp.setXAxis(x);
                dp.setYAxis(y);
                if (x.compareTo(BigDecimal.ZERO) > 0) {
                    xValues.add(x);
                    yValues.add(y);
                }
            }
        } else {
            // Water drive: F/Eo vs We/Eo (requires We calculation)
            plotType = RvMaterialBalanceDto.PLOT_F_EO_VS_EW_EF;
            // For now, use simplified approach without aquifer model
            for (MaterialBalanceDataPoint dp : dataPoints) {
                BigDecimal x = dp.getEfw().divide(dp.getEo(), MC);
                BigDecimal y = dp.getF().divide(dp.getEo(), MC);
                dp.setXAxis(x);
                dp.setYAxis(y);
                if (dp.getEo().compareTo(BigDecimal.ZERO) > 0) {
                    xValues.add(x);
                    yValues.add(y);
                }
            }
        }

        study.setPlotType(plotType);

        // Perform linear regression
        LinearRegressionResult regression = performLinearRegression(xValues, yValues);

        study.setRegressionSlope(regression.slope);
        study.setRegressionIntercept(regression.intercept);
        study.setRegressionR2(regression.r2);

        // Interpret results
        if (plotType.equals(RvMaterialBalanceDto.PLOT_F_VS_EO) ||
            plotType.equals(RvMaterialBalanceDto.PLOT_F_VS_EO_EG)) {
            // Slope = N (OOIP in same units as F, typically MMbbl)
            study.setCalculatedOOIP(regression.slope.setScale(4, RoundingMode.HALF_UP));
        } else {
            // Intercept = N for water drive plot
            study.setCalculatedOOIP(regression.intercept.setScale(4, RoundingMode.HALF_UP));
        }

        // Calculate drive indices
        calculateDriveIndices(study);

        // Assess analysis quality based on R²
        if (regression.r2.compareTo(new BigDecimal("0.95")) >= 0) {
            study.setAnalysisQuality("EXCELLENT");
        } else if (regression.r2.compareTo(new BigDecimal("0.85")) >= 0) {
            study.setAnalysisQuality("GOOD");
        } else if (regression.r2.compareTo(new BigDecimal("0.70")) >= 0) {
            study.setAnalysisQuality("FAIR");
        } else {
            study.setAnalysisQuality("POOR");
        }

        log.info("Havlena-Odeh analysis complete: OOIP={} MMSTB, R²={}, Quality={}",
                study.getCalculatedOOIP(), regression.r2, study.getAnalysisQuality());

        return study;
    }

    /**
     * Calculate Drive Mechanism Indices.
     * These indices indicate the relative contribution of each drive mechanism.
     * Sum of all indices should equal 1.0 (100%).
     */
    private void calculateDriveIndices(RvMaterialBalanceDto study) {
        List<MaterialBalanceDataPoint> dataPoints = study.getDataPoints();
        if (dataPoints.isEmpty()) return;

        // Use the last data point for drive index calculation
        MaterialBalanceDataPoint lastPoint = dataPoints.get(dataPoints.size() - 1);

        BigDecimal N = study.getCalculatedOOIP();
        if (N == null || N.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal m = study.getGasCapRatio() != null ? study.getGasCapRatio() : BigDecimal.ZERO;
        BigDecimal F = lastPoint.getF();

        if (F == null || F.compareTo(BigDecimal.ZERO) <= 0) return;

        // DDI (Depletion Drive Index) = N * Eo / F
        BigDecimal ddi = N.multiply(lastPoint.getEo()).divide(F, MC);
        study.setSolutionGasDriveIndex(ddi.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP));

        // SDI (Segregation/Gas Cap Drive Index) = N * m * Eg / F
        BigDecimal sdi = BigDecimal.ZERO;
        if (m.compareTo(BigDecimal.ZERO) > 0 && lastPoint.getEg() != null) {
            sdi = N.multiply(m).multiply(lastPoint.getEg()).divide(F, MC);
        }
        study.setGasCapDriveIndex(sdi.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP));

        // CDI (Compaction/Formation Drive Index) = N * Efw / F
        BigDecimal cdi = BigDecimal.ZERO;
        if (lastPoint.getEfw() != null) {
            cdi = N.multiply(lastPoint.getEfw()).divide(F, MC);
        }
        study.setCompactionDriveIndex(cdi.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP));

        // WDI (Water Drive Index) = We / F (estimated as remainder)
        BigDecimal wdi = BigDecimal.ONE.subtract(ddi).subtract(sdi).subtract(cdi);
        wdi = wdi.max(BigDecimal.ZERO);
        study.setWaterDriveIndex(wdi.setScale(4, RoundingMode.HALF_UP));

        // Determine primary drive mechanism
        BigDecimal maxIndex = ddi;
        String primaryDrive = RvMaterialBalanceDto.DRIVE_SOLUTION_GAS;

        if (sdi.compareTo(maxIndex) > 0) {
            maxIndex = sdi;
            primaryDrive = RvMaterialBalanceDto.DRIVE_GAS_CAP;
        }
        if (wdi.compareTo(maxIndex) > 0) {
            maxIndex = wdi;
            primaryDrive = RvMaterialBalanceDto.DRIVE_WATER;
        }
        if (cdi.compareTo(maxIndex) > 0) {
            primaryDrive = RvMaterialBalanceDto.DRIVE_COMPACTION;
        }

        // If no single mechanism dominates (>50%), it's combination
        if (maxIndex.compareTo(new BigDecimal("0.5")) < 0) {
            primaryDrive = RvMaterialBalanceDto.DRIVE_COMBINATION;
        }

        study.setPrimaryDriveMechanism(primaryDrive);

        log.debug("Drive indices: DDI={}, SDI={}, WDI={}, CDI={}, Primary={}",
                study.getSolutionGasDriveIndex(), study.getGasCapDriveIndex(),
                study.getWaterDriveIndex(), study.getCompactionDriveIndex(),
                primaryDrive);
    }

    /**
     * Calculate water influx using the Fetkovich aquifer model.
     * Wei = J * (Pi - P) * Δt
     * We(cumulative) = Sum of Wei
     */
    public BigDecimal calculateFetkovichWaterInflux(BigDecimal aquiferProductivityIndex,
                                                     BigDecimal initialPressure,
                                                     BigDecimal currentPressure,
                                                     BigDecimal timeStepDays) {
        // Wei = J * (Pi - P) * Δt
        BigDecimal avgPressureDrop = initialPressure.subtract(currentPressure);
        return aquiferProductivityIndex.multiply(avgPressureDrop).multiply(timeStepDays);
    }

    /**
     * Calculate water influx using Carter-Tracy method (simplified).
     * More accurate for infinite-acting aquifers.
     */
    public BigDecimal calculateCarterTracyWaterInflux(BigDecimal B, BigDecimal deltaPressure,
                                                       BigDecimal tD, BigDecimal pD, BigDecimal pDPrime) {
        // We(n) = B * Sum[ΔP(j) * (pD(tD) - pD(tD-1))]
        // Simplified single step calculation
        if (pDPrime.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return B.multiply(deltaPressure).multiply(pD);
    }

    /**
     * Simple linear regression for Havlena-Odeh plot.
     */
    private LinearRegressionResult performLinearRegression(List<BigDecimal> xValues, List<BigDecimal> yValues) {
        if (xValues.size() != yValues.size() || xValues.isEmpty()) {
            throw new RvCalculationException("Invalid data for regression");
        }

        int n = xValues.size();
        BigDecimal sumX = BigDecimal.ZERO;
        BigDecimal sumY = BigDecimal.ZERO;
        BigDecimal sumXY = BigDecimal.ZERO;
        BigDecimal sumX2 = BigDecimal.ZERO;
        BigDecimal sumY2 = BigDecimal.ZERO;

        for (int i = 0; i < n; i++) {
            BigDecimal x = xValues.get(i);
            BigDecimal y = yValues.get(i);
            sumX = sumX.add(x);
            sumY = sumY.add(y);
            sumXY = sumXY.add(x.multiply(y));
            sumX2 = sumX2.add(x.multiply(x));
            sumY2 = sumY2.add(y.multiply(y));
        }

        BigDecimal nBD = new BigDecimal(n);

        // slope = (n*sumXY - sumX*sumY) / (n*sumX2 - sumX²)
        BigDecimal numerator = nBD.multiply(sumXY).subtract(sumX.multiply(sumY));
        BigDecimal denominator = nBD.multiply(sumX2).subtract(sumX.multiply(sumX));

        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            throw new RvCalculationException("Cannot perform regression: denominator is zero");
        }

        BigDecimal slope = numerator.divide(denominator, MC);

        // intercept = (sumY - slope*sumX) / n
        BigDecimal intercept = sumY.subtract(slope.multiply(sumX)).divide(nBD, MC);

        // R² = [n*sumXY - sumX*sumY]² / [(n*sumX2 - sumX²) * (n*sumY2 - sumY²)]
        BigDecimal ssxy = numerator;
        BigDecimal ssx = denominator;
        BigDecimal ssy = nBD.multiply(sumY2).subtract(sumY.multiply(sumY));

        BigDecimal r2 = BigDecimal.ZERO;
        if (ssx.compareTo(BigDecimal.ZERO) > 0 && ssy.compareTo(BigDecimal.ZERO) > 0) {
            r2 = ssxy.multiply(ssxy).divide(ssx.multiply(ssy), MC);
        }

        return new LinearRegressionResult(
                slope.setScale(SCALE, RoundingMode.HALF_UP),
                intercept.setScale(SCALE, RoundingMode.HALF_UP),
                r2.setScale(4, RoundingMode.HALF_UP)
        );
    }

    private void validateInitialConditions(RvMaterialBalanceDto study) {
        if (study.getInitialPressure() == null) {
            throw new RvCalculationException("Initial pressure (Pi) is required");
        }
        if (study.getInitialBo() == null) {
            throw new RvCalculationException("Initial oil FVF (Boi) is required");
        }
        if (study.getInitialRs() == null) {
            throw new RvCalculationException("Initial solution GOR (Rsi) is required");
        }
    }

    /**
     * Helper class for regression results
     */
    private static class LinearRegressionResult {
        final BigDecimal slope;
        final BigDecimal intercept;
        final BigDecimal r2;

        LinearRegressionResult(BigDecimal slope, BigDecimal intercept, BigDecimal r2) {
            this.slope = slope;
            this.intercept = intercept;
            this.r2 = r2;
        }
    }

    // ========== CRUD Operations ==========

    public Page<RvMaterialBalanceDto> getAllMaterialBalanceStudies(UUID tenantId, int page, int size) {
        log.debug("Getting all material balance studies for tenant: {}", tenantId);
        // Implementation would query ThingsBoard assets
        return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
    }

    public Optional<RvMaterialBalanceDto> getMaterialBalanceStudyById(UUID id) {
        log.debug("Getting material balance study by ID: {}", id);
        return Optional.empty();
    }

    public List<RvMaterialBalanceDto> getMaterialBalanceStudiesByReservoir(UUID tenantId, UUID reservoirId) {
        log.debug("Getting material balance studies for reservoir: {}", reservoirId);
        return Collections.emptyList();
    }

    public RvMaterialBalanceDto createMaterialBalanceStudy(UUID tenantId, RvMaterialBalanceDto dto) {
        log.info("Creating material balance study: {}", dto.getName());
        dto.setId(UUID.randomUUID());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(System.currentTimeMillis());
        return dto;
    }

    public RvMaterialBalanceDto updateMaterialBalanceStudy(RvMaterialBalanceDto dto) {
        log.info("Updating material balance study: {}", dto.getId());
        dto.setUpdatedTime(System.currentTimeMillis());
        return dto;
    }

    public void deleteMaterialBalanceStudy(UUID tenantId, UUID id) {
        log.warn("Deleting material balance study: {}", id);
    }
}
