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
import org.thingsboard.nexus.dr.model.DrMudLog;
import org.thingsboard.nexus.dr.model.enums.LithologyType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Mud Log operations.
 */
@Repository
public interface DrMudLogRepository extends JpaRepository<DrMudLog, UUID> {

    // --- Basic Queries ---

    List<DrMudLog> findByTenantId(UUID tenantId);

    Page<DrMudLog> findByTenantId(UUID tenantId, Pageable pageable);

    List<DrMudLog> findByRunId(UUID runId);

    Page<DrMudLog> findByRunId(UUID runId, Pageable pageable);

    List<DrMudLog> findByWellId(UUID wellId);

    Page<DrMudLog> findByWellId(UUID wellId, Pageable pageable);

    /**
     * Find all mud logs for a run ordered by measured depth.
     */
    List<DrMudLog> findByRunIdOrderByMdFtAsc(UUID runId);

    /**
     * Find all mud logs for a well ordered by measured depth.
     */
    List<DrMudLog> findByWellIdOrderByMdFtAsc(UUID wellId);

    // --- Depth-based Queries ---

    /**
     * Find mud log at specific measured depth.
     */
    Optional<DrMudLog> findByRunIdAndMdFt(UUID runId, BigDecimal mdFt);

    /**
     * Find mud logs within a depth range.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.mdFt >= :minDepth AND m.mdFt <= :maxDepth ORDER BY m.mdFt ASC")
    List<DrMudLog> findByRunIdAndDepthRange(
            @Param("runId") UUID runId,
            @Param("minDepth") BigDecimal minDepth,
            @Param("maxDepth") BigDecimal maxDepth);

    /**
     * Find the deepest mud log for a run.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId ORDER BY m.mdFt DESC")
    List<DrMudLog> findDeepestMudLogByRunId(@Param("runId") UUID runId, Pageable pageable);

    // --- Lithology Queries ---

    /**
     * Find mud logs by primary lithology.
     */
    List<DrMudLog> findByRunIdAndPrimaryLithology(UUID runId, LithologyType lithology);

    /**
     * Find mud logs containing specific lithology (primary or secondary).
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND (m.primaryLithology = :lithology OR m.secondaryLithology = :lithology) " +
            "ORDER BY m.mdFt ASC")
    List<DrMudLog> findByRunIdAndLithology(
            @Param("runId") UUID runId,
            @Param("lithology") LithologyType lithology);

    /**
     * Find distinct lithologies for a run.
     */
    @Query("SELECT DISTINCT m.primaryLithology FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.primaryLithology IS NOT NULL")
    List<LithologyType> findDistinctLithologiesByRunId(@Param("runId") UUID runId);

    // --- Formation Queries ---

    /**
     * Find mud logs by formation name.
     */
    List<DrMudLog> findByRunIdAndFormationName(UUID runId, String formationName);

    /**
     * Find distinct formations for a run.
     */
    @Query("SELECT DISTINCT m.formationName FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.formationName IS NOT NULL ORDER BY m.formationName")
    List<String> findDistinctFormationsByRunId(@Param("runId") UUID runId);

    /**
     * Find formation tops (first occurrence of each formation).
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.formationTopFt IS NOT NULL ORDER BY m.mdFt ASC")
    List<DrMudLog> findFormationTopsByRunId(@Param("runId") UUID runId);

    // --- Gas Data Queries ---

    /**
     * Find mud logs with total gas above threshold (gas shows).
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.totalGasUnits > :threshold ORDER BY m.mdFt ASC")
    List<DrMudLog> findGasShowsByRunId(
            @Param("runId") UUID runId,
            @Param("threshold") BigDecimal threshold);

    /**
     * Get maximum total gas for a run.
     */
    @Query("SELECT MAX(m.totalGasUnits) FROM DrMudLog m WHERE m.runId = :runId")
    BigDecimal getMaxTotalGasByRunId(@Param("runId") UUID runId);

    /**
     * Get average background gas for a run.
     */
    @Query("SELECT AVG(m.backgroundGasUnits) FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.backgroundGasUnits IS NOT NULL")
    BigDecimal getAverageBackgroundGasByRunId(@Param("runId") UUID runId);

