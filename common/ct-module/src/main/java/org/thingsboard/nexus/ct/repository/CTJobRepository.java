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
import org.thingsboard.nexus.ct.model.CTJob;
import org.thingsboard.nexus.ct.model.JobStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CTJobRepository extends JpaRepository<CTJob, UUID> {

    Optional<CTJob> findByJobNumber(String jobNumber);

    Page<CTJob> findByTenantId(UUID tenantId, Pageable pageable);

    List<CTJob> findByTenantIdAndStatus(UUID tenantId, JobStatus status);

    @Query("SELECT j FROM CTJob j WHERE j.tenantId = :tenantId " +
           "AND (:status IS NULL OR j.status = :status) " +
           "AND (:jobType IS NULL OR j.jobType = :jobType) " +
           "AND (:unitId IS NULL OR j.unitId = :unitId) " +
           "AND (:wellName IS NULL OR LOWER(j.wellName) LIKE LOWER(CONCAT('%', :wellName, '%')))")
    Page<CTJob> findByFilters(
        @Param("tenantId") UUID tenantId,
        @Param("status") JobStatus status,
        @Param("jobType") String jobType,
        @Param("unitId") UUID unitId,
        @Param("wellName") String wellName,
        Pageable pageable
    );

    @Query("SELECT j FROM CTJob j WHERE j.tenantId = :tenantId " +
           "AND j.unitId = :unitId " +
           "ORDER BY j.actualStartDate DESC")
    Page<CTJob> findJobsByUnit(
        @Param("tenantId") UUID tenantId,
        @Param("unitId") UUID unitId,
        Pageable pageable
    );

    @Query("SELECT j FROM CTJob j WHERE j.tenantId = :tenantId " +
           "AND j.reelId = :reelId " +
           "ORDER BY j.actualStartDate DESC")
    Page<CTJob> findJobsByReel(
        @Param("tenantId") UUID tenantId,
        @Param("reelId") UUID reelId,
        Pageable pageable
    );

    @Query("SELECT j FROM CTJob j WHERE j.tenantId = :tenantId " +
           "AND j.status IN ('IN_PROGRESS', 'PAUSED')")
    List<CTJob> findActiveJobs(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(j) FROM CTJob j WHERE j.tenantId = :tenantId " +
           "AND j.status = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId,
        @Param("status") JobStatus status
    );

    @Query("SELECT j FROM CTJob j WHERE j.tenantId = :tenantId " +
           "AND j.actualStartDate >= :fromDate " +
           "AND j.actualStartDate <= :toDate " +
           "ORDER BY j.actualStartDate DESC")
    List<CTJob> findJobsInDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("fromDate") Long fromDate,
        @Param("toDate") Long toDate
    );

    boolean existsByJobNumber(String jobNumber);
}
