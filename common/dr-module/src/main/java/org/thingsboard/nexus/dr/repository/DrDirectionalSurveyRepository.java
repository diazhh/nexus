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
package org.thingsboard.nexus.dr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.dr.model.DrDirectionalSurvey;
import org.thingsboard.nexus.dr.model.enums.SurveyType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Directional Survey operations.
 */
@Repository
public interface DrDirectionalSurveyRepository extends JpaRepository<DrDirectionalSurvey, UUID> {

    // --- Basic Queries ---

    List<DrDirectionalSurvey> findByTenantId(UUID tenantId);

    Page<DrDirectionalSurvey> findByTenantId(UUID tenantId, Pageable pageable);

    List<DrDirectionalSurvey> findByRunId(UUID runId);

    Page<DrDirectionalSurvey> findByRunId(UUID runId, Pageable pageable);

    List<DrDirectionalSurvey> findByWellId(UUID wellId);

    Page<DrDirectionalSurvey> findByWellId(UUID wellId, Pageable pageable);

    /**
     * Find all surveys for a run ordered by measured depth.
     */
    List<DrDirectionalSurvey> findByRunIdOrderByMdFtAsc(UUID runId);

    /**
     * Find all surveys for a well ordered by measured depth.
     */
    List<DrDirectionalSurvey> findByWellIdOrderByMdFtAsc(UUID wellId);

    // --- Survey Type Queries ---

    List<DrDirectionalSurvey> findByRunIdAndSurveyType(UUID runId, SurveyType surveyType);

    /**
     * Find definitive surveys only for a run.
     */
    List<DrDirectionalSurvey> findByRunIdAndIsDefinitiveTrueOrderByMdFtAsc(UUID runId);

    /**
     * Find definitive surveys only for a well.
     */
    List<DrDirectionalSurvey> findByWellIdAndIsDefinitiveTrueOrderByMdFtAsc(UUID wellId);

    // --- Depth-based Queries ---

    /**
     * Find survey at specific measured depth.
     */
    Optional<DrDirectionalSurvey> findByRunIdAndMdFt(UUID runId, BigDecimal mdFt);

    /**
     * Find surveys within a depth range.
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId " +
            "AND s.mdFt >= :minDepth AND s.mdFt <= :maxDepth ORDER BY s.mdFt ASC")
    List<DrDirectionalSurvey> findByRunIdAndDepthRange(
            @Param("runId") UUID runId,
            @Param("minDepth") BigDecimal minDepth,
            @Param("maxDepth") BigDecimal maxDepth);

    /**
     * Find the deepest survey for a run.
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId ORDER BY s.mdFt DESC")
    List<DrDirectionalSurvey> findDeepestSurveyByRunId(@Param("runId") UUID runId, Pageable pageable);

    /**
     * Find the deepest survey for a well.
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.wellId = :wellId ORDER BY s.mdFt DESC")
    List<DrDirectionalSurvey> findDeepestSurveyByWellId(@Param("wellId") UUID wellId, Pageable pageable);

    /**
     * Find survey closest to a given depth.
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId " +
            "ORDER BY ABS(s.mdFt - :targetDepth) ASC")
    List<DrDirectionalSurvey> findClosestSurveyToDepth(
            @Param("runId") UUID runId,
            @Param("targetDepth") BigDecimal targetDepth,
            Pageable pageable);

    // --- Filter Queries ---

    /**
     * Find surveys with filters.
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.tenantId = :tenantId " +
            "AND (:runId IS NULL OR s.runId = :runId) " +
            "AND (:wellId IS NULL OR s.wellId = :wellId) " +
            "AND (:surveyType IS NULL OR s.surveyType = :surveyType) " +
            "AND (:isDefinitive IS NULL OR s.isDefinitive = :isDefinitive)")
    Page<DrDirectionalSurvey> findByFilters(
            @Param("tenantId") UUID tenantId,
            @Param("runId") UUID runId,
            @Param("wellId") UUID wellId,
            @Param("surveyType") SurveyType surveyType,
            @Param("isDefinitive") Boolean isDefinitive,
            Pageable pageable);

    // --- Trajectory Analysis Queries ---

    /**
     * Find surveys with high dogleg severity.
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId " +
            "AND s.dlsDegPer100ft > :threshold ORDER BY s.dlsDegPer100ft DESC")
    List<DrDirectionalSurvey> findHighDlsSurveys(
            @Param("runId") UUID runId,
            @Param("threshold") BigDecimal threshold);

    /**
     * Get maximum DLS for a run.
     */
    @Query("SELECT MAX(s.dlsDegPer100ft) FROM DrDirectionalSurvey s WHERE s.runId = :runId")
    BigDecimal getMaxDlsByRunId(@Param("runId") UUID runId);

