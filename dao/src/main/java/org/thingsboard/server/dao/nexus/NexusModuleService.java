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
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.nexus.NexusModule;
import org.thingsboard.server.common.data.nexus.TenantModule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service interface for NEXUS Module Management.
 * Handles both module definitions and tenant-module assignments.
 */
public interface NexusModuleService extends EntityDaoService {

    // ========================
    // Module Management (SysAdmin)
    // ========================

    /**
     * Find module by ID
     */
    NexusModule findModuleById(NexusModuleId moduleId);

    /**
     * Find module by unique key (CT, RV, DR, etc.)
     */
    Optional<NexusModule> findModuleByKey(String moduleKey);

    /**
     * Save or update a module (SysAdmin only)
     */
    NexusModule saveModule(NexusModule module);

    /**
     * Delete a module (SysAdmin only)
     */
    void deleteModule(NexusModuleId moduleId);

    /**
     * Find all modules with pagination
     */
    PageData<NexusModule> findAllModules(PageLink pageLink);

    /**
     * Find all available modules (can be assigned to tenants)
     */
    List<NexusModule> findAvailableModules();

    /**
     * Find system modules (core, cannot be disabled)
     */
    List<NexusModule> findSystemModules();

    /**
     * Find modules by category
     */
    List<NexusModule> findModulesByCategory(String category);

    /**
     * Count tenants using a specific module
     */
    long countTenantsUsingModule(NexusModuleId moduleId);

    // ========================
    // Tenant Module Assignment (SysAdmin)
    // ========================

    /**
     * Assign a module to a tenant
     */
    TenantModule assignModuleToTenant(TenantId tenantId, NexusModuleId moduleId, UserId activatedBy);

    /**
     * Remove module assignment from tenant
     */
    void unassignModuleFromTenant(TenantId tenantId, NexusModuleId moduleId);

    /**
     * Activate a previously deactivated module for tenant
     */
    TenantModule activateModuleForTenant(TenantId tenantId, NexusModuleId moduleId);

    /**
     * Deactivate a module for tenant (doesn't remove assignment)
     */
    TenantModule deactivateModuleForTenant(TenantId tenantId, NexusModuleId moduleId);

    /**
     * Find all modules assigned to a tenant (active and inactive)
     */
    List<TenantModule> findModulesByTenantId(TenantId tenantId);

    /**
     * Find only active modules for a tenant
     */
    List<TenantModule> findActiveModulesByTenantId(TenantId tenantId);

    /**
     * Find tenant module assignment with pagination
     */
    PageData<TenantModule> findModulesByTenantIdPaged(TenantId tenantId, PageLink pageLink);

    /**
     * Check if tenant has access to a specific module
     */
    boolean hasTenantModuleAccess(TenantId tenantId, NexusModuleId moduleId);

    /**
     * Check if tenant has access to a specific module by key
     */
    boolean hasTenantModuleAccess(TenantId tenantId, String moduleKey);

    /**
     * Get active module keys for tenant (used for menu filtering)
     */
    Set<String> getActiveModuleKeysForTenant(TenantId tenantId);

    /**
     * Count active modules for a tenant
     */
    long countActiveModulesForTenant(TenantId tenantId);

    // ========================
    // Bulk Operations
    // ========================

    /**
     * Assign multiple modules to a tenant
     */
    List<TenantModule> assignModulesToTenant(TenantId tenantId, List<NexusModuleId> moduleIds, UserId activatedBy);

    /**
     * Copy module assignments from one tenant to another
     */
    List<TenantModule> copyModuleAssignments(TenantId sourceTenantId, TenantId targetTenantId, UserId activatedBy);

    /**
     * Deactivate all modules for a tenant
     */
    void deactivateAllModulesForTenant(TenantId tenantId);

    /**
     * Delete all module assignments when tenant is deleted
     */
    void deleteModuleAssignmentsByTenantId(TenantId tenantId);

    // ========================
    // Module Initialization
    // ========================

    /**
     * Register or update a module definition (called at startup)
     */
    NexusModule registerModule(String moduleKey, String name, String description,
                               String category, String icon, String routePath,
                               boolean isSystem, int displayOrder);

    /**
     * Initialize default modules for a new tenant
     */
    void initializeDefaultModulesForTenant(TenantId tenantId, UserId activatedBy);
}
