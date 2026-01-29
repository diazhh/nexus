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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.rv.dto.RvDeclineAnalysisDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service for managing Decline Curve Analysis entities.
 * Implements Arps decline equations for production forecasting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvDeclineAnalysisService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;
    private final RvCalculationService calculationService;

    /**
     * Creates a new Decline Analysis.
     */
    public RvDeclineAnalysisDto createDeclineAnalysis(UUID tenantId, RvDeclineAnalysisDto dto) {
        log.info("Creating decline analysis: {} for well: {}", dto.getName(), dto.getWellAssetId());

        if (dto.getWellAssetId() != null) {
            if (!rvAssetService.existsById(dto.getWellAssetId())) {
                throw new RvEntityNotFoundException("Well", dto.getWellAssetId());
            }
        }

        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvDeclineAnalysisDto.ASSET_TYPE,
            dto.getName(),
            dto.getLabel()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        saveDeclineAttributes(dto);

        // Link to well if specified
        if (dto.getWellAssetId() != null) {
            rvHierarchyService.createRelation(tenantId, dto.getWellAssetId(), dto.getAssetId(), "AnalyzedBy");
        }

        log.info("Decline analysis created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Decline Analysis by ID.
     */
    public Optional<RvDeclineAnalysisDto> getDeclineAnalysisById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvDeclineAnalysisDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvDeclineAnalysisDto dto = new RvDeclineAnalysisDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                loadDeclineAttributes(dto);

                return dto;
            });
    }

    /**
     * Gets all Decline Analyses for a tenant.
     */
    public Page<RvDeclineAnalysisDto> getAllDeclineAnalyses(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvDeclineAnalysisDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Decline Analyses by Well.
     */
    public List<RvDeclineAnalysisDto> getDeclineAnalysesByWell(UUID tenantId, UUID wellAssetId) {
        List<UUID> analysisIds = rvHierarchyService.getRelatedAssets(tenantId, wellAssetId, "AnalyzedBy");
        List<RvDeclineAnalysisDto> analyses = new ArrayList<>();

        for (UUID analysisId : analysisIds) {
            getDeclineAnalysisById(analysisId).ifPresent(analyses::add);
        }

        // Sort by analysis date (most recent first)
        analyses.sort((a, b) -> {
            Long dateA = a.getAnalysisDate() != null ? a.getAnalysisDate() : 0L;
            Long dateB = b.getAnalysisDate() != null ? b.getAnalysisDate() : 0L;
            return dateB.compareTo(dateA);
        });

        return analyses;
    }

    /**
     * Performs decline curve analysis and calculates Arps parameters.
     */
    public RvDeclineAnalysisDto performAnalysis(UUID assetId, BigDecimal qi, BigDecimal di, BigDecimal b,
                                                 BigDecimal economicLimit, int forecastYears) {
        log.info("Performing decline analysis for {}: qi={}, di={}, b={}", assetId, qi, di, b);

        RvDeclineAnalysisDto dto = getDeclineAnalysisById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("Decline Analysis", assetId));

        // Set Arps parameters
        dto.setQiBopd(qi);
        dto.setDiPerYear(di);
        dto.setDiPerMonth(di.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP));
        dto.setBExponent(b);
        dto.setEconomicLimitBopd(economicLimit);

        // Determine decline type
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            dto.setDeclineType("EXPONENTIAL");
        } else if (b.compareTo(BigDecimal.ONE) == 0) {
            dto.setDeclineType("HARMONIC");
        } else {
            dto.setDeclineType("HYPERBOLIC");
        }

        // Calculate EUR based on decline type
        BigDecimal eur = calculateEur(qi, di, b, economicLimit);
        dto.setEurBbl(eur);

        // Calculate rates at specific times
        dto.setRate1YearBopd(calculationService.calculateArpsDecline(qi, di, b, BigDecimal.ONE));
        dto.setRate3YearsBopd(calculationService.calculateArpsDecline(qi, di, b, BigDecimal.valueOf(3)));
        dto.setRate5YearsBopd(calculationService.calculateArpsDecline(qi, di, b, BigDecimal.valueOf(5)));
        dto.setRate10YearsBopd(calculationService.calculateArpsDecline(qi, di, b, BigDecimal.valueOf(10)));

        // Calculate cumulative production at specific times
        dto.setCumulative1YearBbl(calculationService.calculateArpsCumulative(qi, di, b, BigDecimal.ONE));
        dto.setCumulative3YearsBbl(calculationService.calculateArpsCumulative(qi, di, b, BigDecimal.valueOf(3)));
        dto.setCumulative5YearsBbl(calculationService.calculateArpsCumulative(qi, di, b, BigDecimal.valueOf(5)));

        // Calculate remaining life
        int remainingMonths = calculateRemainingLife(qi, di, b, economicLimit);
        dto.setRemainingLifeMonths(remainingMonths);

        // Calculate EUR for all three methods for comparison
        dto.setEurExponentialBbl(calculateEur(qi, di, BigDecimal.ZERO, economicLimit));
        dto.setEurHyperbolicBbl(calculateEur(qi, di, BigDecimal.valueOf(0.5), economicLimit));
        dto.setEurHarmonicBbl(calculateEurHarmonic(qi, di, economicLimit));

        dto.setAnalysisDate(System.currentTimeMillis());
        dto.setUpdatedTime(System.currentTimeMillis());

        saveDeclineAttributes(dto);

        log.info("Decline analysis completed: EUR={} bbl, type={}", eur, dto.getDeclineType());
        return dto;
    }

    /**
     * Generates production forecast for a given number of years.
     */
    public List<Map<String, Object>> generateForecast(UUID assetId, int forecastYears, int monthlyIntervals) {
        RvDeclineAnalysisDto dto = getDeclineAnalysisById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("Decline Analysis", assetId));

        if (dto.getQiBopd() == null || dto.getDiPerYear() == null || dto.getBExponent() == null) {
            throw new IllegalStateException("Decline analysis parameters not set. Run performAnalysis first.");
        }

        List<Map<String, Object>> forecast = new ArrayList<>();
        BigDecimal qi = dto.getQiBopd();
        BigDecimal di = dto.getDiPerYear();
        BigDecimal b = dto.getBExponent();
        BigDecimal economicLimit = dto.getEconomicLimitBopd() != null ?
            dto.getEconomicLimitBopd() : BigDecimal.valueOf(10);

        int totalMonths = forecastYears * 12;
        BigDecimal cumulativeProduction = BigDecimal.ZERO;

        for (int month = 0; month <= totalMonths; month += monthlyIntervals) {
            BigDecimal timeYears = BigDecimal.valueOf(month).divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            BigDecimal rate = calculationService.calculateArpsDecline(qi, di, b, timeYears);

            // Stop if below economic limit
            if (rate.compareTo(economicLimit) < 0) {
                break;
            }

            BigDecimal cumulative = calculationService.calculateArpsCumulative(qi, di, b, timeYears);

            Map<String, Object> point = new HashMap<>();
            point.put("month", month);
            point.put("timeYears", timeYears);
            point.put("rateBopd", rate);
            point.put("cumulativeBbl", cumulative);
            forecast.add(point);
        }

        return forecast;
    }

    /**
     * Updates a Decline Analysis.
     */
    public RvDeclineAnalysisDto updateDeclineAnalysis(RvDeclineAnalysisDto dto) {
        log.info("Updating decline analysis: {}", dto.getAssetId());

        rvAssetService.getAssetById(dto.getAssetId()).ifPresent(asset -> {
            boolean needsUpdate = false;
            if (!asset.getName().equals(dto.getName())) {
                asset.setName(dto.getName());
                needsUpdate = true;
            }
            if (!Objects.equals(asset.getLabel(), dto.getLabel())) {
                asset.setLabel(dto.getLabel());
                needsUpdate = true;
            }
            if (needsUpdate) {
                rvAssetService.updateAsset(asset);
            }
        });

        dto.setUpdatedTime(System.currentTimeMillis());
        saveDeclineAttributes(dto);

        return dto;
    }

    /**
     * Deletes a Decline Analysis.
     */
    public void deleteDeclineAnalysis(UUID tenantId, UUID assetId) {
        log.warn("Deleting decline analysis: {}", assetId);
        rvHierarchyService.deleteAllRelations(tenantId, assetId);
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    private BigDecimal calculateEur(BigDecimal qi, BigDecimal di, BigDecimal b, BigDecimal economicLimit) {
        // Calculate time to economic limit
        BigDecimal timeToLimit = calculateTimeToRate(qi, di, b, economicLimit);

        // Calculate cumulative at that time
        return calculationService.calculateArpsCumulative(qi, di, b, timeToLimit);
    }

    private BigDecimal calculateEurHarmonic(BigDecimal qi, BigDecimal di, BigDecimal economicLimit) {
        // For harmonic decline, EUR approaches infinity
        // Use practical limit of 50 years
        return calculationService.calculateArpsCumulative(qi, di, BigDecimal.ONE, BigDecimal.valueOf(50));
    }

    private BigDecimal calculateTimeToRate(BigDecimal qi, BigDecimal di, BigDecimal b, BigDecimal targetRate) {
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            // Exponential: t = -ln(q/qi) / di
            double ratio = targetRate.doubleValue() / qi.doubleValue();
            double time = -Math.log(ratio) / di.doubleValue();
            return BigDecimal.valueOf(time);
        } else {
            // Hyperbolic/Harmonic: t = ((qi/q)^b - 1) / (b * di)
            double ratio = Math.pow(qi.doubleValue() / targetRate.doubleValue(), b.doubleValue());
            double time = (ratio - 1) / (b.doubleValue() * di.doubleValue());
            return BigDecimal.valueOf(time);
        }
    }

    private int calculateRemainingLife(BigDecimal qi, BigDecimal di, BigDecimal b, BigDecimal economicLimit) {
        BigDecimal timeYears = calculateTimeToRate(qi, di, b, economicLimit);
        return timeYears.multiply(BigDecimal.valueOf(12)).intValue();
    }

    private void saveDeclineAttributes(RvDeclineAnalysisDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getAnalysisCode() != null) attrs.put(RvDeclineAnalysisDto.ATTR_ANALYSIS_CODE, dto.getAnalysisCode());
        if (dto.getAnalysisDate() != null) attrs.put(RvDeclineAnalysisDto.ATTR_ANALYSIS_DATE, dto.getAnalysisDate());
        if (dto.getDataStartDate() != null) attrs.put("data_start_date", dto.getDataStartDate());
        if (dto.getDataEndDate() != null) attrs.put("data_end_date", dto.getDataEndDate());
        if (dto.getDeclineType() != null) attrs.put(RvDeclineAnalysisDto.ATTR_DECLINE_TYPE, dto.getDeclineType());
        if (dto.getDeclinePhase() != null) attrs.put("decline_phase", dto.getDeclinePhase());
        if (dto.getQiBopd() != null) attrs.put(RvDeclineAnalysisDto.ATTR_QI_BOPD, dto.getQiBopd());
        if (dto.getDiPerYear() != null) attrs.put(RvDeclineAnalysisDto.ATTR_DI_PER_YEAR, dto.getDiPerYear());
        if (dto.getDiPerMonth() != null) attrs.put("di_per_month", dto.getDiPerMonth());
        if (dto.getBExponent() != null) attrs.put(RvDeclineAnalysisDto.ATTR_B_EXPONENT, dto.getBExponent());
        if (dto.getDMinPerYear() != null) attrs.put("d_min_per_year", dto.getDMinPerYear());
        if (dto.getEurBbl() != null) attrs.put(RvDeclineAnalysisDto.ATTR_EUR_BBL, dto.getEurBbl());
        if (dto.getRemainingReservesBbl() != null) attrs.put("remaining_reserves_bbl", dto.getRemainingReservesBbl());
        if (dto.getCumulativeProductionBbl() != null) attrs.put("cumulative_production_bbl", dto.getCumulativeProductionBbl());
        if (dto.getEconomicLimitBopd() != null) attrs.put(RvDeclineAnalysisDto.ATTR_ECONOMIC_LIMIT_BOPD, dto.getEconomicLimitBopd());
        if (dto.getRemainingLifeMonths() != null) attrs.put("remaining_life_months", dto.getRemainingLifeMonths());
        if (dto.getRate1YearBopd() != null) attrs.put("rate_1_year_bopd", dto.getRate1YearBopd());
        if (dto.getRate3YearsBopd() != null) attrs.put("rate_3_years_bopd", dto.getRate3YearsBopd());
        if (dto.getRate5YearsBopd() != null) attrs.put("rate_5_years_bopd", dto.getRate5YearsBopd());
        if (dto.getRate10YearsBopd() != null) attrs.put("rate_10_years_bopd", dto.getRate10YearsBopd());
        if (dto.getCumulative1YearBbl() != null) attrs.put("cumulative_1_year_bbl", dto.getCumulative1YearBbl());
        if (dto.getCumulative3YearsBbl() != null) attrs.put("cumulative_3_years_bbl", dto.getCumulative3YearsBbl());
        if (dto.getCumulative5YearsBbl() != null) attrs.put("cumulative_5_years_bbl", dto.getCumulative5YearsBbl());
        if (dto.getR2Coefficient() != null) attrs.put(RvDeclineAnalysisDto.ATTR_R2_COEFFICIENT, dto.getR2Coefficient());
        if (dto.getFitQuality() != null) attrs.put("fit_quality", dto.getFitQuality());
        if (dto.getEurExponentialBbl() != null) attrs.put("eur_exponential_bbl", dto.getEurExponentialBbl());
        if (dto.getEurHyperbolicBbl() != null) attrs.put("eur_hyperbolic_bbl", dto.getEurHyperbolicBbl());
        if (dto.getEurHarmonicBbl() != null) attrs.put("eur_harmonic_bbl", dto.getEurHarmonicBbl());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadDeclineAttributes(RvDeclineAnalysisDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvDeclineAnalysisDto.ATTR_ANALYSIS_CODE -> dto.setAnalysisCode(entry.getValueAsString());
                case RvDeclineAnalysisDto.ATTR_ANALYSIS_DATE -> entry.getLongValue().ifPresent(dto::setAnalysisDate);
                case "data_start_date" -> entry.getLongValue().ifPresent(dto::setDataStartDate);
                case "data_end_date" -> entry.getLongValue().ifPresent(dto::setDataEndDate);
                case RvDeclineAnalysisDto.ATTR_DECLINE_TYPE -> dto.setDeclineType(entry.getValueAsString());
                case "decline_phase" -> dto.setDeclinePhase(entry.getValueAsString());
                case RvDeclineAnalysisDto.ATTR_QI_BOPD -> entry.getDoubleValue().ifPresent(v -> dto.setQiBopd(BigDecimal.valueOf(v)));
                case RvDeclineAnalysisDto.ATTR_DI_PER_YEAR -> entry.getDoubleValue().ifPresent(v -> dto.setDiPerYear(BigDecimal.valueOf(v)));
                case "di_per_month" -> entry.getDoubleValue().ifPresent(v -> dto.setDiPerMonth(BigDecimal.valueOf(v)));
                case RvDeclineAnalysisDto.ATTR_B_EXPONENT -> entry.getDoubleValue().ifPresent(v -> dto.setBExponent(BigDecimal.valueOf(v)));
                case "d_min_per_year" -> entry.getDoubleValue().ifPresent(v -> dto.setDMinPerYear(BigDecimal.valueOf(v)));
                case RvDeclineAnalysisDto.ATTR_EUR_BBL -> entry.getDoubleValue().ifPresent(v -> dto.setEurBbl(BigDecimal.valueOf(v)));
                case "remaining_reserves_bbl" -> entry.getDoubleValue().ifPresent(v -> dto.setRemainingReservesBbl(BigDecimal.valueOf(v)));
                case "cumulative_production_bbl" -> entry.getDoubleValue().ifPresent(v -> dto.setCumulativeProductionBbl(BigDecimal.valueOf(v)));
                case RvDeclineAnalysisDto.ATTR_ECONOMIC_LIMIT_BOPD -> entry.getDoubleValue().ifPresent(v -> dto.setEconomicLimitBopd(BigDecimal.valueOf(v)));
                case "remaining_life_months" -> entry.getLongValue().ifPresent(v -> dto.setRemainingLifeMonths(v.intValue()));
                case "rate_1_year_bopd" -> entry.getDoubleValue().ifPresent(v -> dto.setRate1YearBopd(BigDecimal.valueOf(v)));
                case "rate_3_years_bopd" -> entry.getDoubleValue().ifPresent(v -> dto.setRate3YearsBopd(BigDecimal.valueOf(v)));
                case "rate_5_years_bopd" -> entry.getDoubleValue().ifPresent(v -> dto.setRate5YearsBopd(BigDecimal.valueOf(v)));
                case "rate_10_years_bopd" -> entry.getDoubleValue().ifPresent(v -> dto.setRate10YearsBopd(BigDecimal.valueOf(v)));
                case "cumulative_1_year_bbl" -> entry.getDoubleValue().ifPresent(v -> dto.setCumulative1YearBbl(BigDecimal.valueOf(v)));
                case "cumulative_3_years_bbl" -> entry.getDoubleValue().ifPresent(v -> dto.setCumulative3YearsBbl(BigDecimal.valueOf(v)));
                case "cumulative_5_years_bbl" -> entry.getDoubleValue().ifPresent(v -> dto.setCumulative5YearsBbl(BigDecimal.valueOf(v)));
                case RvDeclineAnalysisDto.ATTR_R2_COEFFICIENT -> entry.getDoubleValue().ifPresent(v -> dto.setR2Coefficient(BigDecimal.valueOf(v)));
                case "fit_quality" -> dto.setFitQuality(entry.getValueAsString());
                case "eur_exponential_bbl" -> entry.getDoubleValue().ifPresent(v -> dto.setEurExponentialBbl(BigDecimal.valueOf(v)));
                case "eur_hyperbolic_bbl" -> entry.getDoubleValue().ifPresent(v -> dto.setEurHyperbolicBbl(BigDecimal.valueOf(v)));
                case "eur_harmonic_bbl" -> entry.getDoubleValue().ifPresent(v -> dto.setEurHarmonicBbl(BigDecimal.valueOf(v)));
            }
        }
    }

    private RvDeclineAnalysisDto mapAssetToDto(Asset asset) {
        RvDeclineAnalysisDto dto = new RvDeclineAnalysisDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadDeclineAttributes(dto);
        return dto;
    }
}
