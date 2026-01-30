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
package org.thingsboard.server.dao.nexus;

import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantModuleId;
import org.thingsboard.server.common.data.nexus.TenantModule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO interface for TenantModule operations.
 */
public interface TenantModuleDao extends Dao<TenantModule> {

    /**
     * Save or update a tenant module assignment
     */
    TenantModule save(TenantModule tenantModule);

    /**
     * Find tenant module by ID
     */
    TenantModule findById(UUID id);

    /**
     * Find all modules assigned to a tenant
     */
    List<TenantModule> findByTenantId(TenantId tenantId);

    /**
     * Find only active modules for a tenant
     */
    List<TenantModule> findActiveByTenantId(TenantId tenantId);

    /**
     * Find specific tenant-module assignment
     */
    Optional<TenantModule> findByTenantIdAndModuleId(TenantId tenantId, NexusModuleId moduleId);

    /**
     * Check if tenant has specific module active
     */
    boolean isTenantModuleActive(TenantId tenantId, NexusModuleId moduleId);

    /**
     * Find all tenants that have a specific module
     */
    List<TenantModule> findTenantsByModuleId(NexusModuleId moduleId);

    /**
     * Count active modules for a tenant
     */
    long countActiveModulesByTenantId(TenantId tenantId);

    /**
     * Count tenants using a specific module
     */
    long countTenantsByModuleId(NexusModuleId moduleId);

    /**
     * Find tenant modules with pagination
     */
    PageData<TenantModule> findByTenantIdPaged(TenantId tenantId, PageLink pageLink);

    /**
     * Deactivate all modules for a tenant
     */
    int deactivateAllModulesForTenant(TenantId tenantId);

    /**
     * Delete all module assignments for a tenant
     */
    void deleteByTenantId(TenantId tenantId);

    /**
     * Check if assignment exists
     */
    boolean existsByTenantIdAndModuleId(TenantId tenantId, NexusModuleId moduleId);

    /**
     * Find active module keys for a tenant (used for menu filtering)
     */
    List<String> findActiveModuleKeysByTenantId(TenantId tenantId);

    /**
     * Delete tenant module by ID
     */
    boolean removeById(UUID id);
}