    /**
     * Get average inclination for a run.
     */
    @Query("SELECT AVG(s.inclinationDeg) FROM DrDirectionalSurvey s WHERE s.runId = :runId")
    BigDecimal getAverageInclinationByRunId(@Param("runId") UUID runId);

    /**
     * Find surveys where wellbore is horizontal (inclination > threshold).
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId " +
            "AND s.inclinationDeg >= :threshold ORDER BY s.mdFt ASC")
    List<DrDirectionalSurvey> findHorizontalSections(
            @Param("runId") UUID runId,
            @Param("threshold") BigDecimal threshold);

    // --- Time-based Queries ---

    /**
     * Find surveys in a time range.
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId " +
            "AND s.surveyTime >= :startTime AND s.surveyTime <= :endTime ORDER BY s.surveyTime ASC")
    List<DrDirectionalSurvey> findByRunIdAndTimeRange(
            @Param("runId") UUID runId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    /**
     * Get latest survey for a run.
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId ORDER BY s.surveyTime DESC")
    List<DrDirectionalSurvey> findLatestSurveyByRunId(@Param("runId") UUID runId, Pageable pageable);

    // --- Statistics ---

    /**
     * Count surveys by run.
     */
    long countByRunId(UUID runId);

    /**
     * Count surveys by well.
     */
    long countByWellId(UUID wellId);

    /**
     * Count definitive surveys by run.
     */
    long countByRunIdAndIsDefinitiveTrue(UUID runId);

    /**
     * Check if survey exists at depth for run.
     */
    boolean existsByRunIdAndMdFt(UUID runId, BigDecimal mdFt);

    // --- Interpolation Helper Queries ---

    /**
     * Find the two surveys bracketing a given depth (for interpolation).
     */
    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId " +
            "AND s.mdFt <= :targetDepth ORDER BY s.mdFt DESC")
    List<DrDirectionalSurvey> findSurveyBeforeDepth(
            @Param("runId") UUID runId,
            @Param("targetDepth") BigDecimal targetDepth,
            Pageable pageable);

    @Query("SELECT s FROM DrDirectionalSurvey s WHERE s.runId = :runId " +
            "AND s.mdFt >= :targetDepth ORDER BY s.mdFt ASC")
    List<DrDirectionalSurvey> findSurveyAfterDepth(
            @Param("runId") UUID runId,
            @Param("targetDepth") BigDecimal targetDepth,
            Pageable pageable);

    // --- Quality Queries ---

    /**
     * Find surveys by quality.
     */
    List<DrDirectionalSurvey> findByRunIdAndSurveyQuality(UUID runId, String surveyQuality);

    /**
     * Find surveys with magnetic corrections applied.
     */
    List<DrDirectionalSurvey> findByRunIdAndMagneticCorrectionAppliedTrue(UUID runId);

    /**
     * Find surveys with sag corrections applied.
     */
    List<DrDirectionalSurvey> findByRunIdAndSagCorrectionAppliedTrue(UUID runId);
}
