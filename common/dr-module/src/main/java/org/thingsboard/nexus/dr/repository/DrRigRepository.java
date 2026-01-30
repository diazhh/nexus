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
import org.thingsboard.nexus.dr.model.DrRig;
import org.thingsboard.nexus.dr.model.enums.RigStatus;
import org.thingsboard.nexus.dr.model.enums.RigType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Drilling Rig entities
 */
@Repository
public interface DrRigRepository extends JpaRepository<DrRig, UUID> {

    /**
     * Find rig by code
     */
    Optional<DrRig> findByRigCode(String rigCode);

    /**
     * Find rig by asset ID
     */
    Optional<DrRig> findByAssetId(UUID assetId);

    /**
     * Find all rigs for a tenant
     */
    Page<DrRig> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find all rigs for a tenant (list)
     */
    List<DrRig> findByTenantId(UUID tenantId);

    /**
     * Find rigs by tenant and status
     */
    List<DrRig> findByTenantIdAndOperationalStatus(UUID tenantId, RigStatus status);

    /**
     * Find rigs by tenant and type
     */
    List<DrRig> findByTenantIdAndRigType(UUID tenantId, RigType rigType);

    /**
     * Find rig currently assigned to a well
     */
    Optional<DrRig> findByCurrentWellId(UUID wellId);

    /**
     * Find available rigs (standby status)
     */
    @Query("SELECT r FROM DrRig r WHERE r.tenantId = :tenantId " +
           "AND r.operationalStatus = 'STANDBY' " +
           "AND r.currentWellId IS NULL")
    List<DrRig> findAvailableRigs(@Param("tenantId") UUID tenantId);

    /**
     * Find rigs with filters
     */
    @Query("SELECT r FROM DrRig r WHERE r.tenantId = :tenantId " +
           "AND (:status IS NULL OR r.operationalStatus = :status) " +
           "AND (:rigType IS NULL OR r.rigType = :rigType) " +
           "AND (:contractor IS NULL OR LOWER(r.contractor) LIKE LOWER(CONCAT('%', :contractor, '%'))) " +
           "AND (:location IS NULL OR LOWER(r.currentLocation) LIKE LOWER(CONCAT('%', :location, '%')))")
    Page<DrRig> findByFilters(
        @Param("tenantId") UUID tenantId,
        @Param("status") RigStatus status,
        @Param("rigType") RigType rigType,
        @Param("contractor") String contractor,
        @Param("location") String location,
        Pageable pageable
    );

    /**
     * Find rigs with multiple statuses
     */
    @Query("SELECT r FROM DrRig r WHERE r.tenantId = :tenantId " +
           "AND r.operationalStatus IN :statuses")
    List<DrRig> findByTenantIdAndStatuses(
        @Param("tenantId") UUID tenantId,
        @Param("statuses") List<RigStatus> statuses
    );

    /**
     * Count rigs by tenant and status
     */
    @Query("SELECT COUNT(r) FROM DrRig r WHERE r.tenantId = :tenantId " +
           "AND r.operationalStatus = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId,
        @Param("status") RigStatus status
    );

    /**
     * Count rigs by tenant and type
     */
    @Query("SELECT COUNT(r) FROM DrRig r WHERE r.tenantId = :tenantId " +
           "AND r.rigType = :rigType")
    long countByTenantIdAndRigType(
        @Param("tenantId") UUID tenantId,
        @Param("rigType") RigType rigType
    );

    /**
     * Find rigs with overdue BOP test (older than 14 days)
     */
    @Query("SELECT r FROM DrRig r WHERE r.tenantId = :tenantId " +
           "AND r.bopTestDate < :cutoffTime")
    List<DrRig> findRigsWithOverdueBopTest(
        @Param("tenantId") UUID tenantId,
        @Param("cutoffTime") Long cutoffTime
    );

    /**
     * Check if rig code exists
     */
    boolean existsByRigCode(String rigCode);

    /**
     * Check if rig code exists for another rig
     */
    @Query("SELECT COUNT(r) > 0 FROM DrRig r WHERE r.rigCode = :rigCode AND r.id != :excludeId")
    boolean existsByRigCodeAndIdNot(@Param("rigCode") String rigCode, @Param("excludeId") UUID excludeId);
}
