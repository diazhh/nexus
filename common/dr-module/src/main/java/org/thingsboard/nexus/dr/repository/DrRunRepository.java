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
import org.thingsboard.nexus.dr.model.DrRun;
import org.thingsboard.nexus.dr.model.enums.HoleSection;
import org.thingsboard.nexus.dr.model.enums.RunStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Drilling Run entities
 */
@Repository
public interface DrRunRepository extends JpaRepository<DrRun, UUID> {

    /**
     * Find run by tenant and run number
     */
    Optional<DrRun> findByTenantIdAndRunNumber(UUID tenantId, String runNumber);

    /**
     * Find all runs for a tenant
     */
    Page<DrRun> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find all runs for a tenant (list)
     */
    List<DrRun> findByTenantId(UUID tenantId);

    /**
     * Find runs by rig
     */
    List<DrRun> findByRigId(UUID rigId);

    /**
     * Find runs by rig (pageable)
     */
    Page<DrRun> findByRigId(UUID rigId, Pageable pageable);

    /**
     * Find runs by well
     */
    List<DrRun> findByWellId(UUID wellId);

    /**
     * Find runs by well (pageable)
     */
    Page<DrRun> findByWellId(UUID wellId, Pageable pageable);

    /**
     * Find runs by BHA
     */
    List<DrRun> findByBhaId(UUID bhaId);

    /**
     * Find runs by status
     */
    List<DrRun> findByTenantIdAndStatus(UUID tenantId, RunStatus status);

    /**
     * Find active runs (IN_PROGRESS status)
     */
    @Query("SELECT r FROM DrRun r WHERE r.tenantId = :tenantId AND r.status = 'IN_PROGRESS'")
    List<DrRun> findActiveRuns(@Param("tenantId") UUID tenantId);

    /**
     * Find current run for a rig
     */
    @Query("SELECT r FROM DrRun r WHERE r.rigId = :rigId AND r.status = 'IN_PROGRESS'")
    Optional<DrRun> findCurrentRunByRig(@Param("rigId") UUID rigId);

    /**
     * Find current run for a well
     */
    @Query("SELECT r FROM DrRun r WHERE r.wellId = :wellId AND r.status = 'IN_PROGRESS'")
    Optional<DrRun> findCurrentRunByWell(@Param("wellId") UUID wellId);

    /**
     * Find runs by well and hole section
     */
    List<DrRun> findByWellIdAndHoleSection(UUID wellId, HoleSection holeSection);

    /**
     * Find runs with filters
     */
    @Query("SELECT r FROM DrRun r WHERE r.tenantId = :tenantId " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:rigId IS NULL OR r.rigId = :rigId) " +
           "AND (:wellId IS NULL OR r.wellId = :wellId) " +
           "AND (:holeSection IS NULL OR r.holeSection = :holeSection)")
    Page<DrRun> findByFilters(
        @Param("tenantId") UUID tenantId,
        @Param("status") RunStatus status,
        @Param("rigId") UUID rigId,
        @Param("wellId") UUID wellId,
        @Param("holeSection") HoleSection holeSection,
        Pageable pageable
    );

    /**
     * Find runs within date range
     */
    @Query("SELECT r FROM DrRun r WHERE r.tenantId = :tenantId " +
           "AND r.startDate >= :startDate AND r.startDate <= :endDate")
    List<DrRun> findByDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") Long startDate,
        @Param("endDate") Long endDate
    );

    /**
     * Find runs by rig within date range
     */
    @Query("SELECT r FROM DrRun r WHERE r.rigId = :rigId " +
           "AND r.startDate >= :startDate AND r.startDate <= :endDate")
    List<DrRun> findByRigAndDateRange(
        @Param("rigId") UUID rigId,
        @Param("startDate") Long startDate,
        @Param("endDate") Long endDate
    );

    /**
     * Count runs by tenant and status
     */
    @Query("SELECT COUNT(r) FROM DrRun r WHERE r.tenantId = :tenantId AND r.status = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId,
        @Param("status") RunStatus status
    );

    /**
     * Count runs by rig
     */
    long countByRigId(UUID rigId);

    /**
     * Count runs by well
     */
    long countByWellId(UUID wellId);

    /**
     * Check if run number exists
     */
    boolean existsByTenantIdAndRunNumber(UUID tenantId, String runNumber);

    /**
     * Check if run number exists for another run
     */
    @Query("SELECT COUNT(r) > 0 FROM DrRun r WHERE r.tenantId = :tenantId AND r.runNumber = :runNumber AND r.id != :excludeId")
    boolean existsByTenantIdAndRunNumberAndIdNot(
        @Param("tenantId") UUID tenantId,
        @Param("runNumber") String runNumber,
        @Param("excludeId") UUID excludeId
    );

    /**
     * Get total footage drilled by rig
     */
    @Query("SELECT COALESCE(SUM(r.totalFootageFt), 0) FROM DrRun r WHERE r.rigId = :rigId AND r.status = 'COMPLETED'")
    java.math.BigDecimal getTotalFootageByRig(@Param("rigId") UUID rigId);

    /**
     * Get total footage drilled by well
     */
    @Query("SELECT COALESCE(SUM(r.totalFootageFt), 0) FROM DrRun r WHERE r.wellId = :wellId AND r.status = 'COMPLETED'")
    java.math.BigDecimal getTotalFootageByWell(@Param("wellId") UUID wellId);

    /**
     * Get average ROP by rig
     */
    @Query("SELECT AVG(r.avgRopFtHr) FROM DrRun r WHERE r.rigId = :rigId AND r.status = 'COMPLETED' AND r.avgRopFtHr IS NOT NULL")
    java.math.BigDecimal getAverageRopByRig(@Param("rigId") UUID rigId);

    /**
     * Get total NPT hours by rig
     */
    @Query("SELECT COALESCE(SUM(r.totalNptHours), 0) FROM DrRun r WHERE r.rigId = :rigId AND r.status = 'COMPLETED'")
    java.math.BigDecimal getTotalNptByRig(@Param("rigId") UUID rigId);

    /**
     * Find latest completed runs
     */
    @Query("SELECT r FROM DrRun r WHERE r.tenantId = :tenantId AND r.status = 'COMPLETED' ORDER BY r.endDate DESC")
    List<DrRun> findLatestCompletedRuns(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Find runs with high NPT
     */
    @Query("SELECT r FROM DrRun r WHERE r.tenantId = :tenantId AND r.totalNptHours > :nptThreshold")
    List<DrRun> findRunsWithHighNpt(
        @Param("tenantId") UUID tenantId,
        @Param("nptThreshold") java.math.BigDecimal nptThreshold
    );
}
