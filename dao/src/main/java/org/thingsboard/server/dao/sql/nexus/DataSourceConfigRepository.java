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
import org.thingsboard.server.dao.model.sql.DataSourceConfigEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for DataSourceConfig entities.
 */
public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfigEntity, UUID> {

    /**
     * Find configuration by device ID
     */
    @Query("SELECT c FROM DataSourceConfigEntity c WHERE c.tenantId = :tenantId AND c.deviceId = :deviceId")
    Optional<DataSourceConfigEntity> findByTenantIdAndDeviceId(@Param("tenantId") UUID tenantId, @Param("deviceId") UUID deviceId);

    /**
     * Find configurations for a tenant with pagination and search
     */
    @Query("SELECT c FROM DataSourceConfigEntity c WHERE c.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(c.moduleKey, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(c.targetAssetType, CONCAT('%', :searchText, '%')) = true)")
    Page<DataSourceConfigEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                                 @Param("searchText") String searchText,
                                                 Pageable pageable);

    /**
     * Find configurations by module key
     */
    @Query("SELECT c FROM DataSourceConfigEntity c WHERE c.tenantId = :tenantId AND c.moduleKey = :moduleKey")
    List<DataSourceConfigEntity> findByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey);

    /**
     * Find configurations by module key with pagination and search
     */
    @Query("SELECT c FROM DataSourceConfigEntity c WHERE c.tenantId = :tenantId AND c.moduleKey = :moduleKey " +
            "AND (:searchText IS NULL OR ilike(c.targetAssetType, CONCAT('%', :searchText, '%')) = true)")
    Page<DataSourceConfigEntity> findByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId,
                                                              @Param("moduleKey") String moduleKey,
                                                              @Param("searchText") String searchText,
                                                              Pageable pageable);

    /**
     * Find active configurations for a tenant
     */
    @Query("SELECT c FROM DataSourceConfigEntity c WHERE c.tenantId = :tenantId AND c.isActive = true")
    List<DataSourceConfigEntity> findActiveByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find active configurations by module key
     */
    @Query("SELECT c FROM DataSourceConfigEntity c WHERE c.tenantId = :tenantId AND c.moduleKey = :moduleKey AND c.isActive = true")
    List<DataSourceConfigEntity> findActiveByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey);

    /**
     * Check if a device already has a configuration
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM DataSourceConfigEntity c WHERE c.tenantId = :tenantId AND c.deviceId = :deviceId")
    boolean existsByTenantIdAndDeviceId(@Param("tenantId") UUID tenantId, @Param("deviceId") UUID deviceId);

    /**
     * Delete all configurations for a tenant
     */
    @Modifying
    @Query("DELETE FROM DataSourceConfigEntity c WHERE c.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);
}
