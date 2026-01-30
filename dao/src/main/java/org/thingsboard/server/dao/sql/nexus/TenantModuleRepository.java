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
import org.thingsboard.server.dao.model.sql.TenantModuleEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for TenantModule entities.
 */
public interface TenantModuleRepository extends JpaRepository<TenantModuleEntity, UUID> {

    /**
     * Find all modules assigned to a tenant
     */
    @Query("SELECT tm FROM TenantModuleEntity tm WHERE tm.tenantId = :tenantId")
    List<TenantModuleEntity> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find only active modules for a tenant
     */
    @Query("SELECT tm FROM TenantModuleEntity tm WHERE tm.tenantId = :tenantId AND tm.isActive = true")
    List<TenantModuleEntity> findActiveByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find specific tenant-module assignment
     */
    @Query("SELECT tm FROM TenantModuleEntity tm WHERE tm.tenantId = :tenantId AND tm.moduleId = :moduleId")
    Optional<TenantModuleEntity> findByTenantIdAndModuleId(@Param("tenantId") UUID tenantId,
                                                           @Param("moduleId") UUID moduleId);

    /**
     * Check if tenant has specific module active
     */
    @Query("SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TenantModuleEntity tm " +
            "WHERE tm.tenantId = :tenantId AND tm.moduleId = :moduleId AND tm.isActive = true")
    boolean isTenantModuleActive(@Param("tenantId") UUID tenantId, @Param("moduleId") UUID moduleId);

    /**
     * Find tenants that have a specific module
     */
    @Query("SELECT tm FROM TenantModuleEntity tm WHERE tm.moduleId = :moduleId AND tm.isActive = true")
    List<TenantModuleEntity> findTenantsByModuleId(@Param("moduleId") UUID moduleId);

    /**
     * Count active modules for a tenant
     */
    @Query("SELECT COUNT(tm) FROM TenantModuleEntity tm WHERE tm.tenantId = :tenantId AND tm.isActive = true")
    long countActiveModulesByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Count tenants using a specific module
     */
    @Query("SELECT COUNT(tm) FROM TenantModuleEntity tm WHERE tm.moduleId = :moduleId AND tm.isActive = true")
    long countTenantsByModuleId(@Param("moduleId") UUID moduleId);

    /**
     * Find tenant modules with pagination
     */
    @Query("SELECT tm FROM TenantModuleEntity tm WHERE tm.tenantId = :tenantId")
    Page<TenantModuleEntity> findByTenantIdPaged(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Deactivate all modules for a tenant
     */
    @Modifying
    @Query("UPDATE TenantModuleEntity tm SET tm.isActive = false, tm.deactivationDate = :deactivationDate " +
            "WHERE tm.tenantId = :tenantId")
    int deactivateAllModulesForTenant(@Param("tenantId") UUID tenantId,
                                       @Param("deactivationDate") long deactivationDate);

    /**
     * Delete all module assignments for a tenant
     */
    @Modifying
    @Query("DELETE FROM TenantModuleEntity tm WHERE tm.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Check if assignment exists
     */
    boolean existsByTenantIdAndModuleId(UUID tenantId, UUID moduleId);

    /**
     * Find module keys for active tenant modules (used for menu filtering)
     */
    @Query("SELECT m.moduleKey FROM TenantModuleEntity tm " +
            "JOIN NexusModuleEntity m ON tm.moduleId = m.id " +
            "WHERE tm.tenantId = :tenantId AND tm.isActive = true")
    List<String> findActiveModuleKeysByTenantId(@Param("tenantId") UUID tenantId);
}
