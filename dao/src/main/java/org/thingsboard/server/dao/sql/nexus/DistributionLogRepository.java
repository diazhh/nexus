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
package org.thingsboard.server.dao.sql.nexus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.nexus.DistributionStatus;
import org.thingsboard.server.dao.model.sql.DistributionLogEntity;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for DistributionLog entities.
 */
public interface DistributionLogRepository extends JpaRepository<DistributionLogEntity, UUID> {

    /**
     * Find log entries for a tenant with pagination
     */
    @Query("SELECT l FROM DistributionLogEntity l WHERE l.tenantId = :tenantId ORDER BY l.createdTime DESC")
    Page<DistributionLogEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Find log entries for a device with pagination
     */
    @Query("SELECT l FROM DistributionLogEntity l WHERE l.tenantId = :tenantId AND l.deviceId = :deviceId ORDER BY l.createdTime DESC")
    Page<DistributionLogEntity> findByTenantIdAndDeviceId(@Param("tenantId") UUID tenantId, @Param("deviceId") UUID deviceId, Pageable pageable);

    /**
     * Find log entries by status
     */
    @Query("SELECT l FROM DistributionLogEntity l WHERE l.tenantId = :tenantId AND l.status = :status ORDER BY l.createdTime DESC")
    List<DistributionLogEntity> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") DistributionStatus status);

    /**
     * Find log entries within a time range
     */
    @Query("SELECT l FROM DistributionLogEntity l WHERE l.tenantId = :tenantId AND l.createdTime >= :startTime AND l.createdTime <= :endTime ORDER BY l.createdTime DESC")
    Page<DistributionLogEntity> findByTenantIdAndTimeRange(@Param("tenantId") UUID tenantId,
                                                           @Param("startTime") long startTime,
                                                           @Param("endTime") long endTime,
                                                           Pageable pageable);

    /**
     * Count logs by status for a tenant
     */
    @Query("SELECT COUNT(l) FROM DistributionLogEntity l WHERE l.tenantId = :tenantId AND l.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") DistributionStatus status);

    /**
     * Delete logs older than a given timestamp
     */
    @Modifying
    @Query("DELETE FROM DistributionLogEntity l WHERE l.tenantId = :tenantId AND l.createdTime < :timestamp")
    void deleteByTenantIdAndCreatedTimeBefore(@Param("tenantId") UUID tenantId, @Param("timestamp") long timestamp);

    /**
     * Delete all logs for a tenant
     */
    @Modifying
    @Query("DELETE FROM DistributionLogEntity l WHERE l.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);
}
