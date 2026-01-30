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

import com.google.common.util.concurrent.FluentFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantModuleId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.nexus.NexusModule;
import org.thingsboard.server.common.data.nexus.TenantModule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * Implementation of NexusModuleService.
 */
@Service("NexusModuleService")
@Slf4j
public class BaseNexusModuleService implements NexusModuleService {

    public static final String INCORRECT_MODULE_ID = "Incorrect moduleId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String MODULE_KEY_REQUIRED = "Module key is required";
    public static final String MODULE_NAME_REQUIRED = "Module name is required";
    public static final String MODULE_KEY_ALREADY_EXISTS = "Module with key '%s' already exists";
    public static final String MODULE_IN_USE_ERROR = "Module is assigned to %d tenants and cannot be deleted";
    public static final String SYSTEM_MODULE_DELETE_ERROR = "System modules cannot be deleted";
    public static final String MODULE_NOT_FOUND = "Module not found: ";
    public static final String MODULE_ALREADY_ASSIGNED = "Module '%s' is already assigned to this tenant";
    public static final String MODULE_NOT_ASSIGNED = "Module '%s' is not assigned to this tenant";

    @Autowired
    private NexusModuleDao moduleDao;

    @Autowired
    private TenantModuleDao tenantModuleDao;

    // ========================
    // Module Management
    // ========================

    @Override
    public NexusModule findModuleById(NexusModuleId moduleId) {
        log.trace("Executing findModuleById [{}]", moduleId);
        validateId(moduleId, id -> INCORRECT_MODULE_ID + id);
        return moduleDao.findById(moduleId.getId());
    }

    @Override
    public Optional<NexusModule> findModuleByKey(String moduleKey) {
        log.trace("Executing findModuleByKey [{}]", moduleKey);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        return moduleDao.findByModuleKey(moduleKey);
    }

    @Override
    @Transactional
    public NexusModule saveModule(NexusModule module) {
        log.trace("Executing saveModule [{}]", module);
        validateModule(module);

        if (module.getId() == null) {
            // Creating new module
            if (moduleDao.existsByModuleKey(module.getModuleKey())) {
                throw new IncorrectParameterException(String.format(MODULE_KEY_ALREADY_EXISTS, module.getModuleKey()));
            }
        } else {
            // Updating existing module
            Optional<NexusModule> existingByKey = moduleDao.findByModuleKey(module.getModuleKey());
            if (existingByKey.isPresent() && !existingByKey.get().getId().equals(module.getId())) {
                throw new IncorrectParameterException(String.format(MODULE_KEY_ALREADY_EXISTS, module.getModuleKey()));
            }
        }

        return moduleDao.save(module);
    }

    @Override
    @Transactional
    public void deleteModule(NexusModuleId moduleId) {
        log.trace("Executing deleteModule [{}]", moduleId);
        validateId(moduleId, id -> INCORRECT_MODULE_ID + id);

        NexusModule module = findModuleById(moduleId);
        if (module == null) {
            throw new IncorrectParameterException(MODULE_NOT_FOUND + moduleId);
        }

        if (module.isSystemModule()) {
            throw new IncorrectParameterException(SYSTEM_MODULE_DELETE_ERROR);
        }

        long tenantsCount = tenantModuleDao.countTenantsByModuleId(moduleId);
        if (tenantsCount > 0) {
            throw new IncorrectParameterException(String.format(MODULE_IN_USE_ERROR, tenantsCount));
        }

        moduleDao.removeById(moduleId.getId());
    }

    @Override
    public PageData<NexusModule> findAllModules(PageLink pageLink) {
        log.trace("Executing findAllModules [{}]", pageLink);
        validatePageLink(pageLink);
        return moduleDao.findAllModules(pageLink);
    }

    @Override
    public List<NexusModule> findAvailableModules() {
        log.trace("Executing findAvailableModules");
        return moduleDao.findAvailableModules();
    }

    @Override
    public List<NexusModule> findSystemModules() {
        log.trace("Executing findSystemModules");
        return moduleDao.findSystemModules();
    }

    @Override
    public List<NexusModule> findModulesByCategory(String category) {
        log.trace("Executing findModulesByCategory [{}]", category);
        return moduleDao.findByCategory(category);
    }

    @Override
    public long countTenantsUsingModule(NexusModuleId moduleId) {
        log.trace("Executing countTenantsUsingModule [{}]", moduleId);
        validateId(moduleId, id -> INCORRECT_MODULE_ID + id);
        return tenantModuleDao.countTenantsByModuleId(moduleId);
    }

    // ========================
    // Tenant Module Assignment
    // ========================

