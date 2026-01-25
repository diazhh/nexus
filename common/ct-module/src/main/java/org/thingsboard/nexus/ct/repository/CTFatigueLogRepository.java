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
import org.thingsboard.nexus.ct.model.CTFatigueLog;

import java.util.List;
import java.util.UUID;

@Repository
public interface CTFatigueLogRepository extends JpaRepository<CTFatigueLog, UUID> {

    List<CTFatigueLog> findByReelIdOrderByTimestampDesc(UUID reelId);

    Page<CTFatigueLog> findByReelIdOrderByTimestampDesc(UUID reelId, Pageable pageable);

    List<CTFatigueLog> findByJobIdOrderByTimestampAsc(UUID jobId);

    @Query("SELECT f FROM CTFatigueLog f WHERE f.reelId = :reelId " +
           "AND f.timestamp >= :fromTime AND f.timestamp <= :toTime " +
           "ORDER BY f.timestamp ASC")
    List<CTFatigueLog> findByReelIdAndTimeRange(
        @Param("reelId") UUID reelId,
        @Param("fromTime") Long fromTime,
        @Param("toTime") Long toTime
    );

    @Query("SELECT f FROM CTFatigueLog f WHERE f.reelId = :reelId " +
           "ORDER BY f.timestamp DESC LIMIT 1")
    CTFatigueLog findLatestByReelId(@Param("reelId") UUID reelId);

    @Query("SELECT COUNT(f) FROM CTFatigueLog f WHERE f.reelId = :reelId")
    Long countByReelId(@Param("reelId") UUID reelId);

    @Query("SELECT f FROM CTFatigueLog f WHERE f.tenantId = :tenantId " +
           "AND f.accumulatedFatiguePercent >= :threshold " +
           "ORDER BY f.accumulatedFatiguePercent DESC")
    List<CTFatigueLog> findHighFatigueReels(
        @Param("tenantId") UUID tenantId,
        @Param("threshold") java.math.BigDecimal threshold
    );
}
