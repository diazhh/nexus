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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.po.dto.ml.TrainingJobStatus;
import org.thingsboard.nexus.po.model.PoMlTrainingJob;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ML training job tracking.
 */
@Repository
public interface PoMlTrainingJobRepository extends JpaRepository<PoMlTrainingJob, UUID> {

    /**
     * Find training jobs by tenant with pagination.
     */
    Page<PoMlTrainingJob> findByTenantIdOrderByCreatedTimeDesc(UUID tenantId, Pageable pageable);

    /**
     * Find training jobs by status.
     */
    List<PoMlTrainingJob> findByTenantIdAndStatus(UUID tenantId, TrainingJobStatus status);

    /**
     * Find running jobs for a tenant.
     */
    @Query("SELECT j FROM PoMlTrainingJob j WHERE j.tenantId = :tenantId AND j.status IN ('PENDING', 'RUNNING') ORDER BY j.createdTime DESC")
    List<PoMlTrainingJob> findRunningJobs(@Param("tenantId") UUID tenantId);

    /**
     * Find all running jobs (for scheduler).
     */
    @Query("SELECT j FROM PoMlTrainingJob j WHERE j.status IN ('PENDING', 'RUNNING') ORDER BY j.createdTime ASC")
    List<PoMlTrainingJob> findAllRunningJobs();

    /**
     * Find jobs by model name.
     */
    List<PoMlTrainingJob> findByTenantIdAndModelNameOrderByCreatedTimeDesc(UUID tenantId, String modelName);

    /**
     * Count running jobs for tenant.
     */
    @Query("SELECT COUNT(j) FROM PoMlTrainingJob j WHERE j.tenantId = :tenantId AND j.status IN ('PENDING', 'RUNNING')")
    long countRunningJobs(@Param("tenantId") UUID tenantId);

    /**
     * Find stale jobs (running for too long, possible crash).
     */
    @Query("SELECT j FROM PoMlTrainingJob j WHERE j.status = 'RUNNING' AND j.startedTime < :cutoffTime")
    List<PoMlTrainingJob> findStaleJobs(@Param("cutoffTime") Long cutoffTime);
}
