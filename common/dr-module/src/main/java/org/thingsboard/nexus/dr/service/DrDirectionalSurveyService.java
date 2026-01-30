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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.dr.dto.DrDirectionalSurveyDto;
import org.thingsboard.nexus.dr.model.DrDirectionalSurvey;
import org.thingsboard.nexus.dr.model.enums.SurveyType;
import org.thingsboard.nexus.dr.repository.DrDirectionalSurveyRepository;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Directional Survey operations including trajectory calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrDirectionalSurveyService {

    private final DrDirectionalSurveyRepository surveyRepository;

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal DEG_TO_RAD = BigDecimal.valueOf(Math.PI / 180.0);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // --- CRUD Operations ---

    @Transactional(readOnly = true)
    public DrDirectionalSurveyDto getById(UUID id) {
        DrDirectionalSurvey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Survey not found: " + id));
        return DrDirectionalSurveyDto.fromEntity(survey);
    }

    @Transactional(readOnly = true)
    public Page<DrDirectionalSurveyDto> getByTenant(UUID tenantId, Pageable pageable) {
        return surveyRepository.findByTenantId(tenantId, pageable)
                .map(DrDirectionalSurveyDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<DrDirectionalSurveyDto> getByRun(UUID runId, Pageable pageable) {
        return surveyRepository.findByRunId(runId, pageable)
                .map(DrDirectionalSurveyDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<DrDirectionalSurveyDto> getSurveysByRunOrdered(UUID runId) {
        return surveyRepository.findByRunIdOrderByMdFtAsc(runId).stream()
                .map(DrDirectionalSurveyDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrDirectionalSurveyDto> getDefinitiveSurveysByRun(UUID runId) {
        return surveyRepository.findByRunIdAndIsDefinitiveTrueOrderByMdFtAsc(runId).stream()
                .map(DrDirectionalSurveyDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DrDirectionalSurveyDto> getByFilters(UUID tenantId, UUID runId, UUID wellId,
                                                      SurveyType surveyType, Boolean isDefinitive, Pageable pageable) {
        return surveyRepository.findByFilters(tenantId, runId, wellId, surveyType, isDefinitive, pageable)
                .map(DrDirectionalSurveyDto::fromEntity);
    }

    /**
     * Create a new survey and calculate trajectory values.
     */
    @Transactional
    public DrDirectionalSurveyDto create(DrDirectionalSurvey survey) {
        log.info("Creating new directional survey for run {} at MD {} ft", survey.getRunId(), survey.getMdFt());

        // Check if survey already exists at this depth
        if (surveyRepository.existsByRunIdAndMdFt(survey.getRunId(), survey.getMdFt())) {
            throw new IllegalArgumentException("Survey already exists at MD " + survey.getMdFt() + " ft");
        }

        // Calculate trajectory values
        calculateTrajectoryValues(survey);

        DrDirectionalSurvey saved = surveyRepository.save(survey);
        return DrDirectionalSurveyDto.fromEntity(saved);
    }

    /**
     * Update existing survey and recalculate trajectory.
     */
    @Transactional
    public DrDirectionalSurveyDto update(UUID id, DrDirectionalSurvey surveyUpdate) {
        log.info("Updating directional survey: {}", id);

        DrDirectionalSurvey existing = surveyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Survey not found: " + id));

        // Update fields
        existing.setMdFt(surveyUpdate.getMdFt());
        existing.setInclinationDeg(surveyUpdate.getInclinationDeg());
        existing.setAzimuthDeg(surveyUpdate.getAzimuthDeg());
        existing.setToolfaceDeg(surveyUpdate.getToolfaceDeg());
        existing.setSurveyType(surveyUpdate.getSurveyType());
        existing.setIsDefinitive(surveyUpdate.getIsDefinitive());
        existing.setSurveyQuality(surveyUpdate.getSurveyQuality());
        existing.setSurveyTime(surveyUpdate.getSurveyTime());
        existing.setMagneticFieldStrength(surveyUpdate.getMagneticFieldStrength());
        existing.setMagneticDipAngleDeg(surveyUpdate.getMagneticDipAngleDeg());
        existing.setGravityFieldStrength(surveyUpdate.getGravityFieldStrength());
        existing.setBoreholeTempF(surveyUpdate.getBoreholeTempF());
        existing.setSagCorrectionApplied(surveyUpdate.getSagCorrectionApplied());
        existing.setMagneticCorrectionApplied(surveyUpdate.getMagneticCorrectionApplied());
        existing.setNorthUncertaintyFt(surveyUpdate.getNorthUncertaintyFt());
        existing.setEastUncertaintyFt(surveyUpdate.getEastUncertaintyFt());
        existing.setTvdUncertaintyFt(surveyUpdate.getTvdUncertaintyFt());
        existing.setRawData(surveyUpdate.getRawData());
        existing.setNotes(surveyUpdate.getNotes());

        // Recalculate trajectory values
        calculateTrajectoryValues(existing);

        DrDirectionalSurvey saved = surveyRepository.save(existing);
        return DrDirectionalSurveyDto.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting directional survey: {}", id);
        if (!surveyRepository.existsById(id)) {
            throw new EntityNotFoundException("Survey not found: " + id);
        }
        surveyRepository.deleteById(id);
    }

    /**
     * Mark survey as definitive.
     */
    @Transactional
    public DrDirectionalSurveyDto markAsDefinitive(UUID id) {
        log.info("Marking survey as definitive: {}", id);

        DrDirectionalSurvey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Survey not found: " + id));

        survey.setIsDefinitive(true);
        DrDirectionalSurvey saved = surveyRepository.save(survey);
        return DrDirectionalSurveyDto.fromEntity(saved);
    }

    // --- Trajectory Calculations ---

    /**
     * Calculate trajectory values using Minimum Curvature Method.
     */
    private void calculateTrajectoryValues(DrDirectionalSurvey survey) {
        // Get the previous survey for the run
        List<DrDirectionalSurvey> previousSurveys = surveyRepository.findSurveyBeforeDepth(
                survey.getRunId(), survey.getMdFt(), PageRequest.of(0, 1));

        if (previousSurveys.isEmpty()) {
            // This is the first survey (kickoff point or surface)
            calculateSurfaceSurvey(survey);
        } else {
            DrDirectionalSurvey prevSurvey = previousSurveys.get(0);
            calculateMinimumCurvature(prevSurvey, survey);
        }

        // Calculate closure
        calculateClosure(survey);
    }

    /**
     * Calculate values for surface/first survey.
     */
    private void calculateSurfaceSurvey(DrDirectionalSurvey survey) {
        // At surface, TVD = MD (assuming vertical start)
        if (survey.getInclinationDeg() == null || survey.getInclinationDeg().compareTo(BigDecimal.ZERO) == 0) {
            survey.setTvdFt(survey.getMdFt());
            survey.setNorthFt(BigDecimal.ZERO);
            survey.setEastFt(BigDecimal.ZERO);
            survey.setVerticalSectionFt(BigDecimal.ZERO);
            survey.setDlsDegPer100ft(BigDecimal.ZERO);
        } else {
            // First survey with inclination
            double mdFt = survey.getMdFt().doubleValue();
            double incRad = survey.getInclinationDeg().multiply(DEG_TO_RAD).doubleValue();
            double azRad = survey.getAzimuthDeg() != null ?
                    survey.getAzimuthDeg().multiply(DEG_TO_RAD).doubleValue() : 0;

            double tvd = mdFt * Math.cos(incRad);
            double hd = mdFt * Math.sin(incRad);
            double north = hd * Math.cos(azRad);
            double east = hd * Math.sin(azRad);

            survey.setTvdFt(BigDecimal.valueOf(tvd).setScale(2, RoundingMode.HALF_UP));
            survey.setNorthFt(BigDecimal.valueOf(north).setScale(2, RoundingMode.HALF_UP));
            survey.setEastFt(BigDecimal.valueOf(east).setScale(2, RoundingMode.HALF_UP));
            survey.setDlsDegPer100ft(BigDecimal.ZERO);
        }
    }

    /**
     * Calculate trajectory using Minimum Curvature Method.
     * This is the industry-standard method for wellbore trajectory calculation.
     */
    private void calculateMinimumCurvature(DrDirectionalSurvey prev, DrDirectionalSurvey curr) {
        // Get values from previous survey
        double md1 = prev.getMdFt().doubleValue();
        double inc1 = prev.getInclinationDeg() != null ? prev.getInclinationDeg().doubleValue() : 0;
        double azi1 = prev.getAzimuthDeg() != null ? prev.getAzimuthDeg().doubleValue() : 0;
        double tvd1 = prev.getTvdFt() != null ? prev.getTvdFt().doubleValue() : md1;
        double north1 = prev.getNorthFt() != null ? prev.getNorthFt().doubleValue() : 0;
        double east1 = prev.getEastFt() != null ? prev.getEastFt().doubleValue() : 0;

        // Get values from current survey
        double md2 = curr.getMdFt().doubleValue();
        double inc2 = curr.getInclinationDeg() != null ? curr.getInclinationDeg().doubleValue() : 0;
        double azi2 = curr.getAzimuthDeg() != null ? curr.getAzimuthDeg().doubleValue() : 0;

        // Convert to radians
        double inc1Rad = Math.toRadians(inc1);
        double inc2Rad = Math.toRadians(inc2);
        double azi1Rad = Math.toRadians(azi1);
        double azi2Rad = Math.toRadians(azi2);

        // Course length (MD difference)
        double courseLength = md2 - md1;

        // Calculate dogleg (angle change)
        double cosD = Math.cos(inc2Rad - inc1Rad) -
                Math.sin(inc1Rad) * Math.sin(inc2Rad) * (1 - Math.cos(azi2Rad - azi1Rad));
        double dogleg = Math.acos(Math.max(-1, Math.min(1, cosD)));

        // Calculate Ratio Factor (RF)
        double rf;
        if (dogleg < 0.0001) {
            // When dogleg is very small, RF approaches 1
            rf = 1.0;
        } else {
            rf = 2.0 / dogleg * Math.tan(dogleg / 2.0);
        }

        // Calculate deltas
        double deltaTvd = courseLength / 2.0 * (Math.cos(inc1Rad) + Math.cos(inc2Rad)) * rf;
        double deltaNorth = courseLength / 2.0 *
                (Math.sin(inc1Rad) * Math.cos(azi1Rad) + Math.sin(inc2Rad) * Math.cos(azi2Rad)) * rf;
        double deltaEast = courseLength / 2.0 *
                (Math.sin(inc1Rad) * Math.sin(azi1Rad) + Math.sin(inc2Rad) * Math.sin(azi2Rad)) * rf;

        // Calculate new positions
        double tvd2 = tvd1 + deltaTvd;
        double north2 = north1 + deltaNorth;
        double east2 = east1 + deltaEast;

        // Calculate DLS (degrees per 100 feet)
        double dls = 0;
        if (courseLength > 0) {
            dls = Math.toDegrees(dogleg) * 100.0 / courseLength;
        }

        // Set calculated values
        curr.setTvdFt(BigDecimal.valueOf(tvd2).setScale(2, RoundingMode.HALF_UP));
        curr.setNorthFt(BigDecimal.valueOf(north2).setScale(2, RoundingMode.HALF_UP));
        curr.setEastFt(BigDecimal.valueOf(east2).setScale(2, RoundingMode.HALF_UP));
        curr.setDlsDegPer100ft(BigDecimal.valueOf(dls).setScale(3, RoundingMode.HALF_UP));
    }

    /**
     * Calculate closure distance and azimuth.
     */
    private void calculateClosure(DrDirectionalSurvey survey) {
        double north = survey.getNorthFt() != null ? survey.getNorthFt().doubleValue() : 0;
        double east = survey.getEastFt() != null ? survey.getEastFt().doubleValue() : 0;

        // Closure distance (horizontal displacement from surface)
        double closureDistance = Math.sqrt(north * north + east * east);
        survey.setClosureDistanceFt(BigDecimal.valueOf(closureDistance).setScale(2, RoundingMode.HALF_UP));

        // Closure azimuth (direction from surface to current position)
        if (closureDistance > 0.001) {
            double closureAzimuth = Math.toDegrees(Math.atan2(east, north));
            if (closureAzimuth < 0) {
                closureAzimuth += 360;
            }
            survey.setClosureAzimuthDeg(BigDecimal.valueOf(closureAzimuth).setScale(3, RoundingMode.HALF_UP));
        } else {
            survey.setClosureAzimuthDeg(BigDecimal.ZERO);
        }

        // Vertical Section (projection onto a specific azimuth plane)
        // Using closure azimuth as the VS azimuth for now
        survey.setVerticalSectionFt(survey.getClosureDistanceFt());
    }

    /**
     * Recalculate all surveys for a run from a given depth.
     */
    @Transactional
    public void recalculateTrajectoryFromDepth(UUID runId, BigDecimal fromDepth) {
        log.info("Recalculating trajectory for run {} from depth {} ft", runId, fromDepth);

        List<DrDirectionalSurvey> surveys = surveyRepository.findByRunIdOrderByMdFtAsc(runId);

        boolean recalculate = false;
        DrDirectionalSurvey previous = null;

        for (DrDirectionalSurvey survey : surveys) {
            if (survey.getMdFt().compareTo(fromDepth) >= 0) {
                recalculate = true;
            }

            if (recalculate) {
                if (previous == null) {
                    calculateSurfaceSurvey(survey);
                } else {
                    calculateMinimumCurvature(previous, survey);
                }
                calculateClosure(survey);
                surveyRepository.save(survey);
            }

            previous = survey;
        }
    }

    /**
     * Recalculate entire trajectory for a run.
     */
    @Transactional
    public void recalculateEntireTrajectory(UUID runId) {
        recalculateTrajectoryFromDepth(runId, BigDecimal.ZERO);
    }

    // --- Analysis Methods ---

    /**
     * Get surveys with high DLS (potential problem areas).
     */
    @Transactional(readOnly = true)
    public List<DrDirectionalSurveyDto> getHighDlsSurveys(UUID runId, BigDecimal threshold) {
        return surveyRepository.findHighDlsSurveys(runId, threshold).stream()
                .map(DrDirectionalSurveyDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get maximum DLS for a run.
     */
    @Transactional(readOnly = true)
    public BigDecimal getMaxDls(UUID runId) {
        return surveyRepository.getMaxDlsByRunId(runId);
    }

    /**
     * Find horizontal sections (inclination >= threshold).
     */
    @Transactional(readOnly = true)
    public List<DrDirectionalSurveyDto> getHorizontalSections(UUID runId, BigDecimal inclinationThreshold) {
        return surveyRepository.findHorizontalSections(runId, inclinationThreshold).stream()
                .map(DrDirectionalSurveyDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get the deepest survey for a run.
     */
    @Transactional(readOnly = true)
    public DrDirectionalSurveyDto getDeepestSurvey(UUID runId) {
        List<DrDirectionalSurvey> surveys = surveyRepository.findDeepestSurveyByRunId(runId, PageRequest.of(0, 1));
        if (surveys.isEmpty()) {
            return null;
        }
        return DrDirectionalSurveyDto.fromEntity(surveys.get(0));
    }

    /**
     * Get the latest survey for a run.
     */
    @Transactional(readOnly = true)
    public DrDirectionalSurveyDto getLatestSurvey(UUID runId) {
        List<DrDirectionalSurvey> surveys = surveyRepository.findLatestSurveyByRunId(runId, PageRequest.of(0, 1));
        if (surveys.isEmpty()) {
            return null;
        }
        return DrDirectionalSurveyDto.fromEntity(surveys.get(0));
    }

    /**
     * Interpolate position at a given depth.
     */
    @Transactional(readOnly = true)
    public DrDirectionalSurveyDto interpolateAtDepth(UUID runId, BigDecimal targetDepth) {
        List<DrDirectionalSurvey> before = surveyRepository.findSurveyBeforeDepth(runId, targetDepth, PageRequest.of(0, 1));
        List<DrDirectionalSurvey> after = surveyRepository.findSurveyAfterDepth(runId, targetDepth, PageRequest.of(0, 1));

        if (before.isEmpty() || after.isEmpty()) {
            // Can't interpolate - return closest survey
            List<DrDirectionalSurvey> closest = surveyRepository.findClosestSurveyToDepth(runId, targetDepth, PageRequest.of(0, 1));
            return closest.isEmpty() ? null : DrDirectionalSurveyDto.fromEntity(closest.get(0));
        }

        DrDirectionalSurvey surveyBefore = before.get(0);
        DrDirectionalSurvey surveyAfter = after.get(0);

        // Linear interpolation factor
        double md1 = surveyBefore.getMdFt().doubleValue();
        double md2 = surveyAfter.getMdFt().doubleValue();
        double factor = (targetDepth.doubleValue() - md1) / (md2 - md1);

        // Interpolate all values
        DrDirectionalSurveyDto interpolated = new DrDirectionalSurveyDto();
        interpolated.setRunId(runId);
        interpolated.setWellId(surveyBefore.getWellId());
        interpolated.setTenantId(surveyBefore.getTenantId());
        interpolated.setMdFt(targetDepth);

        interpolated.setInclinationDeg(interpolateValue(surveyBefore.getInclinationDeg(), surveyAfter.getInclinationDeg(), factor));
        interpolated.setAzimuthDeg(interpolateAzimuth(surveyBefore.getAzimuthDeg(), surveyAfter.getAzimuthDeg(), factor));
        interpolated.setTvdFt(interpolateValue(surveyBefore.getTvdFt(), surveyAfter.getTvdFt(), factor));
        interpolated.setNorthFt(interpolateValue(surveyBefore.getNorthFt(), surveyAfter.getNorthFt(), factor));
        interpolated.setEastFt(interpolateValue(surveyBefore.getEastFt(), surveyAfter.getEastFt(), factor));

        return interpolated;
    }

    private BigDecimal interpolateValue(BigDecimal v1, BigDecimal v2, double factor) {
        if (v1 == null || v2 == null) return null;
        double result = v1.doubleValue() + (v2.doubleValue() - v1.doubleValue()) * factor;
        return BigDecimal.valueOf(result).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal interpolateAzimuth(BigDecimal a1, BigDecimal a2, double factor) {
        if (a1 == null || a2 == null) return null;
        double az1 = a1.doubleValue();
        double az2 = a2.doubleValue();

        // Handle wrap-around at 360 degrees
        double diff = az2 - az1;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;

        double result = az1 + diff * factor;
        if (result < 0) result += 360;
        if (result >= 360) result -= 360;

        return BigDecimal.valueOf(result).setScale(3, RoundingMode.HALF_UP);
    }

    // --- Statistics ---

    @Transactional(readOnly = true)
    public long countByRun(UUID runId) {
        return surveyRepository.countByRunId(runId);
    }

    @Transactional(readOnly = true)
    public long countDefinitiveByRun(UUID runId) {
        return surveyRepository.countByRunIdAndIsDefinitiveTrue(runId);
    }
}