    @Override
    @Transactional
    public TenantModule assignModuleToTenant(TenantId tenantId, NexusModuleId moduleId, UserId activatedBy) {
        log.trace("Executing assignModuleToTenant [{}] [{}]", tenantId, moduleId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(moduleId, id -> INCORRECT_MODULE_ID + id);

        NexusModule module = findModuleById(moduleId);
        if (module == null) {
            throw new IncorrectParameterException(MODULE_NOT_FOUND + moduleId);
        }

        // Check if already assigned
        Optional<TenantModule> existing = tenantModuleDao.findByTenantIdAndModuleId(tenantId, moduleId);
        if (existing.isPresent()) {
            // If deactivated, reactivate it
            TenantModule tenantModule = existing.get();
            if (!tenantModule.isActive()) {
                tenantModule.reactivate();
                tenantModule.setActivatedBy(activatedBy);
                return tenantModuleDao.save(tenantModule);
            }
            throw new IncorrectParameterException(String.format(MODULE_ALREADY_ASSIGNED, module.getModuleKey()));
        }

        // Create new assignment
        TenantModule tenantModule = new TenantModule();
        tenantModule.setTenantId(tenantId);
        tenantModule.setModuleId(moduleId);
        tenantModule.setActive(true);
        tenantModule.setActivationDate(System.currentTimeMillis());
        tenantModule.setActivatedBy(activatedBy);

        return tenantModuleDao.save(tenantModule);
    }

    @Override
    @Transactional
    public void unassignModuleFromTenant(TenantId tenantId, NexusModuleId moduleId) {
        log.trace("Executing unassignModuleFromTenant [{}] [{}]", tenantId, moduleId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(moduleId, id -> INCORRECT_MODULE_ID + id);

        Optional<TenantModule> existing = tenantModuleDao.findByTenantIdAndModuleId(tenantId, moduleId);
        if (existing.isEmpty()) {
            NexusModule module = findModuleById(moduleId);
            String moduleKey = module != null ? module.getModuleKey() : moduleId.toString();
            throw new IncorrectParameterException(String.format(MODULE_NOT_ASSIGNED, moduleKey));
        }

        tenantModuleDao.removeById(existing.get().getId().getId());
    }

    @Override
    @Transactional
    public TenantModule activateModuleForTenant(TenantId tenantId, NexusModuleId moduleId) {
        log.trace("Executing activateModuleForTenant [{}] [{}]", tenantId, moduleId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(moduleId, id -> INCORRECT_MODULE_ID + id);

        Optional<TenantModule> existing = tenantModuleDao.findByTenantIdAndModuleId(tenantId, moduleId);
        if (existing.isEmpty()) {
            NexusModule module = findModuleById(moduleId);
            String moduleKey = module != null ? module.getModuleKey() : moduleId.toString();
            throw new IncorrectParameterException(String.format(MODULE_NOT_ASSIGNED, moduleKey));
        }

        TenantModule tenantModule = existing.get();
        if (!tenantModule.isActive()) {
            tenantModule.reactivate();
            return tenantModuleDao.save(tenantModule);
        }

        return tenantModule;
    }

    @Override
    @Transactional
    public TenantModule deactivateModuleForTenant(TenantId tenantId, NexusModuleId moduleId) {
        log.trace("Executing deactivateModuleForTenant [{}] [{}]", tenantId, moduleId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(moduleId, id -> INCORRECT_MODULE_ID + id);

        Optional<TenantModule> existing = tenantModuleDao.findByTenantIdAndModuleId(tenantId, moduleId);
        if (existing.isEmpty()) {
            NexusModule module = findModuleById(moduleId);
            String moduleKey = module != null ? module.getModuleKey() : moduleId.toString();
            throw new IncorrectParameterException(String.format(MODULE_NOT_ASSIGNED, moduleKey));
        }

        TenantModule tenantModule = existing.get();
        if (tenantModule.isActive()) {
            tenantModule.deactivate();
            return tenantModuleDao.save(tenantModule);
        }

        return tenantModule;
    }

    @Override
    public List<TenantModule> findModulesByTenantId(TenantId tenantId) {
        log.trace("Executing findModulesByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return tenantModuleDao.findByTenantId(tenantId);
    }

    @Override
    public List<TenantModule> findActiveModulesByTenantId(TenantId tenantId) {
        log.trace("Executing findActiveModulesByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return tenantModuleDao.findActiveByTenantId(tenantId);
    }

    @Override
    public PageData<TenantModule> findModulesByTenantIdPaged(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findModulesByTenantIdPaged [{}] [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return tenantModuleDao.findByTenantIdPaged(tenantId, pageLink);
    }

    @Override
    public boolean hasTenantModuleAccess(TenantId tenantId, NexusModuleId moduleId) {
        log.trace("Executing hasTenantModuleAccess [{}] [{}]", tenantId, moduleId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(moduleId, id -> INCORRECT_MODULE_ID + id);
        return tenantModuleDao.isTenantModuleActive(tenantId, moduleId);
    }

    @Override
    public boolean hasTenantModuleAccess(TenantId tenantId, String moduleKey) {
        log.trace("Executing hasTenantModuleAccess [{}] [{}]", tenantId, moduleKey);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);

        Optional<NexusModule> module = findModuleByKey(moduleKey);
        if (module.isEmpty()) {
            return false;
        }

        return tenantModuleDao.isTenantModuleActive(tenantId, module.get().getId());
    }

    @Override
    public Set<String> getActiveModuleKeysForTenant(TenantId tenantId) {
        log.trace("Executing getActiveModuleKeysForTenant [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        List<String> keys = tenantModuleDao.findActiveModuleKeysByTenantId(tenantId);
        return new HashSet<>(keys);
    }

    @Override
    public long countActiveModulesForTenant(TenantId tenantId) {
        log.trace("Executing countActiveModulesForTenant [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return tenantModuleDao.countActiveModulesByTenantId(tenantId);
    }

    // ========================
    // Bulk Operations
    // ========================

    @Override
    @Transactional
    public List<TenantModule> assignModulesToTenant(TenantId tenantId, List<NexusModuleId> moduleIds, UserId activatedBy) {
        log.trace("Executing assignModulesToTenant [{}] modules: {}", tenantId, moduleIds.size());
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);

        List<TenantModule> result = new ArrayList<>();
        for (NexusModuleId moduleId : moduleIds) {
            try {
                TenantModule assigned = assignModuleToTenant(tenantId, moduleId, activatedBy);
                result.add(assigned);
            } catch (IncorrectParameterException e) {
                log.warn("Could not assign module {} to tenant {}: {}", moduleId, tenantId, e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional
    public List<TenantModule> copyModuleAssignments(TenantId sourceTenantId, TenantId targetTenantId, UserId activatedBy) {
        log.trace("Executing copyModuleAssignments from [{}] to [{}]", sourceTenantId, targetTenantId);
        validateId(sourceTenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(targetTenantId, id -> INCORRECT_TENANT_ID + id);

        List<TenantModule> sourceModules = findActiveModulesByTenantId(sourceTenantId);
        List<NexusModuleId> moduleIds = sourceModules.stream()
                .map(TenantModule::getModuleId)
                .collect(Collectors.toList());

        return assignModulesToTenant(targetTenantId, moduleIds, activatedBy);
    }

    @Override
    @Transactional
    public void deactivateAllModulesForTenant(TenantId tenantId) {
        log.trace("Executing deactivateAllModulesForTenant [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantModuleDao.deactivateAllModulesForTenant(tenantId);
    }

    @Override
    @Transactional
    public void deleteModuleAssignmentsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteModuleAssignmentsByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantModuleDao.deleteByTenantId(tenantId);
    }

    // ========================
    // Module Initialization
    // ========================

    @Override
    @Transactional
    public NexusModule registerModule(String moduleKey, String name, String description,
                                       String category, String icon, String routePath,
                                       boolean isSystem, int displayOrder) {
        log.trace("Executing registerModule [{}]", moduleKey);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        validateString(name, n -> MODULE_NAME_REQUIRED);

        Optional<NexusModule> existing = findModuleByKey(moduleKey);

        NexusModule module;
        if (existing.isPresent()) {
            // Update existing module
            module = existing.get();
            module.setName(name);
            module.setDescription(description);
            module.setCategory(category);
            module.setIcon(icon);
            module.setRoutePath(routePath);
            module.setSystemModule(isSystem);
            module.setDisplayOrder(displayOrder);
        } else {
            // Create new module
            module = new NexusModule();
            module.setModuleKey(moduleKey);
            module.setName(name);
            module.setDescription(description);
            module.setCategory(category);
            module.setIcon(icon);
            module.setRoutePath(routePath);
            module.setSystemModule(isSystem);
            module.setAvailable(true);
            module.setDisplayOrder(displayOrder);
        }

        return moduleDao.save(module);
    }

    @Override
    @Transactional
    public void initializeDefaultModulesForTenant(TenantId tenantId, UserId activatedBy) {
        log.trace("Executing initializeDefaultModulesForTenant [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);

        // Assign all system modules by default
        List<NexusModule> systemModules = findSystemModules();
        for (NexusModule module : systemModules) {
            try {
                assignModuleToTenant(tenantId, module.getId(), activatedBy);
            } catch (IncorrectParameterException e) {
                log.warn("Could not assign system module {} to tenant {}: {}",
                        module.getModuleKey(), tenantId, e.getMessage());
            }
        }
    }

    // ========================
    // EntityDaoService
    // ========================

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findModuleById(new NexusModuleId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(moduleDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NEXUS_MODULE;
    }

    // ========================
    // Private Methods
    // ========================

    private void validateModule(NexusModule module) {
        if (module == null) {
            throw new IncorrectParameterException("Module cannot be null");
        }
        validateString(module.getModuleKey(), k -> MODULE_KEY_REQUIRED);
        validateString(module.getName(), n -> MODULE_NAME_REQUIRED);
    }
}