    /**
     * Find connection gas events.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.connectionGasUnits > :threshold ORDER BY m.mdFt ASC")
    List<DrMudLog> findConnectionGasEventsByRunId(
            @Param("runId") UUID runId,
            @Param("threshold") BigDecimal threshold);

    // --- Oil Show Queries ---

    /**
     * Find mud logs with oil shows.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.oilShowIntensity IS NOT NULL AND m.oilShowIntensity <> 'None' " +
            "ORDER BY m.mdFt ASC")
    List<DrMudLog> findOilShowsByRunId(@Param("runId") UUID runId);

    /**
     * Find mud logs with fluorescence.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.fluorescencePercent > 0 ORDER BY m.mdFt ASC")
    List<DrMudLog> findFluorescenceShowsByRunId(@Param("runId") UUID runId);

    /**
     * Find mud logs by oil show intensity.
     */
    List<DrMudLog> findByRunIdAndOilShowIntensity(UUID runId, String intensity);

    // --- Filter Queries ---

    /**
     * Find mud logs with filters.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.tenantId = :tenantId " +
            "AND (:runId IS NULL OR m.runId = :runId) " +
            "AND (:wellId IS NULL OR m.wellId = :wellId) " +
            "AND (:lithology IS NULL OR m.primaryLithology = :lithology) " +
            "AND (:formationName IS NULL OR m.formationName = :formationName)")
    Page<DrMudLog> findByFilters(
            @Param("tenantId") UUID tenantId,
            @Param("runId") UUID runId,
            @Param("wellId") UUID wellId,
            @Param("lithology") LithologyType lithology,
            @Param("formationName") String formationName,
            Pageable pageable);

    // --- Drilling Parameters Queries ---

    /**
     * Find mud logs with ROP above threshold.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.ropFtHr > :threshold ORDER BY m.mdFt ASC")
    List<DrMudLog> findHighRopZones(
            @Param("runId") UUID runId,
            @Param("threshold") BigDecimal threshold);

    /**
     * Get average ROP for a depth range.
     */
    @Query("SELECT AVG(m.ropFtHr) FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.mdFt >= :minDepth AND m.mdFt <= :maxDepth AND m.ropFtHr IS NOT NULL")
    BigDecimal getAverageRopForDepthRange(
            @Param("runId") UUID runId,
            @Param("minDepth") BigDecimal minDepth,
            @Param("maxDepth") BigDecimal maxDepth);

    // --- Sample Queries ---

    /**
     * Find mud logs by sample type.
     */
    List<DrMudLog> findByRunIdAndSampleType(UUID runId, String sampleType);

    /**
     * Find mud log by sample number.
     */
    Optional<DrMudLog> findByRunIdAndSampleNumber(UUID runId, String sampleNumber);

    // --- Time-based Queries ---

    /**
     * Find mud logs in a time range.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.sampleTime >= :startTime AND m.sampleTime <= :endTime ORDER BY m.sampleTime ASC")
    List<DrMudLog> findByRunIdAndTimeRange(
            @Param("runId") UUID runId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    /**
     * Get latest mud log for a run.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId ORDER BY m.sampleTime DESC")
    List<DrMudLog> findLatestMudLogByRunId(@Param("runId") UUID runId, Pageable pageable);

    // --- Porosity Queries ---

    /**
     * Find porous zones.
     */
    @Query("SELECT m FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.porosityPercent > :threshold ORDER BY m.mdFt ASC")
    List<DrMudLog> findPorousZones(
            @Param("runId") UUID runId,
            @Param("threshold") BigDecimal threshold);

    // --- Statistics ---

    /**
     * Count mud logs by run.
     */
    long countByRunId(UUID runId);

    /**
     * Count mud logs by well.
     */
    long countByWellId(UUID wellId);

    /**
     * Check if mud log exists at depth for run.
     */
    boolean existsByRunIdAndMdFt(UUID runId, BigDecimal mdFt);

    /**
     * Count mud logs by lithology type for a run.
     */
    @Query("SELECT m.primaryLithology, COUNT(m) FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.primaryLithology IS NOT NULL GROUP BY m.primaryLithology")
    List<Object[]> countByLithologyForRunId(@Param("runId") UUID runId);

    // --- Geologist Queries ---

    /**
     * Find mud logs by geologist.
     */
    List<DrMudLog> findByRunIdAndLoggedBy(UUID runId, String loggedBy);

    /**
     * Find distinct geologists for a run.
     */
    @Query("SELECT DISTINCT m.loggedBy FROM DrMudLog m WHERE m.runId = :runId " +
            "AND m.loggedBy IS NOT NULL")
    List<String> findDistinctGeologistsByRunId(@Param("runId") UUID runId);
}
