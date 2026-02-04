/*
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
package org.thingsboard.nexus.po.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.po.dto.OptimizationResultDto.OptimizationRunStatus;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.model.PoOptimizationResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PoOptimizationResult entities.
 */
@Repository
public interface PoOptimizationResultRepository extends JpaRepository<PoOptimizationResult, UUID> {

    /**
     * Find by tenant ID with pagination.
     */
    Page<PoOptimizationResult> findByTenantIdOrderByTimestampDesc(UUID tenantId, Pageable pageable);

    /**
     * Find by asset ID with pagination.
     */
    Page<PoOptimizationResult> findByAssetIdOrderByTimestampDesc(UUID assetId, Pageable pageable);

    /**
     * Find by tenant and asset.
     */
    Page<PoOptimizationResult> findByTenantIdAndAssetIdOrderByTimestampDesc(
            UUID tenantId, UUID assetId, Pageable pageable);

    /**
     * Find by optimization type.
     */
    Page<PoOptimizationResult> findByTenantIdAndOptimizationTypeOrderByTimestampDesc(
            UUID tenantId, OptimizationType type, Pageable pageable);

    /**
     * Find by run status.
     */
    Page<PoOptimizationResult> findByTenantIdAndRunStatusOrderByTimestampDesc(
            UUID tenantId, OptimizationRunStatus status, Pageable pageable);

    /**
     * Find latest result for an asset.
     */
    Optional<PoOptimizationResult> findFirstByAssetIdOrderByTimestampDesc(UUID assetId);

    /**
     * Find latest result for an asset and optimization type.
     */
    Optional<PoOptimizationResult> findFirstByAssetIdAndOptimizationTypeOrderByTimestampDesc(
            UUID assetId, OptimizationType type);

    /**
     * Find results within a time range.
     */
    @Query("SELECT r FROM PoOptimizationResult r WHERE r.tenantId = :tenantId " +
            "AND r.timestamp >= :fromTs AND r.timestamp <= :toTs ORDER BY r.timestamp DESC")
    List<PoOptimizationResult> findByTenantIdAndTimestampBetween(
            @Param("tenantId") UUID tenantId,
            @Param("fromTs") Long fromTs,
            @Param("toTs") Long toTs);

    /**
     * Find results for an asset within a time range.
     */
    @Query("SELECT r FROM PoOptimizationResult r WHERE r.assetId = :assetId " +
            "AND r.timestamp >= :fromTs AND r.timestamp <= :toTs ORDER BY r.timestamp DESC")
    List<PoOptimizationResult> findByAssetIdAndTimestampBetween(
            @Param("assetId") UUID assetId,
            @Param("fromTs") Long fromTs,
            @Param("toTs") Long toTs);

    /**
     * Count by tenant and status.
     */
    long countByTenantIdAndRunStatus(UUID tenantId, OptimizationRunStatus status);

    /**
     * Count by tenant and optimization type.
     */
    long countByTenantIdAndOptimizationType(UUID tenantId, OptimizationType type);

    /**
     * Delete old results (for cleanup).
     */
    @Query("DELETE FROM PoOptimizationResult r WHERE r.tenantId = :tenantId AND r.timestamp < :beforeTs")
    void deleteOldResults(@Param("tenantId") UUID tenantId, @Param("beforeTs") Long beforeTs);
}
