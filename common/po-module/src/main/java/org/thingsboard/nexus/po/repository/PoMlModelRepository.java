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
import org.thingsboard.nexus.po.dto.ml.MlModelStatus;
import org.thingsboard.nexus.po.dto.ml.MlModelType;
import org.thingsboard.nexus.po.model.PoMlModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ML model registry.
 */
@Repository
public interface PoMlModelRepository extends JpaRepository<PoMlModel, UUID> {

    /**
     * Find all models by tenant.
     */
    Page<PoMlModel> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find all models by tenant (no pagination).
     */
    List<PoMlModel> findByTenantIdOrderByCreatedTimeDesc(UUID tenantId);

    /**
     * Find models by tenant and status.
     */
    List<PoMlModel> findByTenantIdAndStatus(UUID tenantId, MlModelStatus status);

    /**
     * Find models by tenant and type.
     */
    List<PoMlModel> findByTenantIdAndModelType(UUID tenantId, MlModelType modelType);

    /**
     * Find active models by tenant.
     */
    @Query("SELECT m FROM PoMlModel m WHERE m.tenantId = :tenantId AND m.status = 'ACTIVE' ORDER BY m.createdTime DESC")
    List<PoMlModel> findActiveModels(@Param("tenantId") UUID tenantId);

    /**
     * Find active model by type.
     * Only one model per type should be active.
     */
    @Query("SELECT m FROM PoMlModel m WHERE m.tenantId = :tenantId AND m.modelType = :modelType AND m.status = 'ACTIVE'")
    Optional<PoMlModel> findActiveModelByType(@Param("tenantId") UUID tenantId, @Param("modelType") MlModelType modelType);

    /**
     * Find model by name and version.
     */
    Optional<PoMlModel> findByTenantIdAndNameAndVersion(UUID tenantId, String name, String version);

    /**
     * Find latest model by name.
     */
    @Query("SELECT m FROM PoMlModel m WHERE m.tenantId = :tenantId AND m.name = :name ORDER BY m.createdTime DESC LIMIT 1")
    Optional<PoMlModel> findLatestByName(@Param("tenantId") UUID tenantId, @Param("name") String name);

    /**
     * Count active models by tenant.
     */
    long countByTenantIdAndStatus(UUID tenantId, MlModelStatus status);
}
