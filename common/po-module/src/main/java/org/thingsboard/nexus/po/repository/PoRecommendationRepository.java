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
package org.thingsboard.nexus.po.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationStatus;
import org.thingsboard.nexus.po.model.PoRecommendation;

import java.util.List;
import java.util.UUID;

/**
 * Repository for PoRecommendation entities.
 */
@Repository
public interface PoRecommendationRepository extends JpaRepository<PoRecommendation, UUID> {

    /**
     * Find by tenant ID with pagination.
     */
    Page<PoRecommendation> findByTenantIdOrderByCreatedTimeDesc(UUID tenantId, Pageable pageable);

    /**
     * Find by asset ID with pagination.
     */
    Page<PoRecommendation> findByAssetIdOrderByCreatedTimeDesc(UUID assetId, Pageable pageable);

    /**
     * Find by status.
     */
    Page<PoRecommendation> findByTenantIdAndStatusOrderByCreatedTimeDesc(
            UUID tenantId, RecommendationStatus status, Pageable pageable);

    /**
     * Find by status (list).
     */
    List<PoRecommendation> findByTenantIdAndStatusOrderByPriorityAscCreatedTimeDesc(
            UUID tenantId, RecommendationStatus status);

    /**
     * Find pending recommendations for an asset.
     */
    List<PoRecommendation> findByAssetIdAndStatusOrderByPriorityAscCreatedTimeDesc(
            UUID assetId, RecommendationStatus status);

    /**
     * Find by optimization type.
     */
    Page<PoRecommendation> findByTenantIdAndOptimizationTypeOrderByCreatedTimeDesc(
            UUID tenantId, OptimizationType type, Pageable pageable);

    /**
     * Find by multiple statuses.
     */
    Page<PoRecommendation> findByTenantIdAndStatusInOrderByCreatedTimeDesc(
            UUID tenantId, List<RecommendationStatus> statuses, Pageable pageable);

    /**
     * Find by optimization result ID.
     */
    List<PoRecommendation> findByOptimizationResultId(UUID optimizationResultId);

    /**
     * Find expired recommendations.
     */
    @Query("SELECT r FROM PoRecommendation r WHERE r.status = 'PENDING' " +
            "AND r.expiryTime IS NOT NULL AND r.expiryTime < :now")
    List<PoRecommendation> findExpiredRecommendations(@Param("now") Long now);

    /**
     * Count by tenant and status.
     */
    long countByTenantIdAndStatus(UUID tenantId, RecommendationStatus status);

    /**
     * Count by asset and status.
     */
    long countByAssetIdAndStatus(UUID assetId, RecommendationStatus status);

    /**
     * Count pending by tenant.
     */
    @Query("SELECT COUNT(r) FROM PoRecommendation r WHERE r.tenantId = :tenantId AND r.status = 'PENDING'")
    long countPendingByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Expire old pending recommendations.
     */
    @Modifying
    @Query("UPDATE PoRecommendation r SET r.status = 'EXPIRED' " +
            "WHERE r.status = 'PENDING' AND r.expiryTime IS NOT NULL AND r.expiryTime < :now")
    int expireOldRecommendations(@Param("now") Long now);

    /**
     * Find recommendations created between timestamps.
     */
    @Query("SELECT r FROM PoRecommendation r WHERE r.tenantId = :tenantId " +
            "AND r.createdTime >= :fromTs AND r.createdTime <= :toTs ORDER BY r.createdTime DESC")
    List<PoRecommendation> findByTenantIdAndCreatedTimeBetween(
            @Param("tenantId") UUID tenantId,
            @Param("fromTs") Long fromTs,
            @Param("toTs") Long toTs);

    /**
     * Count recommendations by status for analytics.
     */
    @Query("SELECT r.status, COUNT(r) FROM PoRecommendation r " +
            "WHERE r.tenantId = :tenantId GROUP BY r.status")
    List<Object[]> countByTenantIdGroupByStatus(@Param("tenantId") UUID tenantId);

    /**
     * Calculate acceptance rate.
     */
    @Query("SELECT " +
            "CAST(SUM(CASE WHEN r.status IN ('APPROVED', 'EXECUTED') THEN 1 ELSE 0 END) AS double) / " +
            "CAST(COUNT(r) AS double) * 100 " +
            "FROM PoRecommendation r WHERE r.tenantId = :tenantId " +
            "AND r.status NOT IN ('PENDING', 'EXPIRED')")
    Double calculateAcceptanceRate(@Param("tenantId") UUID tenantId);
}
