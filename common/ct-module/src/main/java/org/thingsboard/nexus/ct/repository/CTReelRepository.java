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
package org.thingsboard.nexus.ct.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.ct.model.CTReel;
import org.thingsboard.nexus.ct.model.ReelStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CTReelRepository extends JpaRepository<CTReel, UUID> {

    Optional<CTReel> findByReelCode(String reelCode);

    Page<CTReel> findByTenantId(UUID tenantId, Pageable pageable);

    List<CTReel> findByTenantIdAndStatus(UUID tenantId, ReelStatus status);

    @Query("SELECT r FROM CTReel r WHERE r.tenantId = :tenantId " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:odInch IS NULL OR r.tubingOdInch = :odInch) " +
           "AND (:fatigueMin IS NULL OR r.accumulatedFatiguePercent >= :fatigueMin) " +
           "AND (:fatigueMax IS NULL OR r.accumulatedFatiguePercent <= :fatigueMax)")
    Page<CTReel> findByFilters(
        @Param("tenantId") UUID tenantId,
        @Param("status") ReelStatus status,
        @Param("odInch") BigDecimal odInch,
        @Param("fatigueMin") BigDecimal fatigueMin,
        @Param("fatigueMax") BigDecimal fatigueMax,
        Pageable pageable
    );

    @Query("SELECT r FROM CTReel r WHERE r.tenantId = :tenantId " +
           "AND r.status = 'AVAILABLE' " +
           "AND r.tubingOdInch = :odInch " +
           "AND r.accumulatedFatiguePercent < :maxFatigue " +
           "ORDER BY r.accumulatedFatiguePercent ASC")
    List<CTReel> findAvailableReelsBySize(
        @Param("tenantId") UUID tenantId,
        @Param("odInch") BigDecimal odInch,
        @Param("maxFatigue") BigDecimal maxFatigue
    );

    @Query("SELECT COUNT(r) FROM CTReel r WHERE r.tenantId = :tenantId " +
           "AND r.status = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId,
        @Param("status") ReelStatus status
    );

    @Query("SELECT r FROM CTReel r WHERE r.tenantId = :tenantId " +
           "AND r.accumulatedFatiguePercent >= :threshold " +
           "ORDER BY r.accumulatedFatiguePercent DESC")
    List<CTReel> findReelsAboveFatigueThreshold(
        @Param("tenantId") UUID tenantId,
        @Param("threshold") BigDecimal threshold
    );

    boolean existsByReelCode(String reelCode);
}
